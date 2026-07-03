package gotrip.service.recommendation

import cats.data.EitherT
import cats.effect.Sync
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.additionalservice.*
import gotrip.domain.location.LocationId
import gotrip.domain.order.*
import gotrip.domain.recommendation.Recommendation
import gotrip.domain.trip.{TripId, TripLocation}
import gotrip.domain.user.UserId
import gotrip.repository.additionalservice.AdditionalServiceRepository
import gotrip.repository.order.OrderRepository
import gotrip.repository.triplocation.TripLocationRepository

final class RecommendationService[F[_]: Sync](
  orderRepository: OrderRepository[F],
  tripLocationRepository: TripLocationRepository[F],
  additionalServiceRepository: AdditionalServiceRepository[F]
):

  import RecommendationServiceError.*

  def forTrip(userId: UserId, tripId: TripId): F[Either[RecommendationServiceError, List[Recommendation]]] =
    (for {
      _ <- ensureTripExists(userId, tripId)
      orders <- EitherT.liftF(orderRepository.listByTrip(userId, tripId, OrderSearchParams()))
      tripLocations <- EitherT.liftF(tripLocationRepository.listByTrip(tripId))
      services <- EitherT.liftF(activeServices)
    } yield rankTripServices(services, orders, tripLocations)).value

  def forOrder(userId: UserId, orderId: OrderId): F[Either[RecommendationServiceError, List[Recommendation]]] =
    (for {
      order <- EitherT.fromOptionF(orderRepository.findByUser(userId, orderId), OrderNotFound(orderId))
      services <- EitherT.liftF(activeServices)
    } yield rankOrderServices(services, order)).value

  private def ensureTripExists(userId: UserId, tripId: TripId): EitherT[F, RecommendationServiceError, Unit] =
    EitherT {
      orderRepository.tripExistsForUser(userId, tripId).map { exists =>
        Either.cond(exists, (), TripNotFound(tripId))
      }
    }

  private def activeServices: F[List[AdditionalService]] =
    additionalServiceRepository.search(AdditionalServiceSearchParams()).map(_.filter(_.is_active))

  private def rankTripServices(
    services: List[AdditionalService],
    orders: List[Order],
    tripLocations: List[TripLocation]
  ): List[Recommendation] =
    val tripLocationIds = tripLocations.map(_.location_id).toSet
    val orderLocationIds = orders.flatMap(order => List(order.departure_location_id, order.arrival_location_id).flatten).toSet
    val locationIds = tripLocationIds ++ orderLocationIds
    val providerIds = orders.flatMap(_.provider_id).toSet
    val orderedTypes = orders.map(_.service_type).toSet
    val complementaryTypes = orderedTypes.flatMap(complementsFor)

    services.flatMap { service =>
      val matches = List(
        service.location_id.exists(locationIds.contains) -> "matches a location in your trip",
        service.provider_id.exists(providerIds.contains) -> "uses a provider already in your trip",
        complementaryTypes.contains(service.service_type) -> s"complements your ${orderedTypes.map(encodeServiceType).toList.sorted.mkString(", ")} booking",
        (!orderedTypes.contains(service.service_type) && commonTravelAddOns.contains(service.service_type)) ->
          "fills a common travel need not yet covered by your orders"
      )
      recommendation(service, matches)
    }.sortBy(recommendation => (-recommendation.score.getOrElse(0.0), recommendation.service.title.value))

  private def rankOrderServices(services: List[AdditionalService], order: Order): List[Recommendation] =
    val routeLocationIds = Set(order.departure_location_id, order.arrival_location_id).flatten
    val complementaryTypes = complementsFor(order.service_type)

    services.flatMap { service =>
      val matches = List(
        service.location_id.exists(routeLocationIds.contains) -> "matches this order route",
        service.provider_id.exists(order.provider_id.contains) -> "uses the same provider as this order",
        complementaryTypes.contains(service.service_type) -> s"complements your ${encodeServiceType(order.service_type)} order"
      )
      recommendation(service, matches)
    }.sortBy(recommendation => (-recommendation.score.getOrElse(0.0), recommendation.service.title.value))

  private def recommendation(
    service: AdditionalService,
    matches: List[(Boolean, String)]
  ): Option[Recommendation] =
    val reasons = matches.collect { case (true, reason) => reason }
    if reasons.isEmpty then None
    else
      Some(
        Recommendation(
          service = service,
          reason = s"Recommended because it ${reasons.mkString("; ")}.",
          score = Some(score(matches))
        )
      )

  private def score(matches: List[(Boolean, String)]): Double =
    val weights = List(0.55, 0.20, 0.25, 0.20)
    matches.zip(weights).collect { case ((true, _), weight) => weight }.sum.min(1.0)

  private def complementsFor(serviceType: ServiceType): Set[ServiceType] =
    serviceType match
      case ServiceType.Flight =>
        Set(ServiceType.Taxi, ServiceType.Insurance, ServiceType.Lounge, ServiceType.ExtraBaggage, ServiceType.Esim, ServiceType.Hotel)
      case ServiceType.Train | ServiceType.Bus =>
        Set(ServiceType.Taxi, ServiceType.Insurance, ServiceType.Hotel, ServiceType.Esim)
      case ServiceType.Hotel =>
        Set(ServiceType.Tour, ServiceType.Taxi, ServiceType.Insurance, ServiceType.Esim)
      case ServiceType.Tour =>
        Set(ServiceType.Taxi, ServiceType.Insurance, ServiceType.Esim)
      case ServiceType.CarRental =>
        Set(ServiceType.Insurance, ServiceType.Esim)
      case ServiceType.Taxi =>
        Set(ServiceType.Esim, ServiceType.Insurance)
      case ServiceType.Esim =>
        Set(ServiceType.Insurance)
      case ServiceType.Lounge =>
        Set(ServiceType.Taxi, ServiceType.Esim)
      case ServiceType.ExtraBaggage =>
        Set(ServiceType.Insurance)
      case ServiceType.Insurance | ServiceType.Other =>
        Set(ServiceType.Esim, ServiceType.Insurance)

  private val commonTravelAddOns: Set[ServiceType] =
    Set(ServiceType.Insurance, ServiceType.Esim, ServiceType.Taxi)

  private def encodeServiceType(serviceType: ServiceType): String =
    serviceType match
      case ServiceType.Flight       => "flight"
      case ServiceType.Train        => "train"
      case ServiceType.Bus          => "bus"
      case ServiceType.Hotel        => "hotel"
      case ServiceType.Tour         => "tour"
      case ServiceType.CarRental    => "car rental"
      case ServiceType.Insurance    => "insurance"
      case ServiceType.Taxi         => "taxi"
      case ServiceType.Esim         => "eSIM"
      case ServiceType.Lounge       => "lounge"
      case ServiceType.ExtraBaggage => "extra baggage"
      case ServiceType.Other        => "other"

enum RecommendationServiceError:
  case TripNotFound(id: TripId)
  case OrderNotFound(id: OrderId)
