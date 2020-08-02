package dao

import javax.inject.{Inject, Singleton}
import models.Commitment
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait CommitmentComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class CommitmentTable(tag: Tag) extends Table[Commitment](tag, "COMMITMENT") {
    def pk = column[String]("PK")
    def a = column[String]("A")
    def requestId = column[Long]("REQUEST_ID")
    def * = (pk, a, requestId) <> (Commitment.tupled, Commitment.unapply)
  }
}

@Singleton()
class CommitmentDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends CommitmentComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val commitments = TableQuery[CommitmentTable]

  def insert(commitment: Commitment): Future[Unit] = db.run(commitments += commitment).map(_ => ())

  def all(): Future[Seq[Commitment]] = db.run(commitments.result)
}