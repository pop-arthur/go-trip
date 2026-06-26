package gotrip.service.order

import cats.Id
import gotrip.domain.additionalservice.ServiceType
import gotrip.domain.location.*
import gotrip.domain.notification.*
import gotrip.domain.notificationpreference.*
import gotrip.domain.order.*
import gotrip.domain.provider.*
import gotrip.domain.trip.*
import gotrip.domain.user.*
import gotrip.repository.notification.NotificationRepository
import gotrip.repository.notificationpreference.NotificationPreferenceRepository
import gotrip.repository.order.OrderRepository
import gotrip.service.notification.NotificationService
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Instant, OffsetDateTime}

final class OrderServiceSpec extends AnyWordSpec with Matchers with MockFactory:

  "OrderService" should {
    "reject create when trip is not owned by the user" in {
      val repository = mock[OrderRepository[Id]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.tripExistsForUser.expects(userId, tripId).returning(false)

      service.create(userId, tripId, orderCreate) shouldBe Left(OrderServiceError.TripNotFound(tripId))
    }

    "reject create when provider does not exist" in {
      val repository = mock[OrderRepository[Id]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.tripExistsForUser.expects(userId, tripId).returning(true)
      repository.providerExists.expects(providerId).returning(false)

      service.create(userId, tripId, orderCreate) shouldBe Left(OrderServiceError.ProviderNotFound(providerId))
    }

    "reject create when start datetime is after end datetime" in {
      val repository = mock[OrderRepository[Id]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.tripExistsForUser.expects(userId, tripId).returning(true)

      service.create(userId, tripId, invalidDateOrderCreate) shouldBe Left(OrderServiceError.InvalidDateTimeRange)
    }

    "write a status event and notification when preferences are enabled" in {
      val repository = mock[OrderRepository[Id]]
      val notifications = new RecordingNotificationRepository
      val service = serviceWith(repository, preference = Some(enabledPreference), notifications = notifications)

      repository.findByUser.expects(userId, orderId).returning(Some(order))
      repository.updateStatus.expects(userId, orderId, statusUpdate).returning(Some(updatedOrder))
      repository.insertStatusEvent.expects(where { (event: OrderStatusEvent) =>
        event.order_id == orderId &&
        event.old_status.contains(OrderStatus.PendingVerification) &&
        event.new_status == OrderStatus.Confirmed &&
        event.source == OrderStatusEventSource.UserEdit
      }).returning(statusEvent)

      service.updateStatus(userId, orderId, statusUpdate) shouldBe Right(updatedOrder)
      notifications.created.size shouldBe 1
      notifications.created.head.orderId.value shouldBe Some(orderId.value)
    }

    "skip notification when preferences are disabled" in {
      val repository = mock[OrderRepository[Id]]
      val notifications = new RecordingNotificationRepository
      val service = serviceWith(repository, preference = Some(disabledPreference), notifications = notifications)

      repository.findByUser.expects(userId, orderId).returning(Some(order))
      repository.updateStatus.expects(userId, orderId, statusUpdate).returning(Some(updatedOrder))
      repository.insertStatusEvent.expects(*).returning(statusEvent)

      service.updateStatus(userId, orderId, statusUpdate) shouldBe Right(updatedOrder)
      notifications.created shouldBe empty
    }
  }

  private def serviceWith(
    repository: OrderRepository[Id],
    preference: Option[NotificationPreference],
    notifications: RecordingNotificationRepository
  ): OrderService[Id] =
    OrderService[Id](
      repository,
      new StaticNotificationPreferenceRepository(preference),
      NotificationService[Id](notifications)
    )

  private val userId = UserId(1L)
  private val tripId = TripId(10L)
  private val orderId = OrderId(100L)
  private val providerId = ProviderId(5L)
  private val departureLocationId = LocationId(20L)
  private val arrivalLocationId = LocationId(21L)
  private val startDateTime = OffsetDateTime.parse("2026-06-10T12:00:00Z")
  private val endDateTime = OffsetDateTime.parse("2026-06-10T14:00:00Z")

  private val orderCreate = OrderCreate(
    provider_id = Some(providerId),
    service_type = ServiceType.Flight,
    external_order_id = Some("VN123456"),
    title = OrderTitle("Flight Hanoi to Bangkok"),
    status = None,
    price_amount = Some(129.99),
    price_currency = Some("USD"),
    start_datetime = Some(startDateTime),
    end_datetime = Some(endDateTime),
    departure_location_id = Some(departureLocationId),
    arrival_location_id = Some(arrivalLocationId)
  )

  private val invalidDateOrderCreate = orderCreate.copy(
    start_datetime = Some(endDateTime.plusHours(1))
  )

  private val order = Order(
    id = orderId,
    user_id = userId,
    trip_id = tripId,
    provider_id = Some(providerId),
    service_type = ServiceType.Flight,
    external_order_id = Some("VN123456"),
    title = OrderTitle("Flight Hanoi to Bangkok"),
    status = OrderStatus.PendingVerification,
    price_amount = Some(129.99),
    price_currency = Some("USD"),
    start_datetime = Some(startDateTime),
    end_datetime = Some(endDateTime),
    departure_location_id = Some(departureLocationId),
    arrival_location_id = Some(arrivalLocationId),
    created_at = Instant.parse("2026-06-01T10:00:00Z"),
    updated_at = Instant.parse("2026-06-01T10:00:00Z")
  )

  private val updatedOrder = order.copy(status = OrderStatus.Confirmed)
  private val statusUpdate = OrderStatusUpdate(status = OrderStatus.Confirmed, reason = Some("Confirmed by provider"))
  private val statusEvent = OrderStatusEvent(
    id = OrderStatusEventId(1L),
    order_id = orderId,
    old_status = Some(OrderStatus.PendingVerification),
    new_status = OrderStatus.Confirmed,
    reason = Some("Confirmed by provider"),
    payload = None,
    source = OrderStatusEventSource.UserEdit,
    created_at = Instant.parse("2026-06-01T10:05:00Z")
  )

  private val enabledPreference = NotificationPreference(
    id = NotificationPreferenceId(1L),
    userId = userId,
    isEnabled = true,
    createdAt = Instant.parse("2026-06-01T10:00:00Z"),
    updatedAt = Instant.parse("2026-06-01T10:00:00Z")
  )

  private val disabledPreference = enabledPreference.copy(isEnabled = false)

private final class StaticNotificationPreferenceRepository(
  preference: Option[NotificationPreference]
) extends NotificationPreferenceRepository[Id]:
  override def getByUserId(userId: UserId): Option[NotificationPreference] =
    preference.filter(_.userId == userId)

  override def upsert(userId: UserId, isEnabled: Boolean): NotificationPreference =
    NotificationPreference(
      id = NotificationPreferenceId(1L),
      userId = userId,
      isEnabled = isEnabled,
      createdAt = Instant.parse("2026-06-01T10:00:00Z"),
      updatedAt = Instant.parse("2026-06-01T10:00:00Z")
    )

private final class RecordingNotificationRepository extends NotificationRepository[Id]:
  var created: List[UserNotification] = Nil

  override def create(notification: UserNotification): UserNotification =
    created = notification :: created
    notification.copy(id = NotificationId(created.size.toLong))

  override def findById(id: NotificationId): Option[UserNotification] =
    created.find(_.id == id)

  override def findByUserId(userId: NotificationUserId, limit: Int, offset: Int): List[UserNotification] =
    created.filter(_.userId == userId).slice(offset, offset + limit)

  override def markAsRead(id: NotificationId): Int = 0
  override def markAllAsRead(userId: NotificationUserId): Int = 0
  override def delete(id: NotificationId): Int = 0
  override def deleteAllForUser(userId: NotificationUserId): Int = 0
