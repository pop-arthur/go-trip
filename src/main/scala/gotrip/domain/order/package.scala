package gotrip.domain

import gotrip.domain.validation.DomainValidation.Result
import gotrip.domain.validation.DomainValidation.*

import scala.annotation.targetName

package object order {
  opaque type OrderId = Long
  object OrderId {
    def apply(value: Long): OrderId = value

    def from(value: Long): Result[OrderId] =
      validatePositiveLong(value, IdIsNotPositive)(OrderId.apply)
  }
  extension (id: OrderId) {
    def value: Long = id
  }

  opaque type OrderStatusEventId = Long
  object OrderStatusEventId {
    def apply(value: Long): OrderStatusEventId = value

    def from(value: Long): Result[OrderStatusEventId] =
      validatePositiveLong(value, IdIsNotPositive)(OrderStatusEventId.apply)
  }
  extension (id: OrderStatusEventId) {
    @targetName("orderStatusEventIdValue")
    def value: Long = id
  }

  opaque type OrderFileId = Long
  object OrderFileId {
    def apply(value: Long): OrderFileId = value

    def from(value: Long): Result[OrderFileId] =
      validatePositiveLong(value, IdIsNotPositive)(OrderFileId.apply)
  }
  extension (id: OrderFileId) {
    @targetName("orderFileIdValue")
    def value: Long = id
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
