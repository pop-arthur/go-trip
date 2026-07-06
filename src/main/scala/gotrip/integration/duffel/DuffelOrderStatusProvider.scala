package gotrip.integration.duffel

import cats.effect.{Clock, Sync}
import cats.syntax.either.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.order.*
import gotrip.integration.{ExternalOrderStatusUpdate, OrderStatusProvider, OrderStatusProviderError}
import gotrip.service.GeneratedData
import io.circe.{Json, ParsingFailure}
import io.circe.parser.parse

import java.net.URI
import java.net.http.{HttpClient, HttpRequest, HttpResponse}
import java.time.Instant
import scala.util.Try

final class DuffelOrderStatusProvider[F[_]: Sync: Clock](
  config: DuffelConfig,
  httpClient: HttpClient
) extends OrderStatusProvider[F]:

  override def checkUpdates(order: Order): F[Either[OrderStatusProviderError, List[ExternalOrderStatusUpdate]]] =
    order.external_order_id match
      case None =>
        Sync[F].pure(Left(OrderStatusProviderError.ExternalOrderIdMissing))
      case Some(externalOrderId) =>
        getOrder(externalOrderId).flatMap:
          case Left(error) =>
            Sync[F].pure(Left(error))
          case Right(payload) =>
            GeneratedData.now[F].flatMap { now =>
              statusFromPayload(payload, order, now) match
                case Left(error) =>
                  Sync[F].pure(Left(error))
                case Right(None) =>
                  Sync[F].pure(Right(List.empty))
                case Right(Some(status)) if status == order.status =>
                  Sync[F].pure(Right(List.empty))
                case Right(Some(status)) =>
                  ExternalOrderStatusUpdate
                    .create[F](
                      orderId = order.id,
                      externalOrderId = externalOrderId,
                      status = status,
                      reason = Some("Duffel order status changed"),
                      payload = Some(payload)
                    )
                    .map(update => Right(List(update)))
            }

  private def getOrder(externalOrderId: String): F[Either[OrderStatusProviderError, Json]] =
    for {
      request <- Sync[F].delay(buildGetOrderRequest(externalOrderId))
      response <- Sync[F].blocking(httpClient.send(request, HttpResponse.BodyHandlers.ofString()))
    } yield handleResponse(response)

  private def buildGetOrderRequest(externalOrderId: String): HttpRequest =
    HttpRequest
      .newBuilder(orderUri(externalOrderId))
      .GET()
      .header("Accept", "application/json")
      .header("Duffel-Version", config.version)
      .header("Authorization", s"Bearer ${config.accessToken}")
      .build()

  private def orderUri(externalOrderId: String): URI =
    URI.create(s"${config.baseUrl.stripSuffix("/")}/air/orders/$externalOrderId")

  private def handleResponse(response: HttpResponse[String]): Either[OrderStatusProviderError, Json] =
    response.statusCode() match
      case status if status >= 200 && status < 300 =>
        parse(response.body()).leftMap(invalidJson)
      case 401 | 403 =>
        Left(OrderStatusProviderError.Unauthorized("Duffel API rejected the access token"))
      case 404 =>
        Left(OrderStatusProviderError.NotFound("Duffel order was not found"))
      case status if status >= 500 =>
        Left(OrderStatusProviderError.ProviderUnavailable(s"Duffel API returned $status"))
      case status =>
        Left(OrderStatusProviderError.InvalidResponse(s"Duffel API returned $status: ${response.body()}"))

  private def statusFromPayload(
    payload: Json,
    order: Order,
    now: Instant
  ): Either[OrderStatusProviderError, Option[OrderStatus]] =
    val cursor = payload.hcursor.downField("data")
    for {
      cancelledAt <- cursor.get[Option[String]]("cancelled_at").leftMap(error =>
        OrderStatusProviderError.InvalidResponse(error.message)
      )
      cancellation <- cursor.get[Option[Json]]("cancellation").leftMap(error =>
        OrderStatusProviderError.InvalidResponse(error.message)
      )
      airlineInitiatedChanges <- cursor.get[Option[List[Json]]]("airline_initiated_changes").leftMap(error =>
        OrderStatusProviderError.InvalidResponse(error.message)
      )
      awaitingPayment <- cursor.downField("payment_status").get[Option[Boolean]]("awaiting_payment").leftMap(error =>
        OrderStatusProviderError.InvalidResponse(error.message)
      )
      bookingReference <- cursor.get[Option[String]]("booking_reference").leftMap(error =>
        OrderStatusProviderError.InvalidResponse(error.message)
      )
    } yield
      cancellationStatus(cancellation)
        .orElse(cancelledStatus(cancelledAt))
        .orElse(airlineInitiatedChangeStatus(airlineInitiatedChanges.getOrElse(List.empty)))
        .orElse(paymentStatus(awaitingPayment))
        .orElse(bookingStatus(bookingReference, order, now))

  private def cancellationStatus(cancellation: Option[Json]): Option[OrderStatus] =
    cancellation.map { value =>
      val cursor = value.hcursor
      val confirmedAt = cursor.get[Option[String]]("confirmed_at").toOption.flatten
      val refundAmount = cursor.get[Option[String]]("refund_amount").toOption.flatten
      val airlineCredits = cursor.get[Option[List[Json]]]("airline_credits").toOption.flatten.getOrElse(List.empty)

      if confirmedAt.isEmpty then
        OrderStatus.RefundPending
      else if refundAmount.exists(isPositiveAmount) || airlineCredits.nonEmpty then
        OrderStatus.Refunded
      else
        OrderStatus.Cancelled
    }

  private def cancelledStatus(cancelledAt: Option[String]): Option[OrderStatus] =
    cancelledAt.map(_ => OrderStatus.Cancelled)

  private def airlineInitiatedChangeStatus(changes: List[Json]): Option[OrderStatus] =
    if changes.exists(actionTaken(_).contains("cancelled")) then
      Some(OrderStatus.Cancelled)
    else if changes.exists(actionTaken(_).isEmpty) then
      Some(OrderStatus.Delayed)
    else
      None

  private def paymentStatus(awaitingPayment: Option[Boolean]): Option[OrderStatus] =
    if awaitingPayment.contains(true) then Some(OrderStatus.PendingVerification)
    else None

  private def bookingStatus(
    bookingReference: Option[String],
    order: Order,
    now: Instant
  ): Option[OrderStatus] =
    if bookingReference.exists(_.nonEmpty) then
      if order.end_datetime.exists(_.toInstant.isBefore(now)) then Some(OrderStatus.Completed)
      else Some(OrderStatus.Confirmed)
    else
      None

  private def actionTaken(change: Json): Option[String] =
    change.hcursor.get[Option[String]]("action_taken").toOption.flatten

  private def isPositiveAmount(value: String): Boolean =
    Try(BigDecimal(value)).toOption.exists(_ > 0)

  private def invalidJson(error: ParsingFailure): OrderStatusProviderError =
    OrderStatusProviderError.InvalidResponse(s"Duffel API returned invalid JSON: ${error.message}")

object DuffelOrderStatusProvider:
  def make[F[_]: Sync: Clock](config: DuffelConfig): DuffelOrderStatusProvider[F] =
    new DuffelOrderStatusProvider[F](config, HttpClient.newHttpClient())
