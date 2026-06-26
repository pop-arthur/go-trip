package gotrip.http.orderfile

import gotrip.domain.order.*
import gotrip.http.{EndpointErrors, HttpError}
import gotrip.http.auth.AuthEndpoints
import gotrip.http.order.OrderCodecs
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

object OrderFileEndpoints:
  import OrderCodecs.given
  import OrderFileCodecs.given

  type ErrorResponse = HttpError

  val listOrderFiles: Endpoint[String, OrderId, ErrorResponse, List[OrderFile], Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("orders" / path[OrderId]("orderId") / "files")
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[List[OrderFile]])

  val createOrderFile: Endpoint[String, (OrderId, OrderFileCreate), ErrorResponse, OrderFile, Any] =
    endpoint.post
      .securityIn(AuthEndpoints.bearer)
      .in("orders" / path[OrderId]("orderId") / "files")
      .in(jsonBody[OrderFileCreate])
      .errorOut(EndpointErrors.validationOrNotFound)
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[OrderFile])

  val getOrderFile: Endpoint[String, (OrderId, OrderFileId), ErrorResponse, OrderFile, Any] =
    endpoint.get
      .securityIn(AuthEndpoints.bearer)
      .in("orders" / path[OrderId]("orderId") / "files" / path[OrderFileId]("fileId"))
      .errorOut(EndpointErrors.notFound)
      .out(jsonBody[OrderFile])

  val deleteOrderFile: Endpoint[String, (OrderId, OrderFileId), ErrorResponse, Unit, Any] =
    endpoint.delete
      .securityIn(AuthEndpoints.bearer)
      .in("orders" / path[OrderId]("orderId") / "files" / path[OrderFileId]("fileId"))
      .errorOut(EndpointErrors.notFound)
      .out(statusCode(StatusCode.NoContent))
