package dao

import javax.inject.{Inject, Singleton}
import models.Transaction
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait TransactionComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class TransactionTable(tag: Tag) extends Table[Transaction](tag, "UNSIGNEDTRANSACTION") {
    def requestId = column[Long]("REQUEST_ID")
    def txBytes = column[Array[Byte]]("TX_BYTES")
    def * = (requestId, txBytes) <> (Transaction.tupled, Transaction.unapply)
  }
}

@Singleton()
class TransactionDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends TransactionComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val transactions = TableQuery[TransactionTable]

  /**
   * inserts a transaction into db
   * @param transaction transaction
   */
  def insert(transaction: Transaction): Future[Unit] = db.run(transactions += transaction).map(_ => ())

  /**
   * @param reqId proposal id
   * @return the transaction associated with the proposal if set
   */
  def byId(reqId: Long): Future[Transaction] = db.run(transactions.filter(tx => tx.requestId === reqId).result.head)

  def deleteById(reqId: Long): Unit = db.run(transactions.filter(tx  => tx.requestId === reqId).delete)

  def all(): Future[Seq[Transaction]] = db.run(transactions.result)
}