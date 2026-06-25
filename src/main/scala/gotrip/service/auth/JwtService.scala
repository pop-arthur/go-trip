package gotrip.service.auth

import cats.effect.Sync
import cats.syntax.all._
import gotrip.config.AuthConfig
import gotrip.domain.user.*
import gotrip.domain.userrole.Role
import gotrip.http.HttpError
import gotrip.http.auth.AuthenticatedUser
import io.circe.{Decoder, Encoder, Json}
import io.circe.parser.decode
import io.circe.syntax._
import pdi.jwt.{JwtAlgorithm, JwtCirce, JwtClaim}

import java.time.Instant
import java.util.UUID

enum TokenType(val value: String):
  case Access extends TokenType("access")
  case Refresh extends TokenType("refresh")

final case class JwtCustomClaims(
  tokenType: String,
  email: String,
  roles: List[String],
  sid: String
)

object JwtCustomClaims:
  given Encoder[JwtCustomClaims] =
    Encoder.instance { claims =>
      Json.obj(
        "token_type" -> claims.tokenType.asJson,
        "email" -> claims.email.asJson,
        "roles" -> claims.roles.asJson,
        "sid" -> claims.sid.asJson
      )
    }

  given Decoder[JwtCustomClaims] =
    Decoder.instance { cursor =>
      for
        tokenType <- cursor.downField("token_type").as[String]
        email <- cursor.downField("email").as[String]
        roles <- cursor.downField("roles").as[List[String]]
        sid <- cursor.downField("sid").as[String]
      yield JwtCustomClaims(tokenType, email, roles, sid)
    }

final class JwtService[F[_]: Sync](config: AuthConfig):

  def issueAccessToken(userId: UserId, email: UserEmail, roles: List[Role], sessionId: UUID, now: Instant): F[String] =
    issueToken(TokenType.Access, userId, email, roles, sessionId, now, now.plusSeconds(config.accessTokenTtl.toSeconds))

  def issueRefreshToken(userId: UserId, email: UserEmail, roles: List[Role], sessionId: UUID, now: Instant): F[String] =
    issueToken(TokenType.Refresh, userId, email, roles, sessionId, now, now.plusSeconds(config.refreshTokenTtl.toSeconds))

  def authenticateAccess(token: String): F[Either[HttpError, AuthenticatedUser]] =
    decodeToken(token, TokenType.Access).map(_.map { case (userId, email, roles, sessionId) =>
      AuthenticatedUser(userId, email, roles, sessionId)
    })

  def validateRefresh(token: String): F[Either[HttpError, AuthenticatedUser]] =
    decodeToken(token, TokenType.Refresh).map(_.map { case (userId, email, roles, sessionId) =>
      AuthenticatedUser(userId, email, roles, sessionId)
    })

  private def issueToken(
    tokenType: TokenType,
    userId: UserId,
    email: UserEmail,
    roles: List[Role],
    sessionId: UUID,
    now: Instant,
    expiresAt: Instant
  ): F[String] =
    Sync[F].delay {
      val customClaims = JwtCustomClaims(
        tokenType = tokenType.value,
        email = email.value,
        roles = roles.map(Role.toString),
        sid = sessionId.toString
      )
      val claim = JwtClaim(
        content = customClaims.asJson.noSpaces,
        issuer = Some(config.issuer),
        subject = Some(userId.value.toString),
        expiration = Some(expiresAt.getEpochSecond),
        issuedAt = Some(now.getEpochSecond),
        jwtId = Some(UUID.randomUUID().toString)
      )

      JwtCirce.encode(claim, config.jwtSecret, JwtAlgorithm.HS256)
    }

  private def decodeToken(
    token: String,
    expectedType: TokenType
  ): F[Either[HttpError, (UserId, UserEmail, List[Role], UUID)]] =
    Sync[F].delay {
      JwtCirce
        .decode(token, config.jwtSecret, Seq(JwtAlgorithm.HS256))
        .toEither
        .leftMap(_ => unauthorized)
        .flatMap { claim =>
          if !claim.issuer.contains(config.issuer) then Left(unauthorized)
          else
            for
              subject <- claim.subject.toRight(unauthorized)
              userId <- subject.toLongOption.map(UserId.apply).toRight(unauthorized)
              customClaims <- decode[JwtCustomClaims](claim.content).leftMap(_ => unauthorized)
              _ <- Either.cond(customClaims.tokenType == expectedType.value, (), unauthorized)
              sessionId <- Either.catchOnly[IllegalArgumentException](UUID.fromString(customClaims.sid)).leftMap(_ => unauthorized)
              roles = customClaims.roles.flatMap(Role.fromString)
              _ <- Either.cond(roles.nonEmpty, (), unauthorized)
            yield (userId, UserEmail(customClaims.email), roles, sessionId)
        }
    }

  private val unauthorized: HttpError =
    HttpError.Unauthorized("Authentication is required")
