package gotrip.http

import cats.data.NonEmptyList
import gotrip.domain.validation.DomainValidation

object ValidationError:

  def toApiError(errors: NonEmptyList[DomainValidation]): ApiError =
    ApiError(
      code = "VALIDATION_ERROR",
      message = errors.toList.map(_.errorMessage).mkString("; ")
    )
