package gotrip.integration

enum OrderStatusProviderError:
  case ExternalOrderIdMissing
  case ProviderUnavailable(message: String)
  case Unauthorized(message: String)
  case NotFound(message: String)
  case InvalidResponse(message: String)
  case Unexpected(message: String)
