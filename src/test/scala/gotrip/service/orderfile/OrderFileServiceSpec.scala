package gotrip.service.orderfile

import cats.effect.IO
import cats.effect.unsafe.implicits.global
import gotrip.domain.order.*
import gotrip.domain.user.*
import gotrip.repository.orderfile.OrderFileRepository
import gotrip.service.{GeneratedData, GeneratedDataTestSupport}
import org.scalamock.scalatest.MockFactory
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

import java.time.Instant
import java.util.UUID

final class OrderFileServiceSpec extends AnyWordSpec with Matchers with MockFactory with GeneratedDataTestSupport:

  "OrderFileService" should {
    "list files for an owned order" in {
      val repository = mock[OrderFileRepository[IO]]
      val service = OrderFileService[IO](repository)

      repository.orderExistsForUser.expects(userId, orderId).returning(IO.pure(true))
      repository.listByOrder.expects(userId, orderId).returning(IO.pure(List(orderFile)))

      service.listByOrder(userId, orderId).unsafeRunSync() shouldBe Right(List(orderFile))
    }

    "return order not found when listing files for an inaccessible order" in {
      val repository = mock[OrderFileRepository[IO]]
      val service = OrderFileService[IO](repository)

      repository.orderExistsForUser.expects(userId, orderId).returning(IO.pure(false))

      service.listByOrder(userId, orderId).unsafeRunSync() shouldBe Left(OrderFileServiceError.OrderNotFound(orderId))
    }

    "create metadata for an owned order" in {
      val repository = mock[OrderFileRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repository, generatedData)

      repository.orderExistsForUser.expects(userId, orderId).returning(IO.pure(true))
      expectGeneratedId(generatedData, fileId.value)
      expectGeneratedNow(generatedData, orderFile.uploaded_at)
      repository.create.expects(userId, orderFile).returning(IO.pure(Some(orderFile)))

      service.create(userId, orderId, orderFileCreate).unsafeRunSync() shouldBe Right(orderFile)
    }

    "find metadata for an owned order" in {
      val repository = mock[OrderFileRepository[IO]]
      val service = OrderFileService[IO](repository)

      repository.findByOrder.expects(userId, orderId, fileId).returning(IO.pure(Some(orderFile)))

      service.findByOrder(userId, orderId, fileId).unsafeRunSync() shouldBe Right(orderFile)
    }

    "return file not found when metadata does not exist" in {
      val repository = mock[OrderFileRepository[IO]]
      val service = OrderFileService[IO](repository)

      repository.findByOrder.expects(userId, orderId, fileId).returning(IO.pure(None))

      service.findByOrder(userId, orderId, fileId).unsafeRunSync() shouldBe
        Left(OrderFileServiceError.OrderFileNotFound(fileId))
    }

    "return order not found when create cannot persist metadata for the user order" in {
      val repository = mock[OrderFileRepository[IO]]
      val generatedData = generatedDataMock
      val service = serviceWith(repository, generatedData)

      repository.orderExistsForUser.expects(userId, orderId).returning(IO.pure(true))
      expectGeneratedId(generatedData, fileId.value)
      expectGeneratedNow(generatedData, orderFile.uploaded_at)
      repository.create.expects(userId, orderFile).returning(IO.pure(None))

      service.create(userId, orderId, orderFileCreate).unsafeRunSync() shouldBe
        Left(OrderFileServiceError.OrderNotFound(orderId))
    }

    "delete metadata for an owned order" in {
      val repository = mock[OrderFileRepository[IO]]
      val service = OrderFileService[IO](repository)

      repository.orderExistsForUser.expects(userId, orderId).returning(IO.pure(true))
      repository.delete.expects(userId, orderId, fileId).returning(IO.pure(true))

      service.delete(userId, orderId, fileId).unsafeRunSync() shouldBe Right(())
    }

    "return file not found when deleting missing metadata" in {
      val repository = mock[OrderFileRepository[IO]]
      val service = OrderFileService[IO](repository)

      repository.orderExistsForUser.expects(userId, orderId).returning(IO.pure(true))
      repository.delete.expects(userId, orderId, fileId).returning(IO.pure(false))

      service.delete(userId, orderId, fileId).unsafeRunSync() shouldBe
        Left(OrderFileServiceError.OrderFileNotFound(fileId))
    }
  }

  private val userId = UserId(UUID.fromString("00000000-0000-0000-0000-000000000001"))
  private val orderId = OrderId(UUID.fromString("00000000-0000-0000-0000-000000000010"))
  private val fileId = OrderFileId(UUID.fromString("00000000-0000-0000-0000-000000000100"))

  private def serviceWith(
    repository: OrderFileRepository[IO],
    generatedData: GeneratedData[IO]
  ): OrderFileService[IO] =
    given GeneratedData[IO] = generatedData
    OrderFileService[IO](repository)

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
