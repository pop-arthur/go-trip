package gotrip.repository.provider

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.provider.*
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

  override def create(provider: ProviderCreate): F[Provider] =
    sessionPool.use { session =>
      session.prepare(PostgresProviderRepository.createQuery).flatMap { query =>
        query.unique(PostgresProviderRepository.toCreateInput(provider))
      }
    }

  override def update(id: ProviderId, provider: ProviderUpdate): F[Option[Provider]] =
    sessionPool.use { session =>
      session.prepare(PostgresProviderRepository.updateQuery).flatMap { query =>
        query.option(PostgresProviderRepository.toUpdateInput(id, provider))
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
  private type SearchInput = (Option[String], Option[String])
  private type CreateInput = (String, String, Option[String], Option[String])
  private type UpdateInput =
    (Option[String], Option[String], Boolean, Option[String], Boolean, Option[String], Long)
  private type NameExistsInput = (String, Boolean, Long)

  def make[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): ProviderRepository[F] =
    new PostgresProviderRepository(sessionPool)

  private val providerDecoder: Decoder[Provider] =
    (int8 ~ text ~ text ~ text.opt ~ text.opt)
      .map {
        case id ~ name ~ providerType ~ website ~ supportContact =>
          Provider(
            id = ProviderId(id),
            name = ProviderName(name),
            `type` = decodeProviderType(providerType),
            website = website,
            support_contact = supportContact
          )
      }

  val searchQuery: Query[SearchInput, Provider] =
    sql"""
      select id, name::text, type::text, website::text, support_contact::text
      from providers
      where type::text = coalesce(${text.opt}, type::text)
        and lower(name) like coalesce(${text.opt}, lower(name))
      order by name
    """.query(providerDecoder)

  val findByIdQuery: Query[Long, Provider] =
    sql"""
      select id, name::text, type::text, website::text, support_contact::text
      from providers
      where id = $int8
    """.query(providerDecoder)

  val createQuery: Query[CreateInput, Provider] =
    sql"""
      insert into providers (name, type, website, support_contact)
      values ($text, ${text}::provider_type, ${text.opt}, ${text.opt})
      returning id, name::text, type::text, website::text, support_contact::text
    """.query(providerDecoder)

  val updateQuery: Query[UpdateInput, Provider] =
    sql"""
      update providers
      set name = coalesce(${text.opt}, name),
          type = coalesce(${text.opt}::provider_type, type),
          website = case when $bool then ${text.opt} else website end,
          support_contact = case when $bool then ${text.opt} else support_contact end
      where id = $int8
      returning id, name::text, type::text, website::text, support_contact::text
    """.query(providerDecoder)

  val deleteQuery: Query[Long, Long] =
    sql"""
      delete from providers
      where id = $int8
      returning id
    """.query(int8)

  val nameExistsQuery: Query[NameExistsInput, Long] =
    sql"""
      select id
      from providers
      where lower(name) = lower($text)
        and ($bool or id <> $int8)
      limit 1
    """.query(int8)

  val hasAdditionalServicesQuery: Query[Long, Long] =
    sql"""
      select id
      from additional_services
      where provider_id = $int8
      limit 1
    """.query(int8)

  private def toSearchInput(params: ProviderSearchParams): SearchInput =
    (
      params.`type`.map(encodeProviderType),
      params.query.map(query => s"%${query.toLowerCase}%")
    )

  private def toCreateInput(provider: ProviderCreate): CreateInput =
    (
      provider.name.value,
      encodeProviderType(provider.`type`),
      provider.website,
      provider.support_contact
    )

  private def toUpdateInput(id: ProviderId, provider: ProviderUpdate): UpdateInput =
    (
      provider.name.map(_.value),
      provider.`type`.map(encodeProviderType),
      provider.website.isDefined,
      provider.website,
      provider.support_contact.isDefined,
      provider.support_contact,
      id.value
    )

  private def toNameExistsInput(
    name: ProviderName,
    excludeProviderId: Option[ProviderId]
  ): NameExistsInput =
    (
      name.value,
      excludeProviderId.isEmpty,
      excludeProviderId.map(_.value).getOrElse(0L)
    )

  private def encodeProviderType(providerType: ProviderType): String =
    providerType match
      case ProviderType.Airline          => "AIRLINE"
      case ProviderType.Hotel            => "HOTEL"
      case ProviderType.TourCompany      => "TOUR_COMPANY"
      case ProviderType.TransportCompany => "TRANSPORT_COMPANY"
      case ProviderType.BookingPlatform  => "BOOKING_PLATFORM"
      case ProviderType.InsuranceCompany => "INSURANCE_COMPANY"
      case ProviderType.Other            => "OTHER"

  private def decodeProviderType(value: String): ProviderType =
    value match
      case "AIRLINE"             => ProviderType.Airline
      case "HOTEL"               => ProviderType.Hotel
      case "TOUR_COMPANY"        => ProviderType.TourCompany
      case "TRANSPORT_COMPANY"   => ProviderType.TransportCompany
      case "BOOKING_PLATFORM"    => ProviderType.BookingPlatform
      case "INSURANCE_COMPANY"   => ProviderType.InsuranceCompany
      case "OTHER"               => ProviderType.Other
      case other                 => throw new IllegalArgumentException(s"Unknown provider type: $other")
