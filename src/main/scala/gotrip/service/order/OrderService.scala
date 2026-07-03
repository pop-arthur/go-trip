package gotrip.service.order

import cats.Monad
import cats.data.EitherT
import cats.effect.{Clock, Sync}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.location.LocationId
import gotrip.domain.notification.*
import gotrip.domain.order.*
import gotrip.domain.provider.ProviderId
import gotrip.domain.trip.TripId
import gotrip.domain.user.*
import gotrip.repository.notificationpreference.NotificationPreferenceRepository
import gotrip.repository.order.OrderRepository
import gotrip.service.GeneratedData
import gotrip.service.notification.NotificationService
import io.circe.Json

import java.time.OffsetDateTime

final class OrderService[F[_]: Sync: Clock](
  repository: OrderRepository[F],
  notificationPreferenceRepository: NotificationPreferenceRepository[F],
  notificationService: NotificationService[F]
):

  import OrderServiceError.*

  def listByTrip(
    userId: UserId,
    tripId: TripId,
    params: OrderSearchParams
  ): F[Either[OrderServiceError, List[Order]]] =
    (for {
      _ <- ensureTripExists(userId, tripId)
      orders <- EitherT.liftF(repository.listByTrip(userId, tripId, params))
    } yield orders).value

  def findByUser(userId: UserId, orderId: OrderId): F[Either[OrderServiceError, Order]] =
    EitherT.fromOptionF(repository.findByUser(userId, orderId), OrderNotFound(orderId)).value

  def create(
    userId: UserId,
    tripId: TripId,
    order: OrderCreate
  ): F[Either[OrderServiceError, Order]] =
    (for {
      _ <- ensureTripExists(userId, tripId)
      _ <- validateDateTimeRange(order.start_datetime, order.end_datetime)
      _ <- ensureProviderExists(order.provider_id)
      _ <- ensureLocationExists(order.departure_location_id)
      _ <- ensureLocationExists(order.arrival_location_id)
      materialized <- EitherT.liftF(materializeOrder(userId, tripId, order))
      created <- EitherT.liftF(repository.create(materialized))
    } yield created).value

  def update(
    userId: UserId,
    orderId: OrderId,
    order: OrderUpdate
  ): F[Either[OrderServiceError, Order]] =
    (for {
      current <- EitherT.fromOptionF(repository.findByUser(userId, orderId), OrderNotFound(orderId))
      _ <- validateDateTimeRange(nextStartDateTime(current, order), nextEndDateTime(current, order))
      _ <- ensureProviderExists(order.provider_id)
      _ <- ensureLocationExists(order.departure_location_id)
      _ <- ensureLocationExists(order.arrival_location_id)
      materialized <- EitherT.liftF(materializeOrderUpdate(current, order))
      updated <- EitherT.fromOptionF(repository.update(materialized), OrderNotFound(orderId))
    } yield updated).value

  def delete(userId: UserId, orderId: OrderId): F[Either[OrderServiceError, Unit]] =
    (for {
      deleted <- EitherT.liftF(repository.delete(userId, orderId))
      _ <- if deleted then EitherT.rightT[F, OrderServiceError](())
           else EitherT.leftT[F, Unit](OrderNotFound(orderId))
    } yield ()).value

  def updateStatus(
    userId: UserId,
    orderId: OrderId,
    update: OrderStatusUpdate,
    source: OrderStatusEventSource = OrderStatusEventSource.UserEdit
  ): F[Either[OrderServiceError, Order]] =
    (for {
      current <- EitherT.fromOptionF(repository.findByUser(userId, orderId), OrderNotFound(orderId))
      updated <- updateStatusForCurrent(orderId, current, update, source)
    } yield updated).value

  def adminUpdateStatus(
    orderId: OrderId,
    update: OrderStatusUpdate
  ): F[Either[OrderServiceError, Order]] =
    (for {
      current <- EitherT.fromOptionF(repository.findById(orderId), OrderNotFound(orderId))
      updated <- updateStatusForCurrent(orderId, current, update, OrderStatusEventSource.AdminSimulation)
    } yield updated).value

  private def ensureTripExists(userId: UserId, tripId: TripId): EitherT[F, OrderServiceError, Unit] =
    EitherT {
      repository.tripExistsForUser(userId, tripId).map { exists =>
        Either.cond(exists, (), TripNotFound(tripId))
      }
    }

  private def ensureProviderExists(providerId: Option[ProviderId]): EitherT[F, OrderServiceError, Unit] =
    providerId match
      case Some(id) =>
        EitherT {
          repository.providerExists(id).map { exists =>
            Either.cond(exists, (), ProviderNotFound(id))
          }
        }
      case None =>
        EitherT.rightT(())

  private def ensureLocationExists(locationId: Option[LocationId]): EitherT[F, OrderServiceError, Unit] =
    locationId match
      case Some(id) =>
        EitherT {
          repository.locationExists(id).map { exists =>
            Either.cond(exists, (), LocationNotFound(id))
          }
        }
      case None =>
        EitherT.rightT(())

  private def validateDateTimeRange(
    startDateTime: Option[OffsetDateTime],
    endDateTime: Option[OffsetDateTime]
  ): EitherT[F, OrderServiceError, Unit] =
    EitherT.fromEither {
      (startDateTime, endDateTime) match
        case (Some(start), Some(end)) if start.isAfter(end) =>
          Left(InvalidDateTimeRange)
        case _ =>
          Right(())
    }

  private def nextStartDateTime(current: Order, update: OrderUpdate): Option[OffsetDateTime] =
    update.start_datetime.orElse(current.start_datetime)

  private def nextEndDateTime(current: Order, update: OrderUpdate): Option[OffsetDateTime] =
    update.end_datetime.orElse(current.end_datetime)

  private def updateStatusForCurrent(
    orderId: OrderId,
    current: Order,
    update: OrderStatusUpdate,
    source: OrderStatusEventSource
  ): EitherT[F, OrderServiceError, Order] =
    for {
      _ <- validateDateTimeRange(update.new_start_datetime.orElse(current.start_datetime), current.end_datetime)
      materialized <- EitherT.liftF(materializeStatusUpdate(current, update))
      updated <- EitherT.fromOptionF(repository.updateStatus(materialized), OrderNotFound(orderId))
      event <- EitherT.liftF(statusEvent(current, update, source))
      _ <- EitherT.liftF(repository.insertStatusEvent(event))
      _ <- EitherT.liftF(sendStatusNotificationIfEnabled(updated, update.reason))
    } yield updated

  private def materializeOrder(userId: UserId, tripId: TripId, create: OrderCreate): F[Order] =
    for
      id <- GeneratedData.newId[F]
      now <- GeneratedData.now[F]
    yield Order(
      id = OrderId(id),
      user_id = userId,
      trip_id = tripId,
      provider_id = create.provider_id,
      service_type = create.service_type,
      external_order_id = create.external_order_id,
      title = create.title,
      status = create.status.getOrElse(OrderStatus.PendingVerification),
      price_amount = create.price_amount,
      price_currency = create.price_currency,
      start_datetime = create.start_datetime,
      end_datetime = create.end_datetime,
      departure_location_id = create.departure_location_id,
      arrival_location_id = create.arrival_location_id,
      created_at = now,
      updated_at = now
    )

  private def materializeOrderUpdate(current: Order, update: OrderUpdate): F[Order] =
    GeneratedData.now[F].map { now =>
      current.copy(
        provider_id = update.provider_id.orElse(current.provider_id),
        service_type = update.service_type.getOrElse(current.service_type),
        external_order_id = update.external_order_id.orElse(current.external_order_id),
        title = update.title.getOrElse(current.title),
        status = update.status.getOrElse(current.status),
        price_amount = update.price_amount.orElse(current.price_amount),
        price_currency = update.price_currency.orElse(current.price_currency),
        start_datetime = update.start_datetime.orElse(current.start_datetime),
        end_datetime = update.end_datetime.orElse(current.end_datetime),
        departure_location_id = update.departure_location_id.orElse(current.departure_location_id),
        arrival_location_id = update.arrival_location_id.orElse(current.arrival_location_id),
        updated_at = now
      )
    }

  private def materializeStatusUpdate(current: Order, update: OrderStatusUpdate): F[Order] =
    GeneratedData.now[F].map { now =>
      current.copy(
        status = update.status,
        start_datetime = update.new_start_datetime.orElse(current.start_datetime),
        updated_at = now
      )
    }

  private def statusEvent(
    current: Order,
    update: OrderStatusUpdate,
    source: OrderStatusEventSource
  ): F[OrderStatusEvent] =
    for
      id <- GeneratedData.newId[F]
      now <- GeneratedData.now[F]
    yield OrderStatusEvent(
        id = OrderStatusEventId(id),
        order_id = current.id,
        old_status = Some(current.status),
        new_status = update.status,
        reason = update.reason,
        payload = update.payload,
        source = source,
        created_at = now
      )

  private def sendStatusNotificationIfEnabled(order: Order, reason: Option[String]): F[Unit] =
    notificationPreferenceRepository.getByUserId(order.user_id).flatMap { preference =>
      val enabled = preference.forall(_.isEnabled)
      if enabled then
        notificationService
          .send(
            userId = NotificationUserId(order.user_id.value),
            notificationType = NotificationType.StatusChange,
            title = NotificationTitle("Order status changed"),
            body = NotificationBody(statusNotificationBody(order, reason)),
            orderId = NotificationOrderId(Some(order.id.value))
          )
          .map(_ => ())
      else
        Monad[F].pure(())
    }

  private def statusNotificationBody(order: Order, reason: Option[String]): String =
    val status = gotrip.repository.SkunkCodecs.encodeOrderStatus(order.status)
    reason match
      case Some(value) => s"${order.title.value} is now $status: $value"
      case None        => s"${order.title.value} is now $status"

enum OrderServiceError:
  case TripNotFound(id: TripId)
  case OrderNotFound(id: OrderId)
  case ProviderNotFound(id: ProviderId)
  case LocationNotFound(id: LocationId)
  case InvalidDateTimeRange
