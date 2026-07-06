package gotrip.service.order

import cats.effect.IO
import cats.effect.unsafe.implicits.global
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
import gotrip.service.{GeneratedData, GeneratedDataTestSupport}
import gotrip.service.notification.NotificationService
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.{Instant, OffsetDateTime}
import java.util.UUID

final class OrderServiceSpec extends AnyWordSpec with Matchers with MockFactory with GeneratedDataTestSupport:

  "OrderService" should {
    "list orders for an owned trip" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.listByTrip.expects(userId, tripId, searchParams).returning(IO.pure(List(order)))

      service.listByTrip(userId, tripId, searchParams).unsafeRunSync() shouldBe Right(List(order))
    }

    "return trip not found when listing orders for an inaccessible trip" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(false))

      service.listByTrip(userId, tripId, searchParams).unsafeRunSync() shouldBe Left(OrderServiceError.TripNotFound(tripId))
    }

    "return order not found when finding an inaccessible order" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.findByUser.expects(userId, orderId).returning(IO.pure(None))

      service.findByUser(userId, orderId).unsafeRunSync() shouldBe Left(OrderServiceError.OrderNotFound(orderId))
    }

    "create an order with default status" in {
      val repository = mock[OrderRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository, generatedData)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.providerExists.expects(providerId).returning(IO.pure(true))
      repository.locationExists.expects(departureLocationId).returning(IO.pure(true))
      repository.locationExists.expects(arrivalLocationId).returning(IO.pure(true))
      expectGeneratedId(generatedData, orderId.value)
      expectGeneratedNow(generatedData, order.created_at)
      repository.create.expects(order).returning(IO.pure(order))

      service.create(userId, tripId, orderCreate).unsafeRunSync() shouldBe Right(order)
    }

    "reject create when trip is not owned by the user" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(false))

      service.create(userId, tripId, orderCreate).unsafeRunSync() shouldBe Left(OrderServiceError.TripNotFound(tripId))
    }

    "reject create when provider does not exist" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.providerExists.expects(providerId).returning(IO.pure(false))

      service.create(userId, tripId, orderCreate).unsafeRunSync() shouldBe Left(OrderServiceError.ProviderNotFound(providerId))
    }

    "reject create when start datetime is after end datetime" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))

      service.create(userId, tripId, invalidDateOrderCreate).unsafeRunSync() shouldBe Left(OrderServiceError.InvalidDateTimeRange)
    }

    "reject create when departure location does not exist" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.providerExists.expects(providerId).returning(IO.pure(true))
      repository.locationExists.expects(departureLocationId).returning(IO.pure(false))

      service.create(userId, tripId, orderCreate).unsafeRunSync() shouldBe
        Left(OrderServiceError.LocationNotFound(departureLocationId))
    }

    "reject create when arrival location does not exist" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
      repository.providerExists.expects(providerId).returning(IO.pure(true))
      repository.locationExists.expects(departureLocationId).returning(IO.pure(true))
      repository.locationExists.expects(arrivalLocationId).returning(IO.pure(false))

      service.create(userId, tripId, orderCreate).unsafeRunSync() shouldBe
        Left(OrderServiceError.LocationNotFound(arrivalLocationId))
    }

    "update an order with merged fields" in {
      val repository = mock[OrderRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository, generatedData)

      repository.findByUser.expects(userId, orderId).returning(IO.pure(Some(order)))
      repository.providerExists.expects(providerId).returning(IO.pure(true))
      repository.locationExists.expects(arrivalLocationId).returning(IO.pure(true))
      expectGeneratedNow(generatedData, updatedOrderDetails.updated_at)
      repository.update.expects(updatedOrderDetails).returning(IO.pure(Some(updatedOrderDetails)))

      service.update(userId, orderId, orderUpdate).unsafeRunSync() shouldBe Right(updatedOrderDetails)
    }

    "return not found when updating an inaccessible order" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.findByUser.expects(userId, orderId).returning(IO.pure(None))

      service.update(userId, orderId, orderUpdate).unsafeRunSync() shouldBe Left(OrderServiceError.OrderNotFound(orderId))
    }

    "reject update when merged datetime range is invalid" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.findByUser.expects(userId, orderId).returning(IO.pure(Some(order)))

      service.update(userId, orderId, invalidOrderUpdate).unsafeRunSync() shouldBe
        Left(OrderServiceError.InvalidDateTimeRange)
    }

    "delete an order" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.delete.expects(userId, orderId).returning(IO.pure(true))

      service.delete(userId, orderId).unsafeRunSync() shouldBe Right(())
    }

    "return not found when deleting an inaccessible order" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.delete.expects(userId, orderId).returning(IO.pure(false))

      service.delete(userId, orderId).unsafeRunSync() shouldBe Left(OrderServiceError.OrderNotFound(orderId))
    }

    "write a status event and notification when preferences are enabled" in {
      val repository = mock[OrderRepository[IO]]
      val notifications = new RecordingNotificationRepository
      val generatedData = generatedDataMock
      val service = serviceWith(repository, preference = Some(enabledPreference), notifications = notifications, generatedData)

      repository.findByUser.expects(userId, orderId).returning(IO.pure(Some(order)))
      inSequence {
        expectGeneratedNow(generatedData, statusAt)
        expectGeneratedId(generatedData, statusEventId.value)
        expectGeneratedNow(generatedData, statusAt)
        expectGeneratedId(generatedData, notificationId.value)
        expectGeneratedNow(generatedData, notificationAt)
      }
      repository.updateStatus.expects(updatedOrder).returning(IO.pure(Some(updatedOrder)))
      repository.insertStatusEvent.expects(statusEvent).returning(IO.pure(statusEvent))

      service.updateStatus(userId, orderId, statusUpdate).unsafeRunSync() shouldBe Right(updatedOrder)
      notifications.created.size shouldBe 1
      notifications.created.head.orderId.value shouldBe Some(orderId.value)
    }

    "skip notification when preferences are disabled" in {
      val repository = mock[OrderRepository[IO]]
      val notifications = new RecordingNotificationRepository
      val generatedData = generatedDataMock
      val service = serviceWith(repository, preference = Some(disabledPreference), notifications = notifications, generatedData)

      repository.findByUser.expects(userId, orderId).returning(IO.pure(Some(order)))
      inSequence {
        expectGeneratedNow(generatedData, statusAt)
        expectGeneratedId(generatedData, statusEventId.value)
        expectGeneratedNow(generatedData, statusAt)
      }
      repository.updateStatus.expects(updatedOrder).returning(IO.pure(Some(updatedOrder)))
      repository.insertStatusEvent.expects(statusEvent).returning(IO.pure(statusEvent))

      service.updateStatus(userId, orderId, statusUpdate).unsafeRunSync() shouldBe Right(updatedOrder)
      notifications.created shouldBe empty
    }

    "return not found when updating status of an inaccessible order" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.findByUser.expects(userId, orderId).returning(IO.pure(None))

      service.updateStatus(userId, orderId, statusUpdate).unsafeRunSync() shouldBe
        Left(OrderServiceError.OrderNotFound(orderId))
    }

    "reject status update when new start datetime is after current end datetime" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.findByUser.expects(userId, orderId).returning(IO.pure(Some(order)))

      service.updateStatus(userId, orderId, invalidStatusUpdate).unsafeRunSync() shouldBe
        Left(OrderServiceError.InvalidDateTimeRange)
    }

    "admin update status for any order and notify the owner" in {
      val repository = mock[OrderRepository[IO]]
      val notifications = new RecordingNotificationRepository
      val generatedData = generatedDataMock
      val adminStatusEvent = statusEvent.copy(source = OrderStatusEventSource.AdminSimulation)
      val service = serviceWith(repository, preference = Some(enabledPreference), notifications = notifications, generatedData)

      repository.findById.expects(orderId).returning(IO.pure(Some(order)))
      inSequence {
        expectGeneratedNow(generatedData, statusAt)
        expectGeneratedId(generatedData, statusEventId.value)
        expectGeneratedNow(generatedData, statusAt)
        expectGeneratedId(generatedData, notificationId.value)
        expectGeneratedNow(generatedData, notificationAt)
      }
      repository.updateStatus.expects(updatedOrder).returning(IO.pure(Some(updatedOrder)))
      repository.insertStatusEvent.expects(adminStatusEvent).returning(IO.pure(adminStatusEvent))

      service.adminUpdateStatus(orderId, statusUpdate).unsafeRunSync() shouldBe Right(updatedOrder)
      notifications.created.size shouldBe 1
      notifications.created.head.userId.value shouldBe userId.value
      notifications.created.head.orderId.value shouldBe Some(orderId.value)
    }

    "return not found when admin updating a missing order status" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.findById.expects(orderId).returning(IO.pure(None))

      service.adminUpdateStatus(orderId, statusUpdate).unsafeRunSync() shouldBe
        Left(OrderServiceError.OrderNotFound(orderId))
    }

    "reject admin status update when new start datetime is after current end datetime" in {
      val repository = mock[OrderRepository[IO]]
      val service = serviceWith(repository, preference = None, notifications = new RecordingNotificationRepository)

      repository.findById.expects(orderId).returning(IO.pure(Some(order)))

      service.adminUpdateStatus(orderId, invalidStatusUpdate).unsafeRunSync() shouldBe
        Left(OrderServiceError.InvalidDateTimeRange)
    }

    "skip admin status update notification when preferences are disabled" in {
      val repository = mock[OrderRepository[IO]]
      val notifications = new RecordingNotificationRepository
      val generatedData = generatedDataMock
      val adminStatusEvent = statusEvent.copy(source = OrderStatusEventSource.AdminSimulation)
      val service = serviceWith(repository, preference = Some(disabledPreference), notifications = notifications, generatedData)

      repository.findById.expects(orderId).returning(IO.pure(Some(order)))
      inSequence {
        expectGeneratedNow(generatedData, statusAt)
        expectGeneratedId(generatedData, statusEventId.value)
        expectGeneratedNow(generatedData, statusAt)
      }
      repository.updateStatus.expects(updatedOrder).returning(IO.pure(Some(updatedOrder)))
      repository.insertStatusEvent.expects(adminStatusEvent).returning(IO.pure(adminStatusEvent))

      service.adminUpdateStatus(orderId, statusUpdate).unsafeRunSync() shouldBe Right(updatedOrder)
      notifications.created shouldBe empty
    }
  }

  private def serviceWith(
    repository: OrderRepository[IO],
    preference: Option[NotificationPreference],
    notifications: RecordingNotificationRepository
  ): OrderService[IO] =
    OrderService[IO](
      repository,
      new StaticNotificationPreferenceRepository(preference),
      NotificationService[IO](notifications)
    )

  private def serviceWith(
    repository: OrderRepository[IO],
    preference: Option[NotificationPreference],
    notifications: RecordingNotificationRepository,
    generatedData: GeneratedData[IO]
  ): OrderService[IO] =
    given GeneratedData[IO] = generatedData
    OrderService[IO](
      repository,
      new StaticNotificationPreferenceRepository(preference),
      NotificationService[IO](notifications)
    )

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private val userId = UserId(uuid("000000000001"))
  private val tripId = TripId(uuid("000000000010"))
  private val orderId = OrderId(uuid("000000000100"))
  private val statusEventId = OrderStatusEventId(uuid("000000000200"))
  private val notificationId = NotificationId(uuid("000000000300"))
  private val providerId = ProviderId(uuid("000000000005"))
  private val departureLocationId = LocationId(uuid("000000000020"))
  private val arrivalLocationId = LocationId(uuid("000000000021"))
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

  private val searchParams = OrderSearchParams(
    serviceType = Some(ServiceType.Flight),
    status = Some(OrderStatus.PendingVerification)
  )

  private val orderUpdate = OrderUpdate(
    provider_id = Some(providerId),
    service_type = Some(ServiceType.Hotel),
    title = Some(OrderTitle("Hotel in Bangkok")),
    price_amount = Some(280.0),
    end_datetime = Some(endDateTime.plusDays(2)),
    arrival_location_id = Some(arrivalLocationId)
  )

  private val invalidOrderUpdate = OrderUpdate(
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

  private val statusAt = Instant.parse("2026-06-01T10:05:00Z")
  private val notificationAt = Instant.parse("2026-06-01T10:06:00Z")

  private val updatedOrder = order.copy(status = OrderStatus.Confirmed, updated_at = statusAt)
  private val updatedOrderDetails = order.copy(
    service_type = ServiceType.Hotel,
    title = OrderTitle("Hotel in Bangkok"),
    price_amount = Some(280.0),
    end_datetime = Some(endDateTime.plusDays(2)),
    updated_at = statusAt
  )
  private val statusUpdate = OrderStatusUpdate(status = OrderStatus.Confirmed, reason = Some("Confirmed by provider"))
  private val invalidStatusUpdate = statusUpdate.copy(new_start_datetime = Some(endDateTime.plusHours(1)))
  private val statusEvent = OrderStatusEvent(
    id = statusEventId,
    order_id = orderId,
    old_status = Some(OrderStatus.PendingVerification),
    new_status = OrderStatus.Confirmed,
    reason = Some("Confirmed by provider"),
    payload = None,
    source = OrderStatusEventSource.UserEdit,
    created_at = statusAt
  )

  private val enabledPreference = NotificationPreference(
    id = NotificationPreferenceId(uuid("000000000001")),
    userId = userId,
    isEnabled = true,
    createdAt = Instant.parse("2026-06-01T10:00:00Z"),
    updatedAt = Instant.parse("2026-06-01T10:00:00Z")
  )

  private val disabledPreference = enabledPreference.copy(isEnabled = false)

private final class StaticNotificationPreferenceRepository(
  preference: Option[NotificationPreference]
) extends NotificationPreferenceRepository[IO]:
  override def getByUserId(userId: UserId): IO[Option[NotificationPreference]] =
    IO.pure(preference.filter(_.userId == userId))

  override def upsert(preference: NotificationPreference): IO[NotificationPreference] =
    IO.pure(preference)

private final class RecordingNotificationRepository extends NotificationRepository[IO]:
  var created: List[UserNotification] = Nil

  override def create(notification: UserNotification): IO[UserNotification] =
    created = notification :: created
    IO.pure(notification)

  override def findById(id: NotificationId): IO[Option[UserNotification]] =
    IO.pure(created.find(_.id == id))

  override def findByUserId(userId: NotificationUserId, limit: Int, offset: Int): IO[List[UserNotification]] =
    IO.pure(created.filter(_.userId == userId).slice(offset, offset + limit))

  override def markAsRead(id: NotificationId, updatedAt: Instant): IO[Int] = IO.pure(0)
  override def markAllAsRead(userId: NotificationUserId, updatedAt: Instant): IO[Int] = IO.pure(0)
  override def delete(id: NotificationId): IO[Int] = IO.pure(0)
  override def deleteAllForUser(userId: NotificationUserId): IO[Int] = IO.pure(0)
