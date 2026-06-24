package gotrip.http.auth

import cats.effect.IO
import gotrip.service.auth.AuthService
import sttp.tapir.server.ServerEndpoint

final class AuthController(
  service: AuthService[IO],
  authSupport: AuthSupport
):

  val register: ServerEndpoint[Any, IO] =
    AuthEndpoints.register.serverLogic(service.register)

  val login: ServerEndpoint[Any, IO] =
    AuthEndpoints.login.serverLogic(service.login)

  val refresh: ServerEndpoint[Any, IO] =
    AuthEndpoints.refresh.serverLogic(service.refresh)

  val logout: ServerEndpoint[Any, IO] =
    AuthEndpoints.logout
      .serverSecurityLogic(authSupport.authenticate)
      .serverLogic(user => _ => service.logout(user))

  val all: List[ServerEndpoint[Any, IO]] =
    List(register, login, refresh, logout)
