package gotrip.repository

import cats.effect.IO
import gotrip.domain.location.*
import gotrip.domain.order.Order
import gotrip.domain.provider.*
import gotrip.domain.trip.Trip
import gotrip.domain.user.User
import gotrip.repository.location.LocationRepository
import gotrip.repository.order.OrderRepository
import gotrip.repository.orderfile.OrderFileRepository
import gotrip.repository.provider.ProviderRepository
import gotrip.repository.trip.TripRepository
import gotrip.repository.user.UserRepository

final class OrderFileRepositorySpec extends PostgresRepositorySpecBase with RepositoryFixtures:

  repositoryTest("OrderFileRepository creates files only for user-owned orders") {
    val users = UserRepository.makePostgres[IO](sessionPool)
    val orderFiles = OrderFileRepository.makePostgres[IO](sessionPool)

    for
      data <- createOrderFileData(90)
      otherUser <- users.create(sampleUser(91, "other@example.test"))
      file = sampleOrderFile(97, data.order.id)
      created <- orderFiles.create(data.user.id, file)
      createdForOtherUser <- orderFiles.create(otherUser.id, sampleOrderFile(98, data.order.id))
    yield
      assertEquals(created, Some(file))
      assertEquals(createdForOtherUser, None)
  }

  repositoryTest("OrderFileRepository lists and finds order files") {
    val orderFiles = OrderFileRepository.makePostgres[IO](sessionPool)

    for
      data <- createOrderFileData(90)
      file = sampleOrderFile(97, data.order.id)
      _ <- orderFiles.create(data.user.id, file)
      listed <- orderFiles.listByOrder(data.user.id, data.order.id)
      found <- orderFiles.findByOrder(data.user.id, data.order.id, file.id)
    yield
      assertEquals(listed, List(file))
      assertEquals(found, Some(file))
  }

  repositoryTest("OrderFileRepository checks order ownership") {
    val orderFiles = OrderFileRepository.makePostgres[IO](sessionPool)

    for
      data <- createOrderFileData(90)
      exists <- orderFiles.orderExistsForUser(data.user.id, data.order.id)
    yield assertEquals(exists, true)
  }

  repositoryTest("OrderFileRepository deletes order files") {
    val orderFiles = OrderFileRepository.makePostgres[IO](sessionPool)

    for
      data <- createOrderFileData(90)
      file = sampleOrderFile(97, data.order.id)
      _ <- orderFiles.create(data.user.id, file)
      deleted <- orderFiles.delete(data.user.id, data.order.id, file.id)
      found <- orderFiles.findByOrder(data.user.id, data.order.id, file.id)
    yield
      assertEquals(deleted, true)
      assertEquals(found, None)
  }

  private def createOrderFileData(start: Int): IO[OrderFileData] =
    val users = UserRepository.makePostgres[IO](sessionPool)
    val trips = TripRepository.makePostgres[IO](sessionPool)
    val locations = LocationRepository.makePostgres[IO](sessionPool)
    val providers = ProviderRepository.makePostgres[IO](sessionPool)
    val orders = OrderRepository.makePostgres[IO](sessionPool)

    for
      user <- users.create(sampleUser(start))
      trip <- trips.create(sampleTrip(start + 2, user.id).user_id, tripCreate(sampleTrip(start + 2, user.id)))
      provider <- providers.create(sampleProvider(start + 3, "Rail Europe", ProviderType.TransportCompany))
      departure <- locations.create(sampleLocation(start + 4, "Paris Gare de Lyon", LocationType.TrainStation, country = Some("France"), city = Some("Paris")))
      arrival <- locations.create(sampleLocation(start + 5, "Milano Centrale", LocationType.TrainStation, country = Some("Italy"), city = Some("Milan")))
      order <- orders.create(sampleOrder(start + 6, user.id, trip.id, provider.id, departure.id, arrival.id).user_id, sampleOrder(start + 6, user.id, trip.id, provider.id, departure.id, arrival.id).trip_id, orderCreate(sampleOrder(start + 6, user.id, trip.id, provider.id, departure.id, arrival.id)))
    yield OrderFileData(user, trip, order)

  private final case class OrderFileData(user: User, trip: Trip, order: Order)
