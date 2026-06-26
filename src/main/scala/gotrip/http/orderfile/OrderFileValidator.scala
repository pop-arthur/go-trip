package gotrip.http.orderfile

import cats.data.ValidatedNel
import gotrip.domain.order.*
import gotrip.domain.validation.DomainValidation

object OrderFileValidator:

  def validate(file: OrderFileCreate): ValidatedNel[DomainValidation, OrderFileCreate] =
    OrderFileCreate.validate(file)
