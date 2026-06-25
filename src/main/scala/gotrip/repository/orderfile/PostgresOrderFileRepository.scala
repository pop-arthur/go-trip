package gotrip.repository.orderfile

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

  override def create(userId: UserId, orderId: OrderId, file: OrderFileCreate): F[Option[OrderFile]] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderFileRepository.createQuery).flatMap { query =>
        query.option((file.file_url.value, SkunkCodecs.encodeFileType(file.file_type), file.parsed_data.map(_.noSpaces), userId.value, orderId.value))
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
    (int8 ~ int8 ~ text ~ text ~ text.opt ~ timestamptz).map {
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

  val listByOrderQuery: Query[(Long, Long), OrderFile] =
    sql"""
      select f.id, f.order_id, f.file_url, f.file_type, f.parsed_data::text, f.uploaded_at
      from order_files f
      inner join orders o on o.id = f.order_id
      where o.user_id = $int8
        and f.order_id = $int8
      order by f.uploaded_at desc, f.id desc
    """.query(orderFileDecoder)

  val findByOrderQuery: Query[(Long, Long, Long), OrderFile] =
    sql"""
      select f.id, f.order_id, f.file_url, f.file_type, f.parsed_data::text, f.uploaded_at
      from order_files f
      inner join orders o on o.id = f.order_id
      where o.user_id = $int8
        and f.order_id = $int8
        and f.id = $int8
    """.query(orderFileDecoder)

  val createQuery: Query[(String, String, Option[String], Long, Long), OrderFile] =
    sql"""
      insert into order_files (order_id, file_url, file_type, parsed_data)
      select o.id, $text, $text, ${text.opt}::jsonb
      from orders o
      where o.user_id = $int8
        and o.id = $int8
      returning id, order_id, file_url, file_type, parsed_data::text, uploaded_at
    """.query(orderFileDecoder)

  val deleteQuery: Query[(Long, Long, Long), Long] =
    sql"""
      delete from order_files f
      using orders o
      where o.id = f.order_id
        and o.user_id = $int8
        and f.order_id = $int8
        and f.id = $int8
      returning f.id
    """.query(int8)

  val orderExistsForUserQuery: Query[(Long, Long), Long] =
    sql"""
      select id
      from orders
      where user_id = $int8
        and id = $int8
    """.query(int8)
