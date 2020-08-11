package dao

import javax.inject.{Inject, Singleton}
import models.Transaction
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait TransactionComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class TransactionTable(tag: Tag) extends Table[Transaction](tag, "TRANSACTION") {
    def isPartial = column[Boolean]("IS_PARTIAL")
    def isConfirmed = column[Boolean]("IS_CONFIRMED")
    def isValid = column[Boolean]("IS_VALID")
    def requestId = column[Long]("REQUEST_ID")
    def memberId = column[Long]("MEMBER_ID")
    def txBytes = column[Array[Byte]]("TX_BYTES")
    def * = (requestId, isPartial, txBytes, isValid, isConfirmed, memberId) <> (Transaction.tupled, Transaction.unapply)
  }
}

@Singleton()
class TransactionDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends TransactionComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val transactions = TableQuery[TransactionTable]

  def insert(transaction: Transaction): Future[Unit] = db.run(transactions += transaction).map(_ => ())

  def byId(reqId: Long): Future[Seq[Transaction]] = db.run(transactions.filter(tx => tx.requestId === reqId).result)

  def all(): Future[Seq[Transaction]] = db.run(transactions.result)
}