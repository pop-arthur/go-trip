package gotrip.repository.order

import java.util.UUID

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.additionalservice.ServiceType
import gotrip.domain.location.*
import gotrip.domain.order.*
import gotrip.domain.provider.*
import gotrip.domain.trip.*
import gotrip.domain.user.*
import gotrip.repository.SkunkCodecs
import io.circe.Json
import io.circe.parser.parse
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

import java.time.{Instant, LocalDate, OffsetDateTime, ZoneOffset}

final class PostgresOrderRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends OrderRepository[F]:

  override def listByTrip(userId: UserId, tripId: TripId, params: OrderSearchParams): F[List[Order]] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderRepository.listByTripQuery).flatMap { query =>
        query.stream(PostgresOrderRepository.toListInput(userId, tripId, params), 64).compile.toList
      }
    }

  override def listExternalByUser(userId: UserId): F[List[Order]] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderRepository.listExternalByUserQuery).flatMap { query =>
        query.stream(userId.value, 64).compile.toList
      }
    }

  override def findByUser(userId: UserId, orderId: OrderId): F[Option[Order]] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderRepository.findByUserQuery).flatMap { query =>
        query.option((userId.value, orderId.value))
      }
    }

  override def findById(orderId: OrderId): F[Option[Order]] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderRepository.findByIdQuery).flatMap { query =>
        query.option(orderId.value)
      }
    }

  override def create(order: Order): F[Order] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderRepository.createQuery).flatMap { query =>
        query.unique(
          (
            order.id.value,
            order.user_id.value,
            order.trip_id.value,
            order.provider_id.map(_.value),
            order.service_type,
            order.external_order_id,
            order.title.value,
            SkunkCodecs.encodeOrderStatus(order.status),
            order.price_amount,
            order.price_currency,
            order.start_datetime,
            order.end_datetime,
            order.departure_location_id.map(_.value),
            order.arrival_location_id.map(_.value),
            PostgresOrderRepository.toOffset(order.created_at),
            PostgresOrderRepository.toOffset(order.updated_at)
          )
        )
      }
    }

  override def update(order: Order): F[Option[Order]] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderRepository.updateQuery).flatMap { query =>
        query.option(PostgresOrderRepository.toOrderUpdateInput(order))
      }
    }

  override def delete(userId: UserId, orderId: OrderId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderRepository.deleteQuery).flatMap { query =>
        query.option((userId.value, orderId.value)).map(_.isDefined)
      }
    }

  override def updateStatus(order: Order): F[Option[Order]] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderRepository.updateStatusQuery).flatMap { query =>
        query.option(
          (
            SkunkCodecs.encodeOrderStatus(order.status),
            order.start_datetime,
            PostgresOrderRepository.toOffset(order.updated_at),
            order.user_id.value,
            order.id.value
          )
        )
      }
    }

  override def insertStatusEvent(event: OrderStatusEvent): F[OrderStatusEvent] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderRepository.insertStatusEventQuery).flatMap { query =>
        query.unique(
          (
            event.id.value,
            event.order_id.value,
            event.old_status.map(SkunkCodecs.encodeOrderStatus),
            SkunkCodecs.encodeOrderStatus(event.new_status),
            event.reason,
            event.payload.map(_.noSpaces),
            SkunkCodecs.encodeOrderStatusEventSource(event.source),
            PostgresOrderRepository.toOffset(event.created_at)
          )
        )
      }
    }

  override def tripExistsForUser(userId: UserId, tripId: TripId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderRepository.tripExistsForUserQuery).flatMap { query =>
        query.option((userId.value, tripId.value)).map(_.isDefined)
      }
    }

  override def providerExists(providerId: ProviderId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderRepository.providerExistsQuery).flatMap { query =>
        query.option(providerId.value).map(_.isDefined)
      }
    }

  override def locationExists(locationId: LocationId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresOrderRepository.locationExistsQuery).flatMap { query =>
        query.option(locationId.value).map(_.isDefined)
      }
    }

