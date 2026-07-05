package gotrip.http.order

import gotrip.domain.additionalservice.ServiceType
import gotrip.domain.order.*
import gotrip.domain.trip.TripId
import gotrip.http.{EndpointErrors, HttpError, SwaggerTags}
import gotrip.http.auth.AuthEndpoints
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

import java.time.LocalDate

object OrderEndpoints:
  import OrderCodecs.given

  type ErrorResponse = HttpError

  val listTripOrders
      : Endpoint[String, (TripId, Option[ServiceType], Option[OrderStatus], Option[LocalDate], Option[LocalDate]), ErrorResponse, List[Order], Any] =
    endpoint.get
      .tag(SwaggerTags.Orders)
      .securityIn(AuthEndpoints.bearer)
      .in("trips" / path[TripId]("tripId") / "orders")
      .in(query[Option[ServiceType]]("serviceType"))
      .in(query[Option[OrderStatus]]("status"))
      .in(query[Option[LocalDate]]("fromDate"))
      .in(query[Option[LocalDate]]("toDate"))
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[List[Order]])

  val createOrder: Endpoint[String, (TripId, OrderCreate), ErrorResponse, Order, Any] =
    endpoint.post
      .tag(SwaggerTags.Orders)
      .securityIn(AuthEndpoints.bearer)
      .in("trips" / path[TripId]("tripId") / "orders")
      .in(jsonBody[OrderCreate])
      .errorOut(EndpointErrors.validationOrNotFoundOrConflict)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[Order])

  val getOrder: Endpoint[String, OrderId, ErrorResponse, Order, Any] =
    endpoint.get
      .tag(SwaggerTags.Orders)
      .securityIn(AuthEndpoints.bearer)
      .in("orders" / path[OrderId]("orderId"))
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[Order])

  val updateOrder: Endpoint[String, (OrderId, OrderUpdate), ErrorResponse, Order, Any] =
    endpoint.patch
      .tag(SwaggerTags.Orders)
      .securityIn(AuthEndpoints.bearer)
      .in("orders" / path[OrderId]("orderId"))
      .in(jsonBody[OrderUpdate])
      .errorOut(EndpointErrors.validationOrNotFoundOrConflict)
      .out(jsonBody[Order])

  val deleteOrder: Endpoint[String, OrderId, ErrorResponse, Unit, Any] =
    endpoint.delete
      .tag(SwaggerTags.Orders)
      .securityIn(AuthEndpoints.bearer)
      .in("orders" / path[OrderId]("orderId"))
      .errorOut(EndpointErrors.notFound)
      .out(statusCode(StatusCode.NoContent))

  val updateOrderStatus: Endpoint[String, (OrderId, OrderStatusUpdate), ErrorResponse, Order, Any] =
    endpoint.patch
      .tag(SwaggerTags.Orders)
      .securityIn(AuthEndpoints.bearer)
      .in("orders" / path[OrderId]("orderId") / "status")
      .in(jsonBody[OrderStatusUpdate])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(jsonBody[Order])

  val adminSimulateStatusChange: Endpoint[String, (OrderId, OrderStatusUpdate), ErrorResponse, Order, Any] =
    endpoint.post
      .tag(SwaggerTags.AdminOrders)
      .securityIn(AuthEndpoints.bearer)
      .in("admin" / "orders" / path[OrderId]("orderId") / "simulate-status-change")
      .in(jsonBody[OrderStatusUpdate])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(jsonBody[Order])
