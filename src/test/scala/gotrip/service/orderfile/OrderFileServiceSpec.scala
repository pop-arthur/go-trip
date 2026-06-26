package gotrip.service.orderfile

import cats.Id
import gotrip.domain.order.*
import gotrip.domain.user.*
import gotrip.repository.orderfile.OrderFileRepository
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant

final class OrderFileServiceSpec extends AnyWordSpec with Matchers with MockFactory:

  "OrderFileService" should {
    "list files for an owned order" in {
      val repository = mock[OrderFileRepository[Id]]
      val service = OrderFileService[Id](repository)

      repository.orderExistsForUser.expects(userId, orderId).returning(true)
      repository.listByOrder.expects(userId, orderId).returning(List(orderFile))

      service.listByOrder(userId, orderId) shouldBe Right(List(orderFile))
    }

    "return order not found when listing files for an inaccessible order" in {
      val repository = mock[OrderFileRepository[Id]]
      val service = OrderFileService[Id](repository)

      repository.orderExistsForUser.expects(userId, orderId).returning(false)

      service.listByOrder(userId, orderId) shouldBe Left(OrderFileServiceError.OrderNotFound(orderId))
    }

    "create metadata for an owned order" in {
      val repository = mock[OrderFileRepository[Id]]
      val service = OrderFileService[Id](repository)

      repository.orderExistsForUser.expects(userId, orderId).returning(true)
      repository.create.expects(userId, orderId, orderFileCreate).returning(Some(orderFile))

      service.create(userId, orderId, orderFileCreate) shouldBe Right(orderFile)
    }

    "delete metadata for an owned order" in {
      val repository = mock[OrderFileRepository[Id]]
      val service = OrderFileService[Id](repository)

      repository.orderExistsForUser.expects(userId, orderId).returning(true)
      repository.delete.expects(userId, orderId, fileId).returning(true)

      service.delete(userId, orderId, fileId) shouldBe Right(())
    }
  }

  private val userId = UserId(1L)
  private val orderId = OrderId(10L)
  private val fileId = OrderFileId(100L)

  private val orderFileCreate = OrderFileCreate(
    file_url = OrderFileUrl("https://cdn.gotrip.example.com/orders/10/ticket.pdf"),
    file_type = FileType.Pdf,
    parsed_data = None
  )

  private val orderFile = OrderFile(
    id = fileId,
    order_id = orderId,
    file_url = orderFileCreate.file_url,
    file_type = orderFileCreate.file_type,
    parsed_data = None,
    uploaded_at = Instant.parse("2026-06-01T10:00:00Z")
  )
