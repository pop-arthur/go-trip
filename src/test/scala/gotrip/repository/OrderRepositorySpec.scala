package gotrip.repository

import cats.effect.IO
import gotrip.domain.additionalservice.ServiceType
import gotrip.domain.location.*
import gotrip.domain.order.*
import gotrip.domain.provider.*
import gotrip.domain.trip.Trip
import gotrip.domain.user.User
import gotrip.repository.location.LocationRepository
import gotrip.repository.order.OrderRepository
import gotrip.repository.provider.ProviderRepository
import gotrip.repository.trip.TripRepository
import gotrip.repository.user.UserRepository

final class OrderRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("OrderRepository creates and finds orders by id and owner") {
    val orders = OrderRepository.makePostgres[IO](sessionPool)

    for
      data <- createOrderData(80)
      otherUser <- UserRepository.makePostgres[IO](sessionPool).create(sampleUser(81, "other@example.test"))
      order <- orders.create(sampleOrder(86, data.user.id, data.trip.id, data.provider.id, data.departure.id, data.arrival.id))
      byId <- orders.findById(order.id)
      byUser <- orders.findByUser(data.user.id, order.id)
      byOtherUser <- orders.findByUser(otherUser.id, order.id)
    yield
      assertEquals(byId, Some(order))
      assertEquals(byUser, Some(order))
      assertEquals(byOtherUser, None)
  }

  repositoryTest("OrderRepository lists trip orders with filters") {
    val orders = OrderRepository.makePostgres[IO](sessionPool)

    for
      data <- createOrderData(80)
      order <- orders.create(sampleOrder(86, data.user.id, data.trip.id, data.provider.id, data.departure.id, data.arrival.id))
      listed <- orders.listByTrip(data.user.id, data.trip.id, OrderSearchParams(serviceType = Some(ServiceType.Train), status = Some(OrderStatus.PendingVerification)))
    yield assertEquals(listed, List(order))
  }

  repositoryTest("OrderRepository exposes trip, provider, and location helpers") {
    val orders = OrderRepository.makePostgres[IO](sessionPool)

    for
      data <- createOrderData(80)
      tripExists <- orders.tripExistsForUser(data.user.id, data.trip.id)
      providerExists <- orders.providerExists(data.provider.id)
      locationExists <- orders.locationExists(data.departure.id)
    yield
      assertEquals(tripExists, true)
      assertEquals(providerExists, true)
      assertEquals(locationExists, true)
  }

  repositoryTest("OrderRepository updates order fields") {
    val orders = OrderRepository.makePostgres[IO](sessionPool)

    for
      data <- createOrderData(80)
      order <- orders.create(sampleOrder(86, data.user.id, data.trip.id, data.provider.id, data.departure.id, data.arrival.id))
      updatedOrder = order.copy(title = OrderTitle("Updated Train"), status = OrderStatus.Confirmed, updated_at = t(87))
      updated <- orders.update(updatedOrder)
    yield assertEquals(updated.map(_.title), Some(OrderTitle("Updated Train")))
  }

  repositoryTest("OrderRepository updates order status") {
    val orders = OrderRepository.makePostgres[IO](sessionPool)

    for
      data <- createOrderData(80)
      order <- orders.create(sampleOrder(86, data.user.id, data.trip.id, data.provider.id, data.departure.id, data.arrival.id))
      statusUpdated <- orders.updateStatus(order.copy(status = OrderStatus.Delayed, start_datetime = Some(odt(88)), updated_at = t(88)))
    yield assertEquals(statusUpdated.map(_.status), Some(OrderStatus.Delayed))
  }

  repositoryTest("OrderRepository inserts order status events") {
    val orders = OrderRepository.makePostgres[IO](sessionPool)

    for
      data <- createOrderData(80)
      order <- orders.create(sampleOrder(86, data.user.id, data.trip.id, data.provider.id, data.departure.id, data.arrival.id))
      event = sampleOrderStatusEvent(89, order.id)
      insertedEvent <- orders.insertStatusEvent(event)
    yield assertEquals(insertedEvent, event)
  }

  repositoryTest("OrderRepository deletes orders by owner") {
    val orders = OrderRepository.makePostgres[IO](sessionPool)

    for
      data <- createOrderData(80)
      order <- orders.create(sampleOrder(86, data.user.id, data.trip.id, data.provider.id, data.departure.id, data.arrival.id))
      deleted <- orders.delete(data.user.id, order.id)
      byUser <- orders.findByUser(data.user.id, order.id)
    yield
      assertEquals(deleted, true)
      assertEquals(byUser, None)
  }

  private def createOrderData(start: Int): IO[OrderData] =
    val users = UserRepository.makePostgres[IO](sessionPool)
    val trips = TripRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val providers = ProviderRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(start))
      trip <- trips.create(sampleTrip(start + 2, user.id))
      provider <- providers.create(sampleProvider(start + 3, "Rail Europe", ProviderType.TransportCompany))
      departure <- locations.create(sampleLocation(start + 4, "Paris Gare de Lyon", LocationType.TrainStation, country = Some("France"), city = Some("Paris")))
      arrival <- locations.create(sampleLocation(start + 5, "Milano Centrale", LocationType.TrainStation, country = Some("Italy"), city = Some("Milan")))
    yield OrderData(user, trip, provider, departure, arrival)

  private final case class OrderData(
    user: User,
    trip: Trip,
    provider: Provider,
    departure: Location,
    arrival: Location
  )
