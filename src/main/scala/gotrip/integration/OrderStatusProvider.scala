package gotrip.integration

import gotrip.domain.order.*

trait OrderStatusProvider[F[_]]:
  def checkUpdates(order: Order): F[Either[OrderStatusProviderError, List[ExternalOrderStatusUpdate]]]
