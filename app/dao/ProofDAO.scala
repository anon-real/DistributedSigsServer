package dao

import javax.inject.{Inject, Singleton}
import models.Proof
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

trait ProofComponent { self: HasDatabaseConfigProvider[JdbcProfile] =>
  import profile.api._

  class ProofTable(tag: Tag) extends Table[Proof](tag, "PROOF") {
    def memberId = column[Long]("MEMBER_ID")
    def requestId = column[Long]("REQUEST_ID")
    def proof = column[String]("PROOF")
    def simulated = column[Boolean]("CONTAINS_SIMULATION")
    def * = (memberId, requestId, proof, simulated) <> (Proof.tupled, Proof.unapply)
  }
}

@Singleton()
class ProofDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext)
  extends ProofComponent
    with HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  val proofs = TableQuery[ProofTable]

  def getRequestProofs(reqId: Long): Future[Seq[Proof]] = db.run(proofs.filter(req => req.requestId === reqId).result)

  def requestSimulated(reqId: Long): Future[Boolean] = db.run(proofs.filter(req => req.requestId === reqId && req.simulated === true).exists.result)

  def insert(proof: Proof): Future[Unit] = {
    db.run(proofs += proof).map(_ => ())
  }

  def all(): Future[Seq[Proof]] = db.run(proofs.result)
}