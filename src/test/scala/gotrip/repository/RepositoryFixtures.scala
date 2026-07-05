package gotrip.repository

import gotrip.domain.achievement.*
import gotrip.domain.additionalservice.*
import gotrip.domain.auth.AuthSession
import gotrip.domain.location.*
import gotrip.domain.notification.*
import gotrip.domain.notificationpreference.*
import gotrip.domain.order.*
import gotrip.domain.provider.*
import gotrip.domain.review.*
import gotrip.domain.trip.*
import gotrip.domain.user.*
import gotrip.domain.userachievement.*
import io.circe.Json

import java.time.{Instant, LocalDate, OffsetDateTime, ZoneOffset}
import java.util.UUID

trait RepositoryFixtures:

  protected def id(value: Int): UUID =
    UUID.fromString(f"00000000-0000-0000-0000-$value%012d")

  protected def t(value: Int): Instant =
    Instant.parse("2026-07-03T00:00:00Z").plusSeconds(value.toLong)

  protected def odt(value: Int): OffsetDateTime =
    t(value).atOffset(ZoneOffset.UTC)

  protected def sampleUser(value: Int, email: String = "user@example.test"): User =
    User(UserId(id(value)), UserEmail(email), UserPasswordHash("hash"), UserFullName(Some("Test User")), t(1), t(1))

  protected def sampleSession(value: Int, userId: UserId, expiresAt: Instant, revokedAt: Option[Instant]): AuthSession =
    AuthSession(id(value), userId, s"hash-$value", expiresAt, revokedAt, t(value), t(value))

  protected def sampleLocation(
    value: Int,
    name: String,
    locationType: LocationType,
    country: Option[String] = Some("Vietnam"),
    city: Option[String] = Some("Hanoi")
  ): Location =
    Location(
      LocationId(id(value)),
      LocationName(name),
      locationType,
      LocationCountry(country),
      LocationCity(city),
      LocationAddress(Some("Address")),
      LocationLatitude(Some(21.0)),
      LocationLongitude(Some(105.0))
    )

  protected def sampleProvider(value: Int, name: String, providerType: ProviderType): Provider =
    Provider(ProviderId(id(value)), ProviderName(name), providerType, Some("https://example.test"), Some("support@example.test"))

  protected def sampleService(value: Int, providerId: ProviderId, locationId: LocationId): AdditionalService =
    AdditionalService(
      ServiceId(id(value)),
      ServiceTitle("Airport Lounge"),
      Some("Quiet lounge"),
      ServiceType.Lounge,
      Some(providerId),
      Some(locationId),
      Some(25.0),
      Some("USD"),
      is_active = true
    )

  protected def sampleAchievement(value: Int, code: String): Achievement =
    Achievement(
      AchievementId(id(value)),
      AchievementCode(code),
      AchievementTitle("First Trip"),
      AchievementDescription(Some("Complete one trip")),
      AchievementConditionType.TripsCount,
      1,
      AchievementIconUrl(Some("https://example.test/icon.png")),
      t(value),
      t(value)
    )

  protected def sampleUserAchievement(value: Int, userId: UserId, achievementId: AchievementId): UserAchievement =
    UserAchievement(UserAchievementId(id(value)), userId, achievementId, t(value), t(value), t(value))

  protected def sampleTrip(value: Int, userId: UserId, title: String = "Summer Trip"): Trip =
    Trip(
      TripId(id(value)),
      userId,
      TripTitle(title),
      TripStartDate(Some(LocalDate.of(2026, 7, 10))),
      TripEndDate(Some(LocalDate.of(2026, 7, 20))),
      TripStatus.Planned,
      t(value),
      t(value)
    )

  protected def sampleTripLocation(value: Int, tripId: TripId, locationId: LocationId, visitOrder: Int): TripLocation =
    TripLocation(
      TripLocationId(id(value)),
      tripId,
      locationId,
      VisitOrder(visitOrder),
      TripLocationArrivalDate(Some(odt(value))),
      TripLocationDepartureDate(Some(odt(value + 1)))
    )

  protected def sampleOrder(
    value: Int,
    userId: UserId,
    tripId: TripId,
    providerId: ProviderId,
    departureId: LocationId,
    arrivalId: LocationId
  ): Order =
    Order(
      OrderId(id(value)),
      userId,
      tripId,
      Some(providerId),
      ServiceType.Train,
      Some("EXT-1"),
      OrderTitle("Train Ticket"),
      OrderStatus.PendingVerification,
      Some(120.5),
      Some("EUR"),
      Some(odt(value)),
      Some(odt(value + 1)),
      Some(departureId),
      Some(arrivalId),
      t(value),
      t(value)
    )

  protected def sampleOrderStatusEvent(value: Int, orderId: OrderId): OrderStatusEvent =
    OrderStatusEvent(
      OrderStatusEventId(id(value)),
      orderId,
      Some(OrderStatus.PendingVerification),
      OrderStatus.Confirmed,
      Some("Confirmed by provider"),
      Some(Json.obj("source" -> Json.fromString("test"))),
      OrderStatusEventSource.System,
      t(value)
    )

  protected def sampleOrderFile(value: Int, orderId: OrderId): OrderFile =
    OrderFile(
      OrderFileId(id(value)),
      orderId,
      OrderFileUrl("https://example.test/ticket.pdf"),
      FileType.Pdf,
      Some(Json.obj("pnr" -> Json.fromString("ABC123"))),
      t(value)
    )

  protected def samplePreference(value: Int, userId: UserId, isEnabled: Boolean): NotificationPreference =
    NotificationPreference(NotificationPreferenceId(id(value)), userId, isEnabled, t(value), t(value))

  protected def sampleNotification(value: Int, userId: UserId, sentAt: Instant): UserNotification =
    UserNotification(
      NotificationId(id(value)),
      NotificationUserId(userId.value),
      NotificationOrderId(None),
      NotificationType.General,
      NotificationTitle(s"Title $value"),
      NotificationBody("Body"),
      isRead = false,
      sentAt,
      t(value),
      t(value)
    )

  protected def sampleReview(value: Int, userId: UserId, targetId: LocationId, rating: ReviewRating, createdAt: Instant): Review =
    Review(ReviewId(id(value)), userId, ReviewTargetType.Location, ReviewTargetId(targetId.value), rating, ReviewText(Some("Nice")), createdAt, createdAt)
