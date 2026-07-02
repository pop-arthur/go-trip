package gotrip.repository.orderfile

import java.util.UUID

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.order.*
import gotrip.domain.user.*
import gotrip.repository.SkunkCodecs
import io.circe.parser.parse
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import java.time.{Instant, OffsetDateTime, ZoneOffset}

final class PostgresOrderFileRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends OrderFileRepository[F]:

  override def listByOrder(userId: UserId, orderId: OrderId): F[List[OrderFile]] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderFileRepository.listByOrderQuery).flatMap { query =>
        query.stream((userId.value, orderId.value), 64).compile.toList
      }
    }

  override def findByOrder(userId: UserId, orderId: OrderId, fileId: OrderFileId): F[Option[OrderFile]] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderFileRepository.findByOrderQuery).flatMap { query =>
        query.option((userId.value, orderId.value, fileId.value))
      }
    }

  override def create(userId: UserId, file: OrderFile): F[Option[OrderFile]] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderFileRepository.createQuery).flatMap { query =>
        query.option(
          (
            file.id.value,
            file.file_url.value,
            SkunkCodecs.encodeFileType(file.file_type),
            file.parsed_data.map(_.noSpaces),
            PostgresOrderFileRepository.toOffset(file.uploaded_at),
            userId.value,
            file.order_id.value
          )
        )
      }
    }

  override def delete(userId: UserId, orderId: OrderId, fileId: OrderFileId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderFileRepository.deleteQuery).flatMap { query =>
        query.option((userId.value, orderId.value, fileId.value)).map(_.isDefined)
      }
    }

  override def orderExistsForUser(userId: UserId, orderId: OrderId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderFileRepository.orderExistsForUserQuery).flatMap { query =>
        query.option((userId.value, orderId.value)).map(_.isDefined)
      }
    }

object PostgresOrderFileRepository:

  def make[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): OrderFileRepository[F] =
    new PostgresOrderFileRepository(sessionPool)

  private val orderFileDecoder: Decoder[OrderFile] =
    (uuid ~ uuid ~ text ~ text ~ text.opt ~ timestamptz).map {
      case id ~ orderId ~ fileUrl ~ fileType ~ parsedData ~ uploadedAt =>
        OrderFile(
          id = OrderFileId(id),
          order_id = OrderId(orderId),
          file_url = OrderFileUrl(fileUrl),
          file_type = SkunkCodecs.decodeFileType(fileType).get,
          parsed_data = parsedData.flatMap(value => parse(value).toOption),
          uploaded_at = uploadedAt.toInstant
        )
    }

  val listByOrderQuery: Query[(UUID, UUID), OrderFile] =
    sql"""
      select f.id, f.order_id, f.file_url, f.file_type, f.parsed_data::text, f.uploaded_at
      from order_files f
      inner join orders o on o.id = f.order_id
      where o.user_id = $uuid
        and f.order_id = $uuid
      order by f.uploaded_at desc, f.id desc
    """.query(orderFileDecoder)

  val findByOrderQuery: Query[(UUID, UUID, UUID), OrderFile] =
    sql"""
      select f.id, f.order_id, f.file_url, f.file_type, f.parsed_data::text, f.uploaded_at
      from order_files f
      inner join orders o on o.id = f.order_id
      where o.user_id = $uuid
        and f.order_id = $uuid
        and f.id = $uuid
    """.query(orderFileDecoder)

  private def toOffset(instant: Instant): OffsetDateTime =
    instant.atOffset(ZoneOffset.UTC)

  val createQuery: Query[(UUID, String, String, Option[String], OffsetDateTime, UUID, UUID), OrderFile] =
    sql"""
      insert into order_files (id, order_id, file_url, file_type, parsed_data, uploaded_at)
      select $uuid, o.id, $text, $text, ${text.opt}::jsonb, $timestamptz
      from orders o
      where o.user_id = $uuid
        and o.id = $uuid
      returning id, order_id, file_url, file_type, parsed_data::text, uploaded_at
    """.query(orderFileDecoder)

  val deleteQuery: Query[(UUID, UUID, UUID), UUID] =
    sql"""
      delete from order_files f
      using orders o
      where o.id = f.order_id
        and o.user_id = $uuid
        and f.order_id = $uuid
        and f.id = $uuid
      returning f.id
    """.query(uuid)

  val orderExistsForUserQuery: Query[(UUID, UUID), UUID] =
    sql"""
      select id
      from orders
      where user_id = $uuid
        and id = $uuid
    """.query(uuid)
