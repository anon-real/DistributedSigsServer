package dao

import javax.inject.{Inject, Singleton}
import models.{Commitment, Member}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait MemberComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class MemberTable(tag: Tag) extends Table[Member](tag, "MEMBER") {
    def pk = column[String]("PK")
    def teamId = column[Long]("TEAM_ID")
    def * = (pk, teamId) <> (Member.tupled, Member.unapply)
  }
}

@Singleton()
class MemberDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends MemberComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val members = TableQuery[MemberTable]

  def insert(member: Member): Future[Unit] = db.run(members += member).map(_ => ())

  def byId(pk: String, teamId: Long): Future[Member] = db.run(members.filter(mem => mem.pk === pk && mem.teamId === teamId).result.head)

  def all(): Future[Seq[Member]] = db.run(members.result)
}