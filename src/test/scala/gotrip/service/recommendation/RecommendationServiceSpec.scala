package gotrip.service.recommendation

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.additionalservice.*
import gotrip.domain.location.*
import gotrip.domain.order.*
import gotrip.domain.provider.*
import gotrip.domain.trip.*
import gotrip.domain.user.*
import gotrip.repository.additionalservice.AdditionalServiceRepository
import gotrip.repository.order.OrderRepository
import gotrip.repository.triplocation.TripLocationRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID

final class RecommendationServiceSpec extends AnyWordSpec with Matchers with MockFactory:

  "RecommendationService" should {
    "return TripNotFound when trip is not owned by user" in {
      val (orderRepository, tripLocationRepository, additionalServiceRepository, service) = serviceFixture()

      orderRepository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(false))

      service.forTrip(userId, tripId).unsafeRunSync() shouldBe
        Left(RecommendationServiceError.TripNotFound(tripId))
    }

    "filter inactive services and rank trip matches first" in {
      val (orderRepository, tripLocationRepository, additionalServiceRepository, service) = serviceFixture()
      val taxi = additionalService("Airport taxi", ServiceType.Taxi, locationId = Some(arrivalLocationId))
      val insurance = additionalService("Travel insurance", ServiceType.Insurance)
      val inactiveLounge = additionalService("Closed lounge", ServiceType.Lounge, locationId = Some(arrivalLocationId), active = false)
      val unrelatedTour = additionalService("Museum tour", ServiceType.Tour)

      inSequence {
        orderRepository.tripExistsForUser.expects(userId, tripId).returning(IO.pure(true))
        orderRepository.listByTrip.expects(userId, tripId, OrderSearchParams()).returning(IO.pure(List(flightOrder)))
        tripLocationRepository.listByTrip.expects(tripId).returning(IO.pure(List(tripLocation)))
        additionalServiceRepository.search.expects(AdditionalServiceSearchParams()).returning {
          IO.pure(List(insurance, inactiveLounge, unrelatedTour, taxi))
        }
      }

      val result = service.forTrip(userId, tripId).unsafeRunSync()

      result.map(_.map(_.service.title.value)) shouldBe Right(List("Airport taxi", "Travel insurance"))
      result.map(_.map(_.score)) shouldBe Right(List(Some(1.0), Some(0.45)))
      result.toOption.get.head.reason should include("matches a location in your trip")
      result.toOption.get.head.reason should include("complements your flight booking")
    }

    "return OrderNotFound when order is not owned by user" in {
      val (orderRepository, tripLocationRepository, additionalServiceRepository, service) = serviceFixture()

      orderRepository.findByUser.expects(userId, orderId).returning(IO.pure(None))

      service.forOrder(userId, orderId).unsafeRunSync() shouldBe
        Left(RecommendationServiceError.OrderNotFound(orderId))
    }

    "rank order recommendations by route, provider, and complementary matches" in {
      val (orderRepository, tripLocationRepository, additionalServiceRepository, service) = serviceFixture()
      val taxi = additionalService("Airport taxi", ServiceType.Taxi, locationId = Some(departureLocationId))
      val lounge = additionalService("Provider lounge", ServiceType.Lounge, providerId = Some(providerId))
      val esim = additionalService("Travel eSIM", ServiceType.Esim)
      val inactiveBaggage = additionalService("Inactive baggage", ServiceType.ExtraBaggage, active = false)

      inSequence {
        orderRepository.findByUser.expects(userId, orderId).returning(IO.pure(Some(flightOrder)))
        additionalServiceRepository.search.expects(AdditionalServiceSearchParams()).returning {
          IO.pure(List(esim, lounge, taxi, inactiveBaggage))
        }
      }

      val result = service.forOrder(userId, orderId).unsafeRunSync()

      result.map(_.map(_.service.title.value)) shouldBe Right(List("Airport taxi", "Provider lounge", "Travel eSIM"))
      result.map(_.map(_.score)) shouldBe Right(List(Some(0.8), Some(0.45), Some(0.25)))
      result.toOption.get.head.reason should include("matches this order route")
      result.toOption.get(1).reason should include("uses the same provider as this order")
      result.toOption.get(2).reason should include("complements your flight order")
    }
  }

  private def serviceFixture()
      : (
        OrderRepository[IO],
        TripLocationRepository[IO],
        AdditionalServiceRepository[IO],
        RecommendationService[IO]
      ) =
    val orderRepository = mock[OrderRepository[IO]]
    val tripLocationRepository = mock[TripLocationRepository[IO]]
    val additionalServiceRepository = mock[AdditionalServiceRepository[IO]]
    val service = RecommendationService[IO](orderRepository, tripLocationRepository, additionalServiceRepository)
    (orderRepository, tripLocationRepository, additionalServiceRepository, service)

  private def uuid(suffix: String): UUID =
    UUID.fromString(s"00000000-0000-0000-0000-$suffix")

  private val userId = UserId(uuid("000000000001"))
  private val tripId = TripId(uuid("000000000002"))
  private val orderId = OrderId(uuid("000000000003"))
  private val providerId = ProviderId(uuid("000000000004"))
  private val departureLocationId = LocationId(uuid("000000000005"))
  private val arrivalLocationId = LocationId(uuid("000000000006"))
  private val tripLocationId = TripLocationId(uuid("000000000007"))

  private val flightOrder = Order(
    id = orderId,
    user_id = userId,
    trip_id = tripId,
    provider_id = Some(providerId),
    service_type = ServiceType.Flight,
    external_order_id = None,
    title = OrderTitle("Flight to Paris"),
    status = OrderStatus.Confirmed,
    price_amount = Some(250.0),
    price_currency = Some("USD"),
    start_datetime = None,
    end_datetime = None,
    departure_location_id = Some(departureLocationId),
    arrival_location_id = Some(arrivalLocationId),
    created_at = Instant.EPOCH,
    updated_at = Instant.EPOCH
  )

  private val tripLocation = TripLocation(
    id = tripLocationId,
    trip_id = tripId,
    location_id = arrivalLocationId,
    visit_order = VisitOrder(1),
    arrival_date = TripLocationArrivalDate(None),
    departure_date = TripLocationDepartureDate(None)
  )

  private def additionalService(
    title: String,
    serviceType: ServiceType,
    providerId: Option[ProviderId] = None,
    locationId: Option[LocationId] = None,
    active: Boolean = true
  ): AdditionalService =
    AdditionalService(
      id = ServiceId(UUID.nameUUIDFromBytes(title.getBytes)),
      title = ServiceTitle(title),
      description = None,
      service_type = serviceType,
      provider_id = providerId,
      location_id = locationId,
      price_amount = None,
      price_currency = None,
      is_active = active
    )
