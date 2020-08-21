package dao

import javax.inject.{Inject, Singleton}

import scala.concurrent.{ExecutionContext, Future}
import models.{Member, Team}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
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
  extends TeamComponent with MemberComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val teams = TableQuery[TeamTable]
  val members = TableQuery[MemberTable]

  val insertQuery = teams returning teams.map(_.id) into ((_, id) => id)

  /**
   * inserts a team into db
   * @param team team
   */
  def insert(team: Team) : Future[Long] = {
    val action = insertQuery += team
    db.run(action)
  }

  /**
   * @param id team id
   * @return team
   */
  def byId(id: Long): Future[Team] = db.run(teams.filter(team => team.id === id).result.head)

  /**
   * @param id team id
   * @return list of members of the team
   */
  def getMembers(id: Long): Future[Seq[Member]] = db.run(members.filter(member => member.teamId === id).result)

  /**
   * @param pk public key of the member
   * @return list of teams of the member with pk
   */
  def getForAMember(pk: String): Future[Seq[Team]] = {
    val query = for {
      t <- teams
      m <- members if m.teamId === t.id && m.public_key === pk
    } yield t
    db.run(query.result)
  }

  /**
   * searches for team contining a string in their name
   * @param par pattern
   * @return list of teams
   */
  def search(par: String): Future[Seq[Team]] = db.run(teams.filter(team => team.name.toLowerCase like par.toLowerCase).result)

  def all(): Future[Seq[Team]] = db.run(teams.result)
}