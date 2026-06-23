package gotrip.http

final case class ApiError(
  code: String,
  message: String
)
