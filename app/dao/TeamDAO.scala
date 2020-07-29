package dao

import javax.inject.{ Inject, Singleton }

import scala.concurrent.{ ExecutionContext, Future }
import models.Team
import play.api.db.slick.{ DatabaseConfigProvider, HasDatabaseConfigProvider }
import slick.jdbc.JdbcProfile

trait TeamComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class TeamTable(tag: Tag) extends Table[Team](tag, "TEAM") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def description = column[String]("DESCRIPTION")
    def address = column[String]("ADDRESS")
    def * = (name, description, address, id.?) <> (Team.tupled, Team.unapply)
  }
}

@Singleton()
class TeamDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends TeamComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val teams = TableQuery[TeamTable]

  def insert(team: Team): Future[Unit] = db.run(teams += team).map(_ => ())

  def all(): Future[Seq[Team]] = db.run(teams.result)

}