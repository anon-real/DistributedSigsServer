package controllers

import akka.actor.ActorSystem
import dao._
import javax.inject._
import models.Forms._
import models._
import play.api.libs.json.JsValue
import play.api.mvc._

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

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
                                transactions: TransactionDAO, proofs: ProofDAO,
                                cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  // TODO needs authentication! proof of knowledge of pk, also pk must be relevant to the request!

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
                |  "success": true
                |}""".stripMargin
            ).as("application/json")
          } else {
            BadRequest(
              s"""{
                 |  "success": false,
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
                 |  "success": false,
                 |  "message": "$err"
                 |}""".stripMargin
            ).as("application/json")
        }
        okRes
      } else {
        Future {
          BadRequest(
            s"""{
               |  "success": false,
               |  "message": "You are not a member of this team!"
               |}""".stripMargin
          ).as("application/json")
        }
      }
    }).flatten
  }

  def newProof(requestId: Long) = Action(parse.json).async { implicit request =>
    val body = request.body
    val proof = (body \ "proof").get.toString()
    val simulated = (body \ "simulated").as[Boolean]
    val memberId = (body \ "memberId").as[Long]
    val alreadySim = proofs.requestSimulated(requestId)
    val memberOk = requests.isMemberPartOf(requestId, memberId)
    val at = for {
      f <- memberOk
      s <- alreadySim
    } yield (f, s)
    at.map(both => {
      val isOk = both._1 && !(both._2 && simulated)
      if (isOk) {
        val okRes = requests.byId(requestId).map(req => {
          if (req.status.get == RequestStatus.approved) {
            proofs.insert(Proof(memberId, requestId, proof, simulated)).recover {
              case e: Throwable => e.printStackTrace()
            }
            Ok(
              """{
                |  "success": true
                |}""".stripMargin
            ).as("application/json")
          } else {
            BadRequest(
              s"""{
                 |  "success": false,
                 |  "message": "request is marked as ${req.status.get}"
                 |}""".stripMargin
            ).as("application/json")
          }
        }).recover {
          case e: Exception =>
            e.printStackTrace()
            val err = e.getMessage.replace("\"", "").replace("\n", "")
            BadRequest(
              s"""{
                 |  "success": false,
                 |  "message": "$err"
                 |}""".stripMargin
            ).as("application/json")
        }
        okRes
      } else {
        Future {
          BadRequest(
            s"""{
               |  "success": false,
               |  "message": "You are not a member of this team!"
               |}""".stripMargin
          ).as("application/json")
        }
      }
    }).flatten
  }

  def setTx(reqId: Long) = Action(parse.json).async { implicit request =>
    val tx: String = (request.body \ "tx").get.toString()
    transactions.insert(Transaction(reqId, tx.getBytes("utf-16"))).map(_ => {
      Ok(
        """{
          |  "success": true
          |}""".stripMargin
      ).as("application/json")
    }).recover {
      case e: Exception =>
        val err = e.getMessage.replace("\"", "").replace("\n", "")
        BadRequest(
          s"""{
             |  "success": false,
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
      Ok(
        s"""
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
      Ok(
        s"""{
           |  "team": ${res._1.toJson},
           |  "memberId": ${res._3.filter(_.pk == pk).head.id.get},
           |  "proposals": [$proposals]
           |}""".stripMargin
      ).as("application/json")
    })
  }

  def getUnsignedTx(reqId: Long) = Action.async { implicit request =>
    transactions.byId(reqId).map(tx => {
      Ok(tx.toJson).as("application/json")
    }).recover {
      case e: Exception => NotFound(
        s"""{
           |  "message": "unsigned transaction for this proposal is not set yet.",
           |  "success": false
           |}""".stripMargin).as("application/json")
    }
  }

  def getMembers(teamId: Long) = Action.async { implicit request =>
    teams.getMembers(teamId).map(members => {
      Ok(s"""
          |[${members.map(_.toJson).mkString(",")}]
          |""".stripMargin)
    }).recover {
      case e: Exception =>
        e.printStackTrace()
        val err = e.getMessage.replace("\"", "").replace("\n", "")
        BadRequest(
          s"""{
             |  "success": false,
             |  "message": "$err"
             |}""".stripMargin
        ).as("application/json")
    }
  }

  def getCommitments(reqId: Long) = Action.async { implicit request =>
    val prop = requests.byId(reqId)
    val cmnts = commitments.getRequestCommitments(reqId)
    val aggFut = for {
      f <- prop
      s <- cmnts
    } yield (f, s)
    val res = aggFut.map(both => {
      val reqs = both._2
      teams.getMembers(both._1.teamId).map(members => {
        Ok(s"""
              |[${reqs.map(cmt => cmt.toJson(members.filter(_.id.get == cmt.memberId).head)).mkString(",")}]
              |""".stripMargin)
      })
    }).recover {
      case e: Exception =>
        e.printStackTrace()
        val err = e.getMessage.replace("\"", "").replace("\n", "")
        Future{BadRequest(
          s"""{
             |  "success": false,
             |  "message": "$err"
             |}""".stripMargin
        ).as("application/json")}
    }
    res.flatten
  }

  def getProofs(reqId: Long) = Action.async { implicit request =>
    proofs.getRequestProofs(reqId).map(proofs => {
      Ok(s"""
            |[${proofs.map(_.toJson).mkString(",")}]
            |""".stripMargin).as("application/json")

  }).recover {
      case e: Exception =>
        e.printStackTrace()
        val err = e.getMessage.replace("\"", "").replace("\n", "")
        BadRequest(
          s"""{
             |  "success": false,
             |  "message": "$err"
             |}""".stripMargin
        ).as("application/json")
    }
  }

  def getApprovedProposals(teamId: Long) = Action.async { implicit request =>
    requests.teamProposals(teamId, Seq(RequestStatus.approved)).map(props => {
      Ok(s"""
            |[${props.map(_.toJson()).mkString(",")}]
            |""".stripMargin)
    }).recover {
      case e: Exception =>
        e.printStackTrace()
        val err = e.getMessage.replace("\"", "").replace("\n", "")
        BadRequest(
          s"""{
             |  "success": false,
             |  "message": "$err"
             |}""".stripMargin
        ).as("application/json")
    }
  }

  def proposalDecision(reqId: Long) = Action(parse.json).async { implicit request =>
    val approved = (request.body \\ "decision").head.as[Boolean]
    val res = requests.byId(reqId).map(proposal => {
      if (proposal.status.get == RequestStatus.pendingApproval) {
        val status = if (approved) RequestStatus.approved else RequestStatus.rejected
        proposal.status = Some(status)
        requests.updateById(proposal).map(_ => {
          Ok("{}").as("application/json")

        }).recover {
          case e: Exception => BadRequest(
            s"""{
               |  "message": "${e.getMessage}"
               |}""".stripMargin).as("application/json")
        }
      } else {
        Future {
          BadRequest(
            s"""{
               |  "message": "proposal is already in ${proposal.status.get} status!",
               |  "success": false
               |}""".stripMargin).as("application/json"
          )
        }
      }
    }).recover {
      case e: Exception => Future{BadRequest(
        s"""{
           |  "message": "${e.getMessage}",
           |  "success": false
           |}""".stripMargin).as("application/json")}
    }
    res.flatten
  }

  def proposalSetPaid(requestId: Long) = Action(parse.json).async { implicit request =>
    val txId = (request.body \\ "txId").head.as[String]
    val res = requests.byId(requestId).map(proposal => {
      if (proposal.status.get == RequestStatus.approved) {
        proposal.status = Some(RequestStatus.paid)
        proposal.txId = Some(txId)
        requests.updateById(proposal).map(_ => {
          Ok("{}").as("application/json")

        }).recover {
          case e: Exception => BadRequest(
            s"""{
               |  "message": "${e.getMessage}"
               |}""".stripMargin).as("application/json")
        }
      } else {
        Future {
          BadRequest(
            s"""{
               |  "message": "proposal is in ${proposal.status.get} status!",
               |  "success": false
               |}""".stripMargin).as("application/json"
          )
        }
      }
    }).recover {
      case e: Exception => Future{BadRequest(
        s"""{
           |  "message": "${e.getMessage}",
           |  "success": false
           |}""".stripMargin).as("application/json")}
    }
    res.flatten
  }

  def test(reqId: Long) = Action.async { implicit request =>
    teams.getMembers(reqId).map(res => {
      val lst = res.map(rs => rs.toJson).mkString(",")
      Ok(
        s"""[
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
