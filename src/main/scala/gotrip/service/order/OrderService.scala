package gotrip.service.order

import cats.Monad
import cats.data.EitherT
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
import gotrip.service.notification.NotificationService
import io.circe.Json

import java.time.{Instant, OffsetDateTime}

final class OrderService[F[_]: Monad](
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
      created <- EitherT.liftF(repository.create(userId, tripId, order))
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
      updated <- EitherT.fromOptionF(repository.update(userId, orderId, order), OrderNotFound(orderId))
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
      _ <- validateDateTimeRange(update.new_start_datetime.orElse(current.start_datetime), current.end_datetime)
      updated <- EitherT.fromOptionF(repository.updateStatus(userId, orderId, update), OrderNotFound(orderId))
      _ <- EitherT.liftF(repository.insertStatusEvent(statusEvent(current, update, source)))
      _ <- EitherT.liftF(sendStatusNotificationIfEnabled(updated, update.reason))
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

  private def statusEvent(
    current: Order,
    update: OrderStatusUpdate,
    source: OrderStatusEventSource
  ): OrderStatusEvent =
    OrderStatusEvent(
      id = OrderStatusEventId(0L),
      order_id = current.id,
      old_status = Some(current.status),
      new_status = update.status,
      reason = update.reason,
      payload = update.payload,
      source = source,
      created_at = Instant.now()
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
