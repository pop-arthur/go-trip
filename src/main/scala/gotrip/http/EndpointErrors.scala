package gotrip.http

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

object EndpointErrors:
  import HttpError.given

  private val validationVariant =
    oneOfVariant(
      statusCode(StatusCode.UnprocessableEntity)
        .and(jsonBody[HttpError.Validation].description("Request validation failed"))
    )

  private val notFoundVariant =
    oneOfVariant(
      statusCode(StatusCode.NotFound)
        .and(jsonBody[HttpError.NotFound].description("Requested resource was not found"))
    )

  private val conflictVariant =
    oneOfVariant(
      statusCode(StatusCode.Conflict)
        .and(jsonBody[HttpError.Conflict].description("Request conflicts with current resource state"))
    )

  private val internalVariant =
    oneOfVariant(
      statusCode(StatusCode.InternalServerError)
        .and(jsonBody[HttpError.Internal].description("Unexpected server error"))
    )

  val internalOnly =
    oneOf[HttpError](
      internalVariant
    )

  val notFound =
    oneOf[HttpError](
      notFoundVariant,
      internalVariant
    )

  val validation =
    oneOf[HttpError](
      validationVariant,
      internalVariant
    )

  val validationOrNotFound =
    oneOf[HttpError](
      validationVariant,
      notFoundVariant,
      internalVariant
    )

  val notFoundOrConflict =
    oneOf[HttpError](
      notFoundVariant,
      conflictVariant,
      internalVariant
    )

  val validationOrConflict =
    oneOf[HttpError](
      validationVariant,
      conflictVariant,
      internalVariant
    )

  val validationOrNotFoundOrConflict =
    oneOf[HttpError](
      validationVariant,
      notFoundVariant,
      conflictVariant,
      internalVariant
    )
