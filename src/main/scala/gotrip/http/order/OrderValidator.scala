package gotrip.http.order

import cats.data.ValidatedNel
import gotrip.domain.order.*
import gotrip.domain.validation.DomainValidation

object OrderValidator:

  def validate(order: OrderCreate): ValidatedNel[DomainValidation, OrderCreate] =
    OrderCreate.validate(order)

  def validate(order: OrderUpdate): ValidatedNel[DomainValidation, OrderUpdate] =
    OrderUpdate.validate(order)

  def validate(update: OrderStatusUpdate): ValidatedNel[DomainValidation, OrderStatusUpdate] =
    OrderStatusUpdate.validate(update)
