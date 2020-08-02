package dao

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import models.Request
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

trait RequestComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class RequestTable(tag: Tag) extends Table[Request](tag, "REQUEST") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def title = column[String]("TITLE")
    def amount = column[Long]("AMOUNT")
    def description = column[String]("DESCRIPTION")
    def address = column[String]("ADDRESS")
    def teamId = column[Long]("TEAM_ID")
    def status = column[String]("STATUS")
    def * = (title, amount, description, address, teamId, status.?, id.?) <> (Request.tupled, Request.unapply)
  }
}

@Singleton()
class RequestDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends RequestComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val requests = TableQuery[RequestTable]

  def insert(request: Request): Future[Unit] = db.run(requests += request).map(_ => ())

  def byId(id: Long): Future[Request] = db.run(requests.filter(req => req.id === id).result.head)

  def teamProposals(teamId: Long): Future[Seq[Request]] = db.run(requests.filter(_.teamId === teamId).result)

  def all(): Future[Seq[Request]] = db.run(requests.result)
}