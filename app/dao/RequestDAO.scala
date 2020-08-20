package dao

import javax.inject.{Inject, Singleton}

import scala.concurrent.{Await, ExecutionContext, Future}
import models.{Commitment, Request}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

trait RequestComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class RequestTable(tag: Tag) extends Table[Request](tag, "REQUEST") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def title = column[String]("TITLE")
    def confirmedTxId = column[String]("CONFIRMED_TX_ID")
    def amount = column[Double]("AMOUNT")
    def description = column[String]("DESCRIPTION")
    def address = column[String]("ADDRESS")
    def teamId = column[Long]("TEAM_ID")
    def status = column[String]("STATUS")
    def * = (title, amount, description, address, teamId, confirmedTxId.?, status.?, id.?) <> (Request.tupled, Request.unapply)
  }
}

@Singleton()
class RequestDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends RequestComponent with MemberComponent with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val requests = TableQuery[RequestTable]
  val members = TableQuery[MemberTable]

  def insert(request: Request): Future[Unit] = db.run(requests += request).map(_ => ())

  def byId(id: Long): Future[Request] = db.run(requests.filter(req => req.id === id).result.head)

  def updateById(prop: Request): Future[Unit] = db.run(requests.filter(_.id === prop.id).update(prop)).map(_ => ())

  def isMemberPartOf(reqId: Long, memberId: Long): Future[Boolean] = {
    val f = db.run(requests.filter(req => req.id === reqId).result.head)
    val s = db.run(members.filter(mem => mem.id === memberId).result.head)
    val aggFut = for {
      f <- f
      s <- s
    } yield (f, s)
    aggFut.map(res => res._1.teamId == res._2.teamId)
  }

  def teamProposals(teamId: Long): Future[Seq[Request]] = db.run(requests.filter(_.teamId === teamId).result)

  def teamProposals(teamId: Long, statuses: Seq[String]): Future[Seq[Request]] = db.run(requests.filter(req => (req.teamId === teamId) && (req.status inSetBind statuses)).result)

  def all(): Future[Seq[Request]] = db.run(requests.result)
}