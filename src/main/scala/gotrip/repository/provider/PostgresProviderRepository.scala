package gotrip.repository.provider

import java.util.UUID

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.provider.*
import gotrip.repository.SkunkCodecs.providerType
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

final class PostgresProviderRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends ProviderRepository[F]:

  override def search(params: ProviderSearchParams): F[List[Provider]] =
    sessionPool.use { session =>
      session.prepare(PostgresProviderRepository.searchQuery).flatMap { query =>
        query.stream(PostgresProviderRepository.toSearchInput(params), 64).compile.toList
      }
    }

  override def findById(id: ProviderId): F[Option[Provider]] =
    sessionPool.use { session =>
      session.prepare(PostgresProviderRepository.findByIdQuery).flatMap { query =>
        query.option(id.value)
      }
    }

  override def create(provider: Provider): F[Provider] =
    sessionPool.use { session =>
      PostgresProviderRepository.uniqueProvider(session, PostgresProviderRepository.createFragment(provider))
    }

  override def update(id: ProviderId, provider: ProviderUpdate): F[Option[Provider]] =
    sessionPool.use { session =>
      PostgresProviderRepository.updateFragment(id, provider) match
        case Some(fragment) =>
          PostgresProviderRepository.optionProvider(session, fragment)
        case None =>
          session.prepare(PostgresProviderRepository.findByIdQuery).flatMap { query =>
            query.option(id.value)
          }
    }

  override def delete(id: ProviderId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresProviderRepository.deleteQuery).flatMap { query =>
        query.option(id.value).map(_.isDefined)
      }
    }

  override def nameExists(name: ProviderName, excludeProviderId: Option[ProviderId] = None): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresProviderRepository.nameExistsQuery).flatMap { query =>
        query.option(PostgresProviderRepository.toNameExistsInput(name, excludeProviderId)).map(_.isDefined)
      }
    }

  override def hasAdditionalServices(id: ProviderId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresProviderRepository.hasAdditionalServicesQuery).flatMap { query =>
        query.option(id.value).map(_.isDefined)
      }
    }

object PostgresProviderRepository:
  private type SearchInput = (Option[ProviderType], Option[String])
  private type NameExistsInput = (String, Boolean, UUID)

  def make[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): ProviderRepository[F] =
    new PostgresProviderRepository(sessionPool)

  private def uniqueProvider[F[_]: Concurrent](
    session: Session[F],
    fragment: AppliedFragment
  ): F[Provider] =
    session.prepare(fragment.fragment.query(providerDecoder)).flatMap { query =>
      query.unique(fragment.argument)
    }

  private def optionProvider[F[_]: Concurrent](
    session: Session[F],
    fragment: AppliedFragment
  ): F[Option[Provider]] =
    session.prepare(fragment.fragment.query(providerDecoder)).flatMap { query =>
      query.option(fragment.argument)
    }

  private val providerDecoder: Decoder[Provider] =
    (uuid ~ text ~ providerType ~ text.opt ~ text.opt)
      .map {
        case id ~ name ~ decodedProviderType ~ website ~ supportContact =>
          Provider(
            id = ProviderId(id),
            name = ProviderName(name),
            `type` = decodedProviderType,
            website = website,
            support_contact = supportContact
          )
      }

  val searchQuery: Query[SearchInput, Provider] =
    sql"""
      select id, name::text, type, website::text, support_contact::text
      from providers
      where type = coalesce(${providerType.opt}, type)
        and lower(name) like coalesce(${text.opt}, lower(name))
      order by name
    """.query(providerDecoder)

  val findByIdQuery: Query[UUID, Provider] =
    sql"""
      select id, name::text, type, website::text, support_contact::text
      from providers
      where id = $uuid
    """.query(providerDecoder)

  val deleteQuery: Query[UUID, UUID] =
    sql"""
      delete from providers
      where id = $uuid
      returning id
    """.query(uuid)

  val nameExistsQuery: Query[NameExistsInput, UUID] =
    sql"""
      select id
      from providers
      where lower(name) = lower($text)
        and ($bool or id <> $uuid)
      limit 1
    """.query(uuid)

  val hasAdditionalServicesQuery: Query[UUID, UUID] =
    sql"""
      select id
      from additional_services
      where provider_id = $uuid
      limit 1
    """.query(uuid)

  private def toSearchInput(params: ProviderSearchParams): SearchInput =
    (
      params.`type`,
      params.query.map(query => s"%${query.toLowerCase}%")
    )

  private def toNameExistsInput(
    name: ProviderName,
    excludeProviderId: Option[ProviderId]
  ): NameExistsInput =
    (
      name.value,
      excludeProviderId.isEmpty,
      excludeProviderId.map(_.value).getOrElse(UUID.fromString("00000000-0000-0000-0000-000000000000"))
    )

  private def createFragment(provider: Provider): AppliedFragment =
    val fields =
      List(
        "id" -> sql"$uuid"(provider.id.value),
        "name" -> sql"$text"(provider.name.value),
        "type" -> sql"$providerType"(provider.`type`)
      ) ++ List(
        provider.website.map(value => "website" -> sql"$text"(value)),
        provider.support_contact.map(value => "support_contact" -> sql"$text"(value))
      ).flatten

    val columns = fields.map(_._1).mkString(", ")
    val values = combineApplied(fields.map(_._2))
    AppliedFragment(
      sql"""
        insert into providers (#$columns)
        values (${values.fragment})
        returning id, name::text, type, website::text, support_contact::text
      """,
      values.argument
    )

  private def updateFragment(id: ProviderId, provider: ProviderUpdate): Option[AppliedFragment] =
    val fields =
      List(
        provider.name.map(value => sql"name = $text"(value.value)),
        provider.`type`.map(value => sql"type = $providerType"(value)),
        provider.website.map(value => sql"website = $text"(value)),
        provider.support_contact.map(value => sql"support_contact = $text"(value))
      ).flatten

    fields.headOption.map { head =>
      val sets = combineApplied(head :: fields.tail)
      AppliedFragment(sql"update providers set ${sets.fragment}", sets.argument) |+|
        sql"""
          where id = $uuid
          returning id, name::text, type, website::text, support_contact::text
        """(id.value)
    }

  private def combineApplied(fragments: List[AppliedFragment]): AppliedFragment =
    fragments.reduceLeft { (left, right) =>
      left |+| sql", "(Void) |+| right
    }
