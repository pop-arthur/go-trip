package gotrip.repository

import gotrip.domain.additionalservice.ServiceType
import gotrip.domain.location.LocationType
import gotrip.domain.order.{FileType, OrderStatus, OrderStatusEventSource}
import gotrip.domain.provider.ProviderType
import gotrip.domain.trip.TripStatus
import skunk.Codec
import skunk.codec.all.`enum`
import skunk.data.Type

object SkunkCodecs:

  val locationType: Codec[LocationType] =
    `enum`(
      encodeLocationType,
      decodeLocationType,
      Type("location_type")
    )

  val providerType: Codec[ProviderType] =
    `enum`(
      encodeProviderType,
      decodeProviderType,
      Type("provider_type")
    )

  val serviceType: Codec[ServiceType] =
    `enum`(
      encodeServiceType,
      decodeServiceType,
      Type("service_type")
    )

  val tripStatus: Codec[TripStatus] =
    `enum`(
      encodeTripStatus,
      decodeTripStatus,
      Type("trip_status")
    )

  def encodeLocationType(locationType: LocationType): String =
    locationType match
      case LocationType.Country      => "COUNTRY"
      case LocationType.City         => "CITY"
      case LocationType.Airport      => "AIRPORT"
      case LocationType.TrainStation => "TRAIN_STATION"
      case LocationType.BusStation   => "BUS_STATION"
      case LocationType.Port         => "PORT"
      case LocationType.Hotel        => "HOTEL"
      case LocationType.MeetingPoint => "MEETING_POINT"
      case LocationType.Attraction   => "ATTRACTION"
      case LocationType.Other        => "OTHER"

  def decodeLocationType(value: String): Option[LocationType] =
    value match
      case "COUNTRY"       => Some(LocationType.Country)
      case "CITY"          => Some(LocationType.City)
      case "AIRPORT"       => Some(LocationType.Airport)
      case "TRAIN_STATION" => Some(LocationType.TrainStation)
      case "BUS_STATION"   => Some(LocationType.BusStation)
      case "PORT"          => Some(LocationType.Port)
      case "HOTEL"         => Some(LocationType.Hotel)
      case "MEETING_POINT" => Some(LocationType.MeetingPoint)
      case "ATTRACTION"    => Some(LocationType.Attraction)
      case "OTHER"         => Some(LocationType.Other)
      case _               => None

  def encodeProviderType(providerType: ProviderType): String =
    providerType match
      case ProviderType.Airline          => "AIRLINE"
      case ProviderType.Hotel            => "HOTEL"
      case ProviderType.TourCompany      => "TOUR_COMPANY"
      case ProviderType.TransportCompany => "TRANSPORT_COMPANY"
      case ProviderType.BookingPlatform  => "BOOKING_PLATFORM"
      case ProviderType.InsuranceCompany => "INSURANCE_COMPANY"
      case ProviderType.Other            => "OTHER"

  def decodeProviderType(value: String): Option[ProviderType] =
    value match
      case "AIRLINE"           => Some(ProviderType.Airline)
      case "HOTEL"             => Some(ProviderType.Hotel)
      case "TOUR_COMPANY"      => Some(ProviderType.TourCompany)
      case "TRANSPORT_COMPANY" => Some(ProviderType.TransportCompany)
      case "BOOKING_PLATFORM"  => Some(ProviderType.BookingPlatform)
      case "INSURANCE_COMPANY" => Some(ProviderType.InsuranceCompany)
      case "OTHER"             => Some(ProviderType.Other)
      case _                   => None

  def encodeServiceType(serviceType: ServiceType): String =
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

  def decodeServiceType(value: String): Option[ServiceType] =
    value match
      case "FLIGHT"        => Some(ServiceType.Flight)
      case "TRAIN"         => Some(ServiceType.Train)
      case "BUS"           => Some(ServiceType.Bus)
      case "HOTEL"         => Some(ServiceType.Hotel)
      case "TOUR"          => Some(ServiceType.Tour)
      case "CAR_RENTAL"    => Some(ServiceType.CarRental)
      case "INSURANCE"     => Some(ServiceType.Insurance)
      case "TAXI"          => Some(ServiceType.Taxi)
      case "ESIM"          => Some(ServiceType.Esim)
      case "LOUNGE"        => Some(ServiceType.Lounge)
      case "EXTRA_BAGGAGE" => Some(ServiceType.ExtraBaggage)
      case "OTHER"         => Some(ServiceType.Other)
      case _               => None

  def encodeTripStatus(status: TripStatus): String =
    status match
      case TripStatus.Planned   => "PLANNED"
      case TripStatus.Active    => "ACTIVE"
      case TripStatus.Completed => "COMPLETED"
      case TripStatus.Cancelled => "CANCELLED"

  def decodeTripStatus(value: String): Option[TripStatus] =
    value match
      case "PLANNED"   => Some(TripStatus.Planned)
      case "ACTIVE"    => Some(TripStatus.Active)
      case "COMPLETED" => Some(TripStatus.Completed)
      case "CANCELLED" => Some(TripStatus.Cancelled)
      case _           => None

  def encodeOrderStatus(status: OrderStatus): String =
    status match
      case OrderStatus.PendingVerification => "PENDING_VERIFICATION"
      case OrderStatus.Confirmed           => "CONFIRMED"
      case OrderStatus.Delayed             => "DELAYED"
      case OrderStatus.Cancelled           => "CANCELLED"
      case OrderStatus.Completed           => "COMPLETED"
      case OrderStatus.RefundPending       => "REFUND_PENDING"
      case OrderStatus.Refunded            => "REFUNDED"

  def decodeOrderStatus(value: String): Option[OrderStatus] =
    value match
      case "PENDING_VERIFICATION" => Some(OrderStatus.PendingVerification)
      case "CONFIRMED"            => Some(OrderStatus.Confirmed)
      case "DELAYED"              => Some(OrderStatus.Delayed)
      case "CANCELLED"            => Some(OrderStatus.Cancelled)
      case "COMPLETED"            => Some(OrderStatus.Completed)
      case "REFUND_PENDING"       => Some(OrderStatus.RefundPending)
      case "REFUNDED"             => Some(OrderStatus.Refunded)
      case _                      => None

  def encodeFileType(fileType: FileType): String =
    fileType match
      case FileType.Pdf   => "PDF"
      case FileType.Image => "IMAGE"
      case FileType.Email => "EMAIL"
      case FileType.Json  => "JSON"
      case FileType.Other => "OTHER"

  def decodeFileType(value: String): Option[FileType] =
    value match
      case "PDF"   => Some(FileType.Pdf)
      case "IMAGE" => Some(FileType.Image)
      case "EMAIL" => Some(FileType.Email)
      case "JSON"  => Some(FileType.Json)
      case "OTHER" => Some(FileType.Other)
      case _       => None

  def encodeOrderStatusEventSource(source: OrderStatusEventSource): String =
    source match
      case OrderStatusEventSource.System          => "system"
      case OrderStatusEventSource.AdminSimulation => "admin_simulation"
      case OrderStatusEventSource.UserEdit        => "user_edit"

  def decodeOrderStatusEventSource(value: String): Option[OrderStatusEventSource] =
    value match
      case "system"           => Some(OrderStatusEventSource.System)
      case "admin_simulation" => Some(OrderStatusEventSource.AdminSimulation)
      case "user_edit"        => Some(OrderStatusEventSource.UserEdit)
      case _                  => None
