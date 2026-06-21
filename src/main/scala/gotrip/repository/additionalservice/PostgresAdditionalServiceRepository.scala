package gotrip.repository.additionalservice

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.additionalservice.*
import gotrip.domain.location.*
import gotrip.domain.provider.*
import skunk.*
import skunk.codec.all.*
import skunk.implicits.*

final class PostgresAdditionalServiceRepository[F[_]: Concurrent](
  sessionPool: Resource[F, Session[F]]
) extends AdditionalServiceRepository[F]:

  override def search(params: AdditionalServiceSearchParams): F[List[AdditionalService]] =
    sessionPool.use { session =>
      session.prepare(PostgresAdditionalServiceRepository.searchQuery).flatMap { query =>
        query.stream(PostgresAdditionalServiceRepository.toSearchInput(params), 64).compile.toList
      }
    }

  override def findById(id: ServiceId): F[Option[AdditionalService]] =
    sessionPool.use { session =>
      session.prepare(PostgresAdditionalServiceRepository.findByIdQuery).flatMap { query =>
        query.option(id.value)
      }
    }

  override def create(service: AdditionalServiceCreate, isActive: Boolean): F[AdditionalService] =
    sessionPool.use { session =>
      session.prepare(PostgresAdditionalServiceRepository.createQuery).flatMap { query =>
        query.unique(PostgresAdditionalServiceRepository.toCreateInput(service, isActive))
      }
    }

  override def update(id: ServiceId, service: AdditionalServiceUpdate): F[Option[AdditionalService]] =
    sessionPool.use { session =>
      session.prepare(PostgresAdditionalServiceRepository.updateQuery).flatMap { query =>
        query.option(PostgresAdditionalServiceRepository.toUpdateInput(id, service))
      }
    }

  override def delete(id: ServiceId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresAdditionalServiceRepository.deleteQuery).flatMap { query =>
        query.option(id.value).map(_.isDefined)
      }
    }

  override def providerExists(id: ProviderId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresAdditionalServiceRepository.providerExistsQuery).flatMap { query =>
        query.option(id.value).map(_.isDefined)
      }
    }

  override def locationExists(id: LocationId): F[Boolean] =
    sessionPool.use { session =>
      session.prepare(PostgresAdditionalServiceRepository.locationExistsQuery).flatMap { query =>
        query.option(id.value).map(_.isDefined)
      }
    }

