package gotrip.integration.duffel

import cats.effect.{Async, Clock}
import cats.syntax.either.*
import cats.syntax.applicativeError.*
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.order.*
import gotrip.integration.{ExternalOrderStatusUpdate, OrderStatusProvider, OrderStatusProviderError}
import gotrip.service.GeneratedData
import io.circe.{Json, ParsingFailure}
import io.circe.parser.parse
import sttp.client4.*
import sttp.model.StatusCode

import java.time.Instant
import scala.util.Try

final class DuffelOrderStatusProvider[F[_]: Async: Clock](
  config: DuffelConfig,
  backend: Backend[F]
) extends OrderStatusProvider[F]:

  override def checkUpdates(order: Order): F[Either[OrderStatusProviderError, List[ExternalOrderStatusUpdate]]] =
    order.external_order_id match
      case None =>
        Async[F].pure(Left(OrderStatusProviderError.ExternalOrderIdMissing))
      case Some(externalOrderId) =>
        getOrder(externalOrderId).flatMap:
          case Left(error) =>
            Async[F].pure(Left(error))
          case Right(payload) =>
            GeneratedData.now[F].flatMap { now =>
              statusFromPayload(payload, order, now) match
                case Left(error) =>
                  Async[F].pure(Left(error))
                case Right(None) =>
                  Async[F].pure(Right(List.empty))
                case Right(Some(status)) if status == order.status =>
                  Async[F].pure(Right(List.empty))
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
    buildGetOrderRequest(externalOrderId)
      .send(backend)
      .map(handleResponse)
      .handleError(error => Left(transportError(error)))

  private def buildGetOrderRequest(externalOrderId: String): Request[String] =
    basicRequest
      .get(uri"${config.baseUrl.stripSuffix("/")}/air/orders/$externalOrderId")
      .header("Accept", "application/json")
      .header("Duffel-Version", config.version)
      .auth.bearer(config.accessToken)
      .response(asStringAlways)

  private def handleResponse(response: Response[String]): Either[OrderStatusProviderError, Json] =
    response.code match
      case status if status.isSuccess =>
        parse(response.body).leftMap(invalidJson)
      case StatusCode.Unauthorized | StatusCode.Forbidden =>
        Left(OrderStatusProviderError.Unauthorized("Duffel API rejected the access token"))
      case StatusCode.NotFound =>
        Left(OrderStatusProviderError.NotFound("Duffel order was not found"))
      case status if status.isServerError =>
        Left(OrderStatusProviderError.ProviderUnavailable(s"Duffel API returned ${status.code}"))
      case status =>
        Left(OrderStatusProviderError.InvalidResponse(s"Duffel API returned ${status.code}: ${response.body}"))

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

  private def transportError(error: Throwable): OrderStatusProviderError =
    val cause = rootCause(error)
    OrderStatusProviderError.ProviderUnavailable(
      s"Duffel API request failed: ${Option(cause.getMessage).getOrElse(cause.getClass.getSimpleName)}"
    )

  private def rootCause(error: Throwable): Throwable =
    Option(error.getCause).fold(error)(rootCause)

object DuffelOrderStatusProvider:
  def make[F[_]: Async: Clock](config: DuffelConfig, backend: Backend[F]): DuffelOrderStatusProvider[F] =
    new DuffelOrderStatusProvider[F](config, backend)
