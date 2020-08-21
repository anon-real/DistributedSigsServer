package dao

import javax.inject.{Inject, Singleton}
import models.Commitment
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait CommitmentComponent {
  self: HasDatabaseConfigProvider[JdbcProfile] =>

  import profile.api._

  class CommitmentTable(tag: Tag) extends Table[Commitment](tag, "COMMITMENT") {
    def memberId = column[Long]("MEMBER_ID")

    def a = column[String]("A")

    def requestId = column[Long]("REQUEST_ID")

    def * = (memberId, a, requestId) <> (Commitment.tupled, Commitment.unapply)
  }

}

@Singleton()
class CommitmentDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends CommitmentComponent with MemberComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val commitments = TableQuery[CommitmentTable]
  val members = TableQuery[MemberTable]

  /**
   * @param reqId proposal id
   * @return list of commitments of the proposal
   */
  def getRequestCommitments(reqId: Long): Future[Seq[Commitment]] = db.run(commitments.filter(req => req.requestId === reqId).result)

  /**
   * @param reqId proposal id
   * @return list of public keys who sent commitments (approval or rejections)
   */
  def getRequestPks(reqId: Long): Future[Seq[String]] = {
    val query = for {
      c <- commitments
      m <- members if m.id === c.memberId && c.requestId === reqId
    } yield m.public_key
    db.run(query.result)
  }

  /**
   * insert a commitment into db
   * @param commitment commitment
   */
  def insert(commitment: Commitment): Future[Unit] = {
    db.run(commitments += commitment).map(_ => ())
  }

  def all(): Future[Seq[Commitment]] = db.run(commitments.result)
}