object PostgresAdditionalServiceRepository:
  private type SearchInput = (Option[String], Option[Long], Option[Long])
  private type CreateInput =
    (String, Option[String], String, Option[Long], Option[Long], Option[Double], Option[String], Boolean)
  private type UpdateInput =
    (
      Option[String],
      Boolean,
      Option[String],
      Option[String],
      Boolean,
      Option[Long],
      Boolean,
      Option[Long],
      Boolean,
      Option[Double],
      Boolean,
      Option[String],
      Option[Boolean],
      Long
    )

  def make[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): AdditionalServiceRepository[F] =
    new PostgresAdditionalServiceRepository(sessionPool)

  private val additionalServiceDecoder: Decoder[AdditionalService] =
    (int8 ~ text ~ text.opt ~ text ~ int8.opt ~ int8.opt ~ float8.opt ~ text.opt ~ bool)
      .map {
        case id ~ title ~ description ~ serviceType ~ providerId ~ locationId ~ priceAmount ~ priceCurrency ~ isActive =>
          AdditionalService(
            id = ServiceId(id),
            title = ServiceTitle(title),
            description = description,
            service_type = decodeServiceType(serviceType),
            provider_id = providerId.map(ProviderId.apply),
            location_id = locationId.map(LocationId.apply),
            price_amount = priceAmount,
            price_currency = priceCurrency,
            is_active = isActive
          )
      }

  val searchQuery: Query[SearchInput, AdditionalService] =
    sql"""
      select id, title::text, description, service_type::text, provider_id, location_id, price_amount, price_currency::text, is_active
      from additional_services
      where service_type::text = coalesce(${text.opt}, service_type::text)
        and provider_id is not distinct from coalesce(${int8.opt}, provider_id)
        and location_id is not distinct from coalesce(${int8.opt}, location_id)
      order by title
    """.query(additionalServiceDecoder)

  val findByIdQuery: Query[Long, AdditionalService] =
    sql"""
      select id, title::text, description, service_type::text, provider_id, location_id, price_amount, price_currency::text, is_active
      from additional_services
      where id = $int8
    """.query(additionalServiceDecoder)

  val createQuery: Query[CreateInput, AdditionalService] =
    sql"""
      insert into additional_services (
        title,
        description,
        service_type,
        provider_id,
        location_id,
        price_amount,
        price_currency,
        is_active
      )
      values ($text, ${text.opt}, ${text}::service_type, ${int8.opt}, ${int8.opt}, ${float8.opt}, ${text.opt}, $bool)
      returning id, title::text, description, service_type::text, provider_id, location_id, price_amount, price_currency::text, is_active
    """.query(additionalServiceDecoder)

  val updateQuery: Query[UpdateInput, AdditionalService] =
    sql"""
      update additional_services
      set title = coalesce(${text.opt}, title),
          description = case when $bool then ${text.opt} else description end,
          service_type = coalesce(${text.opt}::service_type, service_type),
          provider_id = case when $bool then ${int8.opt} else provider_id end,
          location_id = case when $bool then ${int8.opt} else location_id end,
          price_amount = case when $bool then ${float8.opt} else price_amount end,
          price_currency = case when $bool then ${text.opt} else price_currency end,
          is_active = coalesce(${bool.opt}, is_active)
      where id = $int8
      returning id, title::text, description, service_type::text, provider_id, location_id, price_amount, price_currency::text, is_active
    """.query(additionalServiceDecoder)

  val deleteQuery: Query[Long, Long] =
    sql"""
      delete from additional_services
      where id = $int8
      returning id
    """.query(int8)

  val providerExistsQuery: Query[Long, Long] =
    sql"""
      select id
      from providers
      where id = $int8
    """.query(int8)

  val locationExistsQuery: Query[Long, Long] =
    sql"""
      select id
      from locations
      where id = $int8
    """.query(int8)

  private def toSearchInput(params: AdditionalServiceSearchParams): SearchInput =
    (
      params.serviceType.map(encodeServiceType),
      params.providerId.map(_.value),
      params.locationId.map(_.value)
    )

  private def toCreateInput(service: AdditionalServiceCreate, isActive: Boolean): CreateInput =
    (
      service.title.value,
      service.description,
      encodeServiceType(service.service_type),
      service.provider_id.map(_.value),
      service.location_id.map(_.value),
      service.price_amount,
      service.price_currency,
      isActive
    )

  private def toUpdateInput(id: ServiceId, service: AdditionalServiceUpdate): UpdateInput =
    (
      service.title.map(_.value),
      service.description.isDefined,
      service.description,
      service.service_type.map(encodeServiceType),
      service.provider_id.isDefined,
      service.provider_id.map(_.value),
      service.location_id.isDefined,
      service.location_id.map(_.value),
      service.price_amount.isDefined,
      service.price_amount,
      service.price_currency.isDefined,
      service.price_currency,
      service.is_active,
      id.value
    )

  private def encodeServiceType(serviceType: ServiceType): String =
    serviceType match
      case ServiceType.Flight       => "FLIGHT"
      case ServiceType.Train        => "TRAIN"
      case ServiceType.Bus          => "BUS"
      case ServiceType.Hotel        => "HOTEL"
      case ServiceType.Tour         => "TOUR"
      case ServiceType.CarRental    => "CAR_RENTAL"
      case ServiceType.Insurance    => "INSURANCE"
      case ServiceType.Taxi         => "TAXI"
      case ServiceType.Esim         => "ESIM"
      case ServiceType.Lounge       => "LOUNGE"
      case ServiceType.ExtraBaggage => "EXTRA_BAGGAGE"
      case ServiceType.Other        => "OTHER"

  private def decodeServiceType(value: String): ServiceType =
    value match
      case "FLIGHT"        => ServiceType.Flight
      case "TRAIN"         => ServiceType.Train
      case "BUS"           => ServiceType.Bus
      case "HOTEL"         => ServiceType.Hotel
      case "TOUR"          => ServiceType.Tour
      case "CAR_RENTAL"    => ServiceType.CarRental
      case "INSURANCE"     => ServiceType.Insurance
      case "TAXI"          => ServiceType.Taxi
      case "ESIM"          => ServiceType.Esim
      case "LOUNGE"        => ServiceType.Lounge
      case "EXTRA_BAGGAGE" => ServiceType.ExtraBaggage
      case "OTHER"         => ServiceType.Other
      case other           => throw new IllegalArgumentException(s"Unknown service type: $other")
