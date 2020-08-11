package dao

import javax.inject.{Inject, Singleton}
import models.Member
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait MemberComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class MemberTable(tag: Tag) extends Table[Member](tag, "MEMBER") {
    def id = column[Long]("ID", O.PrimaryKey, O.AutoInc)
    def public_key = column[String]("PUBLIC_KEY")
    def nick_name = column[String]("NICK_NAME")
    def teamId = column[Long]("TEAM_ID")
    def * = (public_key, nick_name, teamId, id.?) <> (Member.tupled, Member.unapply)
  }
}

@Singleton()
class MemberDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends MemberComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val members = TableQuery[MemberTable]

  def insert(member: Member): Future[Unit] = db.run(members += member).map(_ => ())

  def byId(memberId: Long): Future[Member] = db.run(members.filter(mem => mem.id === memberId).result.head)

  def byTeamAndPk(teamId: Long, pk: String): Future[Member] = db.run(members.filter(mem => mem.teamId === teamId && mem.public_key === pk).result.head)

  def all(): Future[Seq[Member]] = db.run(members.result)
}