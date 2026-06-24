package gotrip.http

import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.json.circe.*

object EndpointErrors:
  import HttpError.given

  private val unauthorizedVariant =
    oneOfVariant(
      statusCode(StatusCode.Unauthorized)
        .and(jsonBody[HttpError.Unauthorized].description("Authentication required"))
    )

  private val forbiddenVariant =
    oneOfVariant(
      statusCode(StatusCode.Forbidden)
        .and(jsonBody[HttpError.Forbidden].description("Permission denied"))
    )

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
      unauthorizedVariant,
      forbiddenVariant,
      internalVariant
    )

  val notFound =
    oneOf[HttpError](
      unauthorizedVariant,
      forbiddenVariant,
      notFoundVariant,
      internalVariant
    )

  val validation =
    oneOf[HttpError](
      unauthorizedVariant,
      forbiddenVariant,
      validationVariant,
      internalVariant
    )

  val validationOrNotFound =
    oneOf[HttpError](
      unauthorizedVariant,
      forbiddenVariant,
      validationVariant,
      notFoundVariant,
      internalVariant
    )

  val notFoundOrConflict =
    oneOf[HttpError](
      unauthorizedVariant,
      forbiddenVariant,
      notFoundVariant,
      conflictVariant,
      internalVariant
    )

  val validationOrConflict =
    oneOf[HttpError](
      unauthorizedVariant,
      forbiddenVariant,
      validationVariant,
      conflictVariant,
      internalVariant
    )

  val validationOrNotFoundOrConflict =
    oneOf[HttpError](
      unauthorizedVariant,
      forbiddenVariant,
      validationVariant,
      notFoundVariant,
      conflictVariant,
      internalVariant
    )
