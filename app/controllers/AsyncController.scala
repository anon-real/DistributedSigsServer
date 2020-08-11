package controllers

import javax.inject._
import akka.actor.ActorSystem
import dao.{CommitmentDAO, MemberDAO, RequestDAO, TeamDAO, TransactionDAO}
import play.api.mvc._
import play.api.data._
import models.Forms._
import models.{Commitment, Member, RequestStatus, Team, Transaction}
import play.api.libs.json.JsValue

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success}

/**
 * This controller creates an `Action` that demonstrates how to write
 * simple asynchronous code in a controller. It uses a timer to
 * asynchronously delay sending a response for 1 second.
 *
 * @param cc          standard controller components
 * @param actorSystem We need the `ActorSystem`'s `Scheduler` to
 *                    run code after a delay.
 * @param exec        We need an `ExecutionContext` to execute our
 *                    asynchronous code.  When rendering content, you should use Play's
 *                    default execution context, which is dependency injected.  If you are
 *                    using blocking operations, such as database or network access, then you should
 *                    use a different custom execution context that has a thread pool configured for
 *                    a blocking API.
 */
@Singleton
class AsyncController @Inject()(teams: TeamDAO, requests: RequestDAO, commitments: CommitmentDAO, members: MemberDAO,
                                transactions: TransactionDAO,
                                cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  def createTeamFrom = Action.async { implicit request =>
    getFutureMessage(0.second).map { msg =>
      Ok(views.html.create_team())
    }
  }

  def createTeam = Action(parse.json).async { implicit request =>
    val reqMembers = (request.body \\ "members").head.as[List[JsValue]]
    val name = (request.body \\ "name").head.as[String]
    val description = (request.body \\ "description").head.as[String]
    val address = (request.body \\ "address").head.as[String]
    val res = teams.insert(Team(name, description, address)).map(id => {
      reqMembers.foreach(mem => {
        val nick = (mem \\ "nick").head.as[String]
        val pk = (mem \\ "pk").head.as[String]
        members.insert(Member(pk, nick, id))
      })
    })
    res.map(_ => Ok(
      s"""{
         |  "redirect": "${routes.AsyncController.teamList(name)}"
         |}""".stripMargin).as("application/json")
    ).recover {
      case e: Exception => BadRequest(
        s"""{
           |  "message": "${e.getMessage}"
           |}""".stripMargin).as("application/json")
    }
  }

  def teamList(name: String) = Action.async { implicit request =>
    if (name.isBlank)
      teams.all().map(teams => Ok(views.html.team_list(teams)))
    else
      teams.search("%" + name + "%").map(teams => Ok(views.html.team_list(teams)))
  }

  def newRequest() = Action(parse.form(requestForm)).async { implicit request =>
    request.body.status = Some(RequestStatus.pendingApproval)
    requests.insert(request.body).map(_ => {
      Redirect(routes.AsyncController.proposalList(request.body.teamId))
    })
  }

  def proposalList(teamId: Long) = Action.async { implicit request =>
    val team = teams.byId(teamId)
    val reqs = requests.teamProposals(teamId)
    val aggFut = for {
      f <- team
      s <- reqs
    } yield (f, s)
    aggFut.map(res => Ok(views.html.request_list(res._2, res._1)))
  }

  def newCommitment(requestId: Long) = Action(parse.json).async { implicit request =>
    val body = request.body
    val a = (body \\ "a").head.as[String]
    val memberId = (body \\ "memberId").head.as[Long]
    val memberOk = requests.isMemberPartOf(requestId, memberId)
    memberOk.map(isOk => {
      if (isOk) {
        val okRes = requests.byId(requestId).map(req => {
          if (req.status.get == RequestStatus.pendingApproval) {
            commitments.insert(Commitment(memberId, a, requestId)).recover {
              case e: Throwable => e.printStackTrace()
            }
            Ok(
              """{
                |  "status": true
                |}""".stripMargin
            ).as("application/json")
          } else {
            BadRequest(
              s"""{
                 |  "status": false,
                 |  "message": "request is already marked as ${req.status.get}"
                 |}""".stripMargin
            ).as("application/json")
          }
        }).recover {
          case e: Exception =>
            e.printStackTrace()
            val err = e.getMessage.replace("\"", "").replace("\n", "")
            BadRequest(
              s"""{
                 |  "status": false,
                 |  "message": "$err"
                 |}""".stripMargin
            ).as("application/json")
        }
        okRes
      } else {
        Future {
          BadRequest(
            s"""{
               |  "status": false,
               |  "message": "You are not a member of this team!"
               |}""".stripMargin
          ).as("application/json")
        }
      }
    }).flatten
  }

  def setTx(reqId: Long) = Action(parse.json).async { implicit request =>
    val isPartial: Boolean = (request.body \\ "isPartial").head.as[Boolean]
    val memberId: Long = (request.body \\ "memberId").head.as[Long]
    val tx: String = (request.body \\ "tx").head.toString()
    transactions.insert(Transaction(reqId, isPartial, tx.getBytes("utf-16"), isValid = false, isConfirmed = false, memberId)).map(_ => {
      Ok(
        """{
          |  "status": true
          |}""".stripMargin
      ).as("application/json")
    }).recover {
      case e: Exception =>
        val err = e.getMessage.replace("\"", "").replace("\n", "")
        BadRequest(
          s"""{
             |  "status": false,
             |  "message": "$err"
             |}""".stripMargin
        ).as("application/json")
    }
  }

  def getInfo(pk: String) = Action.async { implicit request =>
    val memTeams = teams.getForAMember(pk)
    val info = memTeams.map(res => {
      val t = res.map(team => {
        val props = requests.teamProposals(team.id.get, Seq(RequestStatus.pendingApproval))
        val mem = members.byTeamAndPk(team.id.get, pk)
        val at = for {
          f <- props
          s <- mem
        } yield (f, s)
        at.map(both => {
          val reqs = both._1
          s"""{
            |  "team": ${team.toJson},
            |  "memberId": ${both._2.id.get},
            |  "pendingNum": ${reqs.length}
            |}""".stripMargin
        })
      })
      Future.sequence(t)
    })
    info.flatten.map(res => {
      val info = res.mkString(",")
      Ok(s"""
            |[$info]
            |""".stripMargin
      ).as("application/json")

    })
  }

  def getProposals(teamId: Long, pk: String) = Action.async { implicit request =>
    val team = teams.byId(teamId)
    val reqs = requests.teamProposals(teamId)
    val mems = teams.getMembers(teamId)
    val at = for {
      f <- reqs
      s <- mems
    } yield (f, s)
    val finalCmts = at.map(both => {
      val res = both._1
      val withCmts = res.map(req => {
        commitments.getRequestCommitments(req.id.get).map(cmts => {
          val cmtList = cmts.map(cmt => cmt.toJson(both._2.filter(_.id.get == cmt.memberId).head)).mkString(",")
          req.toJson(cmtList)
        })
      })
      Future.sequence(withCmts)
    }).flatten
    val aggFut = for {
      f <- team
      s <- finalCmts
      m <- mems
    } yield (f, s, m)

    aggFut.map(res => {
      val proposals = res._2.mkString(",")
      Ok(s"""{
            |  "team": ${res._1.toJson},
            |  "memberId": ${res._3.filter(_.pk == pk).head.id.get},
            |  "proposals": [$proposals]
            |}""".stripMargin
      ).as("application/json")
    })
  }

  def test(reqId: Long) = Action.async { implicit request =>
    teams.getMembers(reqId).map(res => {
      val lst = res.map(rs => rs.toJson).mkString(",")
      Ok(s"""[
            |$lst
            |]""".stripMargin
      ).as("application/json")
    })
  }

  /**
   * Creates an Action that returns a plain text message after a delay
   * of 1 second.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/message`.
   */
  def message = Action.async {
    getFutureMessage(1.second).map { msg => Ok(msg) }
  }

  private def getFutureMessage(delayTime: FiniteDuration): Future[String] = {
    val promise: Promise[String] = Promise[String]()
    actorSystem.scheduler.scheduleOnce(delayTime) {
      promise.success("Hi!")
    }(actorSystem.dispatcher) // run scheduled tasks using the actor system's dispatcher
    promise.future
  }

}
