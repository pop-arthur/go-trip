package gotrip.http

import cats.data.NonEmptyList
import gotrip.domain.validation.DomainValidation

object ValidationError:

  def toHttpError(errors: NonEmptyList[DomainValidation]): HttpError.Validation =
    HttpError.Validation(errorMessage(errors))

  def toApiError(errors: NonEmptyList[DomainValidation]): ApiError =
    ApiError(
      code = "VALIDATION_ERROR",
      message = errorMessage(errors)
    )

  private def errorMessage(errors: NonEmptyList[DomainValidation]): String =
    errors.toList.map(_.errorMessage).mkString("; ")
