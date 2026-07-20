package gotrip.integration.duffel

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.additionalservice.ServiceType
import gotrip.domain.order.*
import gotrip.domain.trip.TripId
import gotrip.domain.user.UserId
import gotrip.integration.OrderStatusProviderError
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import sttp.client4.Backend
import sttp.client4.impl.cats.implicits.*
import sttp.client4.testing.{BackendStub, RecordingBackend}
import sttp.model.StatusCode

import java.io.IOException
import java.time.Instant
import java.util.UUID

final class DuffelOrderStatusProviderSpec extends AnyWordSpec with Matchers:

  "DuffelOrderStatusProvider" should {
    "send the expected request and return no update when the status is unchanged" in {
      val stub = BackendStub[IO](summon)
        .whenAnyRequest
        .thenRespondAdjust(confirmedPayload)
      val recording = RecordingBackend(stub)

      provider(recording).checkUpdates(order(status = OrderStatus.Confirmed)).unsafeRunSync() shouldBe Right(List.empty)

      val requests = recording.allInteractions.map(_._1)
      requests should have size 1
      val request = requests.head
      request.uri.toString shouldBe s"${config.baseUrl.stripSuffix("/")}/air/orders/$externalOrderId"
      request.headers.find(_.name.equalsIgnoreCase("Accept")).map(_.value) shouldBe Some("application/json")
      request.headers.find(_.name.equalsIgnoreCase("Duffel-Version")).map(_.value) shouldBe Some(config.version)
      request.headers.find(_.name.equalsIgnoreCase("Authorization")).map(_.value) shouldBe
        Some(s"Bearer ${config.accessToken}")
    }

    "return an update when Duffel reports a different status" in {
      val stub = BackendStub[IO](summon).whenAnyRequest.thenRespondAdjust(confirmedPayload)

      val result = provider(stub).checkUpdates(order(status = OrderStatus.PendingVerification)).unsafeRunSync()

      result match
        case Right(update :: Nil) =>
          update.orderId shouldBe orderId
          update.externalOrderId shouldBe externalOrderId
          update.status shouldBe OrderStatus.Confirmed
          update.reason shouldBe Some("Duffel order status changed")
          update.payload.map(_.noSpaces) shouldBe Some(confirmedPayload)
        case other => fail(s"Expected one status update, got $other")
    }

    "map authentication, not-found, server, and other HTTP errors" in {
      val cases = List(
        StatusCode.Unauthorized -> OrderStatusProviderError.Unauthorized("Duffel API rejected the access token"),
        StatusCode.Forbidden -> OrderStatusProviderError.Unauthorized("Duffel API rejected the access token"),
        StatusCode.NotFound -> OrderStatusProviderError.NotFound("Duffel order was not found"),
        StatusCode.BadGateway -> OrderStatusProviderError.ProviderUnavailable("Duffel API returned 502"),
        StatusCode.BadRequest -> OrderStatusProviderError.InvalidResponse("Duffel API returned 400: invalid request")
      )

      cases.foreach { case (status, expected) =>
        val stub = BackendStub[IO](summon).whenAnyRequest.thenRespondAdjust("invalid request", status)
        provider(stub).checkUpdates(order()).unsafeRunSync() shouldBe Left(expected)
      }
    }

    "return InvalidResponse for malformed JSON" in {
      val stub = BackendStub[IO](summon).whenAnyRequest.thenRespondAdjust("not-json")

      provider(stub).checkUpdates(order()).unsafeRunSync() match
        case Left(OrderStatusProviderError.InvalidResponse(message)) =>
          message should include("Duffel API returned invalid JSON")
        case other => fail(s"Expected InvalidResponse, got $other")
    }

    "map transport failures to ProviderUnavailable" in {
      val stub = BackendStub[IO](summon).whenAnyRequest.thenThrow(new IOException("connection reset"))

      provider(stub).checkUpdates(order()).unsafeRunSync() match
        case Left(OrderStatusProviderError.ProviderUnavailable(message)) =>
          message should include("Duffel API request failed")
          message should include("connection reset")
        case other => fail(s"Expected ProviderUnavailable, got $other")
    }

    "not send a request when the external order id is missing" in {
      val recording = RecordingBackend(BackendStub[IO](summon))

      provider(recording).checkUpdates(order(externalId = None)).unsafeRunSync() shouldBe
        Left(OrderStatusProviderError.ExternalOrderIdMissing)
      recording.allInteractions shouldBe empty
    }
  }

  private val config = DuffelConfig(
    baseUrl = "https://api.duffel.test/",
    accessToken = "test-token",
    version = "v2"
  )
  private val externalOrderId = "ord_123"
  private val orderId = OrderId(UUID.fromString("00000000-0000-0000-0000-000000000001"))

  private val confirmedPayload =
    """{"data":{"cancelled_at":null,"cancellation":null,"airline_initiated_changes":[],"payment_status":{"awaiting_payment":false},"booking_reference":"ABC123"}}"""

  private def provider(backend: Backend[IO]): DuffelOrderStatusProvider[IO] =
    DuffelOrderStatusProvider.make[IO](config, backend)

  private def order(
    status: OrderStatus = OrderStatus.Confirmed,
    externalId: Option[String] = Some(externalOrderId)
  ): Order =
    Order(
      id = orderId,
      user_id = UserId(UUID.fromString("00000000-0000-0000-0000-000000000002")),
      trip_id = TripId(UUID.fromString("00000000-0000-0000-0000-000000000003")),
      provider_id = None,
      service_type = ServiceType.Flight,
      external_order_id = externalId,
      title = OrderTitle("Test flight"),
      status = status,
      price_amount = None,
      price_currency = None,
      start_datetime = None,
      end_datetime = None,
      departure_location_id = None,
      arrival_location_id = None,
      created_at = Instant.EPOCH,
      updated_at = Instant.EPOCH
    )
