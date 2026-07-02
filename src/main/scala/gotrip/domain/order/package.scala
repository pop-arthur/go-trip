package gotrip.domain

import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

import scala.annotation.targetName
import java.util.UUID

package object order {
  opaque type OrderId = UUID
  object OrderId {
    def apply(value: UUID): OrderId = value

    def from(value: UUID): Result[OrderId] =
      valid(OrderId(value))
  }
  extension (id: OrderId) {
    def value: UUID = id
  }

  opaque type OrderStatusEventId = UUID
  object OrderStatusEventId {
    def apply(value: UUID): OrderStatusEventId = value

    def from(value: UUID): Result[OrderStatusEventId] =
      valid(OrderStatusEventId(value))
  }
  extension (id: OrderStatusEventId) {
    @targetName("orderStatusEventIdValue")
    def value: UUID = id
  }

  opaque type OrderFileId = UUID
  object OrderFileId {
    def apply(value: UUID): OrderFileId = value

    def from(value: UUID): Result[OrderFileId] =
      valid(OrderFileId(value))
  }
  extension (id: OrderFileId) {
    @targetName("orderFileIdValue")
    def value: UUID = id
  }

  opaque type OrderTitle = String
  object OrderTitle {
    def apply(value: String): OrderTitle = value

    def from(value: String): Result[OrderTitle] =
      validateNonBlank(value, OrderTitleIsBlank)(OrderTitle.apply)

    def unwrap(title: OrderTitle): String = title
  }
  extension (title: OrderTitle) {
    @targetName("orderTitleValue")
    def value: String = title
  }

  opaque type OrderFileUrl = String
  object OrderFileUrl {
    def apply(value: String): OrderFileUrl = value

    def from(value: String): Result[OrderFileUrl] =
      validateNonBlank(value, OrderFileUrlIsBlank)(OrderFileUrl.apply)

    def unwrap(url: OrderFileUrl): String = url
  }
  extension (url: OrderFileUrl) {
    @targetName("orderFileUrlValue")
    def value: String = url
  }
}
