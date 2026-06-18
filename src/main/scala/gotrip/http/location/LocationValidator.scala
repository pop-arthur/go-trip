package gotrip.http.location

import gotrip.domain.location.*

object LocationValidator:

  def validate(location: LocationCreate): Either[ApiError, LocationCreate] =
    for {
      _ <- validateName(location.name)
      _ <- validateLatitude(location.latitude)
      _ <- validateLongitude(location.longitude)
    } yield location

  def validate(location: LocationUpdate): Either[ApiError, LocationUpdate] =
    for {
      _ <- location.name.fold[Either[ApiError, Unit]](Right(()))(validateName)
      _ <- location.latitude.fold[Either[ApiError, Unit]](Right(()))(validateLatitude)
      _ <- location.longitude.fold[Either[ApiError, Unit]](Right(()))(validateLongitude)
    } yield location

  private def validateName(name: LocationName): Either[ApiError, Unit] =
    Either.cond(
      name.value.trim.nonEmpty,
      (),
      validationError("Location name must not be blank")
    )

  private def validateLatitude(latitude: LocationLatitude): Either[ApiError, Unit] =
    validateOptionalNumber(latitude.value, -90.0, 90.0, "Latitude")

  private def validateLongitude(longitude: LocationLongitude): Either[ApiError, Unit] =
    validateOptionalNumber(longitude.value, -180.0, 180.0, "Longitude")

  private def validateOptionalNumber(
    value: Option[Double],
    min: Double,
    max: Double,
    fieldName: String
  ): Either[ApiError, Unit] =
    value match
      case Some(number) if number < min || number > max =>
        Left(validationError(s"$fieldName must be between $min and $max"))
      case _ =>
        Right(())

  private def validationError(message: String): ApiError =
    ApiError("VALIDATION_ERROR", message)