object PostgresOrderRepository:
  private type ListInput = (
    UUID,
    UUID,
    Boolean,
    ServiceType,
    Boolean,
    String,
    Boolean,
    LocalDate,
    Boolean,
    LocalDate
  )

  def make[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): OrderRepository[F] =
    new PostgresOrderRepository(sessionPool)

  private val orderDecoder: Decoder[Order] =
    (uuid ~ uuid ~ uuid ~ uuid.opt ~ SkunkCodecs.serviceType ~ text.opt ~ text ~ text ~ float8.opt ~ text.opt ~
      timestamptz.opt ~ timestamptz.opt ~ uuid.opt ~ uuid.opt ~ timestamptz ~ timestamptz)
      .map {
        case id ~ userId ~ tripId ~ providerId ~ serviceType ~ externalOrderId ~ title ~ status ~ priceAmount ~
            priceCurrency ~ startDateTime ~ endDateTime ~ departureLocationId ~ arrivalLocationId ~ createdAt ~ updatedAt =>
          Order(
            id = OrderId(id),
            user_id = UserId(userId),
            trip_id = TripId(tripId),
            provider_id = providerId.map(ProviderId.apply),
            service_type = serviceType,
            external_order_id = externalOrderId,
            title = OrderTitle(title),
            status = SkunkCodecs.decodeOrderStatus(status).get,
            price_amount = priceAmount,
            price_currency = priceCurrency,
            start_datetime = startDateTime,
            end_datetime = endDateTime,
            departure_location_id = departureLocationId.map(LocationId.apply),
            arrival_location_id = arrivalLocationId.map(LocationId.apply),
            created_at = createdAt.toInstant,
            updated_at = updatedAt.toInstant
          )
      }

  private val statusEventDecoder: Decoder[OrderStatusEvent] =
    (uuid ~ uuid ~ text.opt ~ text ~ text.opt ~ text.opt ~ text ~ timestamptz).map {
      case id ~ orderId ~ oldStatus ~ newStatus ~ reason ~ payload ~ source ~ createdAt =>
        OrderStatusEvent(
          id = OrderStatusEventId(id),
          order_id = OrderId(orderId),
          old_status = oldStatus.flatMap(SkunkCodecs.decodeOrderStatus),
          new_status = SkunkCodecs.decodeOrderStatus(newStatus).get,
          reason = reason,
          payload = payload.flatMap(value => parse(value).toOption),
          source = SkunkCodecs.decodeOrderStatusEventSource(source).get,
          created_at = createdAt.toInstant
        )
    }

  val listByTripQuery: Query[ListInput, Order] =
    sql"""
      select id, user_id, trip_id, provider_id, service_type, external_order_id::text, title::text, status,
             price_amount::float8, price_currency::text, start_datetime, end_datetime,
             departure_location_id, arrival_location_id, created_at, updated_at
      from orders
      where user_id = $uuid
        and trip_id = $uuid
        and ($bool or service_type = ${SkunkCodecs.serviceType})
        and ($bool or status = $text)
        and ($bool or start_datetime::date >= $date)
        and ($bool or end_datetime::date <= $date)
      order by coalesce(start_datetime, timestamptz '9999-12-31 00:00:00+00'), id
    """.query(orderDecoder)

  val listExternalByUserQuery: Query[UUID, Order] =
    sql"""
      select id, user_id, trip_id, provider_id, service_type, external_order_id::text, title::text, status,
             price_amount::float8, price_currency::text, start_datetime, end_datetime,
             departure_location_id, arrival_location_id, created_at, updated_at
      from orders
      where user_id = $uuid
        and external_order_id is not null
      order by updated_at desc, id
    """.query(orderDecoder)

  val findByUserQuery: Query[(UUID, UUID), Order] =
    sql"""
      select id, user_id, trip_id, provider_id, service_type, external_order_id::text, title::text, status,
             price_amount::float8, price_currency::text, start_datetime, end_datetime,
             departure_location_id, arrival_location_id, created_at, updated_at
      from orders
      where user_id = $uuid
        and id = $uuid
    """.query(orderDecoder)

  val findByIdQuery: Query[UUID, Order] =
    sql"""
      select id, user_id, trip_id, provider_id, service_type, external_order_id::text, title::text, status,
             price_amount::float8, price_currency::text, start_datetime, end_datetime,
             departure_location_id, arrival_location_id, created_at, updated_at
      from orders
      where id = $uuid
    """.query(orderDecoder)

  private type OrderUpdateInput =
    (Option[UUID], ServiceType, Option[String], String, String, Option[Double], Option[String], Option[OffsetDateTime], Option[OffsetDateTime], Option[UUID], Option[UUID], OffsetDateTime, UUID, UUID)

  private def toOffset(instant: Instant): OffsetDateTime =
    instant.atOffset(ZoneOffset.UTC)

  val createQuery
      : Query[(UUID, UUID, UUID, Option[UUID], ServiceType, Option[String], String, String, Option[Double], Option[String], Option[OffsetDateTime], Option[OffsetDateTime], Option[UUID], Option[UUID], OffsetDateTime, OffsetDateTime), Order] =
    sql"""
      insert into orders (
        id, user_id, trip_id, provider_id, service_type, external_order_id, title, status,
        price_amount, price_currency, start_datetime, end_datetime, departure_location_id, arrival_location_id,
        created_at, updated_at
      )
      values (
        $uuid, $uuid, $uuid, ${uuid.opt}, ${SkunkCodecs.serviceType}, ${text.opt}, $text, $text,
        ${float8.opt}, ${text.opt}, ${timestamptz.opt}, ${timestamptz.opt}, ${uuid.opt}, ${uuid.opt},
        $timestamptz, $timestamptz
      )
      returning id, user_id, trip_id, provider_id, service_type, external_order_id::text, title::text, status,
                price_amount::float8, price_currency::text, start_datetime, end_datetime,
                departure_location_id, arrival_location_id, created_at, updated_at
    """.query(orderDecoder)

  val deleteQuery: Query[(UUID, UUID), UUID] =
    sql"""
      delete from orders
      where user_id = $uuid
        and id = $uuid
      returning id
    """.query(uuid)

  val updateQuery: Query[OrderUpdateInput, Order] =
    sql"""
      update orders
      set provider_id = ${uuid.opt},
          service_type = ${SkunkCodecs.serviceType},
          external_order_id = ${text.opt},
          title = $text,
          status = $text,
          price_amount = ${float8.opt},
          price_currency = ${text.opt},
          start_datetime = ${timestamptz.opt},
          end_datetime = ${timestamptz.opt},
          departure_location_id = ${uuid.opt},
          arrival_location_id = ${uuid.opt},
          updated_at = $timestamptz
      where user_id = $uuid
        and id = $uuid
      returning id, user_id, trip_id, provider_id, service_type, external_order_id::text, title::text, status,
                price_amount::float8, price_currency::text, start_datetime, end_datetime,
                departure_location_id, arrival_location_id, created_at, updated_at
    """.query(orderDecoder)

  val updateStatusQuery: Query[(String, Option[OffsetDateTime], OffsetDateTime, UUID, UUID), Order] =
    sql"""
      update orders
      set status = $text,
          start_datetime = ${timestamptz.opt},
          updated_at = $timestamptz
      where user_id = $uuid
        and id = $uuid
      returning id, user_id, trip_id, provider_id, service_type, external_order_id::text, title::text, status,
                price_amount::float8, price_currency::text, start_datetime, end_datetime,
                departure_location_id, arrival_location_id, created_at, updated_at
    """.query(orderDecoder)

  val insertStatusEventQuery: Query[(UUID, UUID, Option[String], String, Option[String], Option[String], String, OffsetDateTime), OrderStatusEvent] =
    sql"""
      insert into order_status_events (id, order_id, old_status, new_status, reason, payload, source, created_at)
      values ($uuid, $uuid, ${text.opt}, $text, ${text.opt}, ${text.opt}::jsonb, $text, $timestamptz)
      returning id, order_id, old_status, new_status, reason, payload::text, source, created_at
    """.query(statusEventDecoder)

  val tripExistsForUserQuery: Query[(UUID, UUID), UUID] =
    sql"""
      select id
      from trips
      where user_id = $uuid
        and id = $uuid
    """.query(uuid)

  val providerExistsQuery: Query[UUID, UUID] =
    sql"select id from providers where id = $uuid".query(uuid)

  val locationExistsQuery: Query[UUID, UUID] =
    sql"select id from locations where id = $uuid".query(uuid)

  private def toOrderUpdateInput(order: Order): OrderUpdateInput =
    (
      order.provider_id.map(_.value),
      order.service_type,
      order.external_order_id,
      order.title.value,
      SkunkCodecs.encodeOrderStatus(order.status),
      order.price_amount,
      order.price_currency,
      order.start_datetime,
      order.end_datetime,
      order.departure_location_id.map(_.value),
      order.arrival_location_id.map(_.value),
      toOffset(order.updated_at),
      order.user_id.value,
      order.id.value
    )

  private def toListInput(userId: UserId, tripId: TripId, params: OrderSearchParams): ListInput =
    (
      userId.value,
      tripId.value,
      params.serviceType.isEmpty,
      params.serviceType.getOrElse(ServiceType.Other),
      params.status.isEmpty,
      params.status.map(SkunkCodecs.encodeOrderStatus).getOrElse("PENDING_VERIFICATION"),
      params.fromDate.isEmpty,
      params.fromDate.getOrElse(LocalDate.of(1, 1, 1)),
      params.toDate.isEmpty,
      params.toDate.getOrElse(LocalDate.of(9999, 12, 31))
    )
