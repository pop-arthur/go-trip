package gotrip.repository.additionalservice

import java.util.UUID

import cats.effect.{Concurrent, Resource}
import cats.syntax.flatMap.*
import cats.syntax.functor.*
import gotrip.domain.additionalservice.*
import gotrip.domain.location.*
import gotrip.domain.provider.*
import gotrip.repository.SkunkCodecs.serviceType
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

  override def create(service: AdditionalService): F[AdditionalService] =
    sessionPool.use { session =>
      PostgresAdditionalServiceRepository.uniqueAdditionalService(
        session,
        PostgresAdditionalServiceRepository.createFragment(service)
      )
    }

  override def update(id: ServiceId, service: AdditionalServiceUpdate): F[Option[AdditionalService]] =
    sessionPool.use { session =>
      PostgresAdditionalServiceRepository.updateFragment(id, service) match
        case Some(fragment) =>
          PostgresAdditionalServiceRepository.optionAdditionalService(session, fragment)
        case None =>
          session.prepare(PostgresAdditionalServiceRepository.findByIdQuery).flatMap { query =>
            query.option(id.value)
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
  private type SearchInput = (Option[ServiceType], Option[UUID], Option[UUID])

  def make[F[_]: Concurrent](
    sessionPool: Resource[F, Session[F]]
  ): AdditionalServiceRepository[F] =
    new PostgresAdditionalServiceRepository(sessionPool)

  private def uniqueAdditionalService[F[_]: Concurrent](
    session: Session[F],
    fragment: AppliedFragment
  ): F[AdditionalService] =
    session.prepare(fragment.fragment.query(additionalServiceDecoder)).flatMap { query =>
      query.unique(fragment.argument)
    }

  private def optionAdditionalService[F[_]: Concurrent](
    session: Session[F],
    fragment: AppliedFragment
  ): F[Option[AdditionalService]] =
    session.prepare(fragment.fragment.query(additionalServiceDecoder)).flatMap { query =>
      query.option(fragment.argument)
    }

  private val additionalServiceDecoder: Decoder[AdditionalService] =
    (uuid ~ text ~ text.opt ~ serviceType ~ uuid.opt ~ uuid.opt ~ float8.opt ~ text.opt ~ bool)
      .map {
        case id ~ title ~ description ~ decodedServiceType ~ providerId ~ locationId ~ priceAmount ~ priceCurrency ~ isActive =>
          AdditionalService(
            id = ServiceId(id),
            title = ServiceTitle(title),
            description = description,
            service_type = decodedServiceType,
            provider_id = providerId.map(ProviderId.apply),
            location_id = locationId.map(LocationId.apply),
            price_amount = priceAmount,
            price_currency = priceCurrency,
            is_active = isActive
          )
      }

  val searchQuery: Query[SearchInput, AdditionalService] =
    sql"""
      select id, title::text, description, service_type, provider_id, location_id, price_amount, price_currency::text, is_active
      from additional_services
      where service_type = coalesce(${serviceType.opt}, service_type)
        and provider_id is not distinct from coalesce(${uuid.opt}, provider_id)
        and location_id is not distinct from coalesce(${uuid.opt}, location_id)
      order by title
    """.query(additionalServiceDecoder)

  val findByIdQuery: Query[UUID, AdditionalService] =
    sql"""
      select id, title::text, description, service_type, provider_id, location_id, price_amount, price_currency::text, is_active
      from additional_services
      where id = $uuid
    """.query(additionalServiceDecoder)

  val deleteQuery: Query[UUID, UUID] =
    sql"""
      delete from additional_services
      where id = $uuid
      returning id
    """.query(uuid)

  val providerExistsQuery: Query[UUID, UUID] =
    sql"""
      select id
      from providers
      where id = $uuid
    """.query(uuid)

  val locationExistsQuery: Query[UUID, UUID] =
    sql"""
      select id
      from locations
      where id = $uuid
    """.query(uuid)

  private def toSearchInput(params: AdditionalServiceSearchParams): SearchInput =
    (
      params.serviceType,
      params.providerId.map(_.value),
      params.locationId.map(_.value)
    )

  private def createFragment(service: AdditionalService): AppliedFragment =
    val fields =
      List(
        "id" -> sql"$uuid"(service.id.value),
        "title" -> sql"$text"(service.title.value),
        "service_type" -> sql"$serviceType"(service.service_type),
        "is_active" -> sql"$bool"(service.is_active)
      ) ++ List(
        service.description.map(value => "description" -> sql"$text"(value)),
        service.provider_id.map(value => "provider_id" -> sql"$uuid"(value.value)),
        service.location_id.map(value => "location_id" -> sql"$uuid"(value.value)),
        service.price_amount.map(value => "price_amount" -> sql"$float8"(value)),
        service.price_currency.map(value => "price_currency" -> sql"$text"(value))
      ).flatten

    val columns = fields.map(_._1).mkString(", ")
    val values = combineApplied(fields.map(_._2))
    AppliedFragment(
      sql"""
        insert into additional_services (#$columns)
        values (${values.fragment})
        returning id, title::text, description, service_type, provider_id, location_id, price_amount, price_currency::text, is_active
      """,
      values.argument
    )

  private def updateFragment(id: ServiceId, service: AdditionalServiceUpdate): Option[AppliedFragment] =
    val fields =
      List(
        service.title.map(value => sql"title = $text"(value.value)),
        service.description.map(value => sql"description = $text"(value)),
        service.service_type.map(value => sql"service_type = $serviceType"(value)),
        service.provider_id.map(value => sql"provider_id = $uuid"(value.value)),
        service.location_id.map(value => sql"location_id = $uuid"(value.value)),
        service.price_amount.map(value => sql"price_amount = $float8"(value)),
        service.price_currency.map(value => sql"price_currency = $text"(value)),
        service.is_active.map(value => sql"is_active = $bool"(value))
      ).flatten

    fields.headOption.map { head =>
      val sets = combineApplied(head :: fields.tail)
      AppliedFragment(sql"update additional_services set ${sets.fragment}", sets.argument) |+|
        sql"""
          where id = $uuid
          returning id, title::text, description, service_type, provider_id, location_id, price_amount, price_currency::text, is_active
        """(id.value)
    }

  private def combineApplied(fragments: List[AppliedFragment]): AppliedFragment =
    fragments.reduceLeft { (left, right) =>
      left |+| sql", "(Void) |+| right
    }
