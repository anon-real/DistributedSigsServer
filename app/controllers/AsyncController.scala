package controllers

import akka.actor.ActorSystem
import dao._
import javax.inject._
import models.Forms._
import models._
import play.api.libs.json.{JsValue, Json}
import play.api.mvc._
import helpers.Conf

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future, Promise}

@Singleton
class AsyncController @Inject()(teams: TeamDAO, requests: RequestDAO, commitments: CommitmentDAO, members: MemberDAO,
                                transactions: TransactionDAO, proofs: ProofDAO,
                                cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  // TODO needs authentication! proof of knowledge of pk, also pk must be relevant to the request!

  /**
   * GET endpoint for team form
   */
  def createTeamFrom = Action { implicit request =>
    if (Conf.publicTeamCreation) {
      Ok(views.html.create_team())
    } else {
      BadRequest("Public team creation is not enabled.")
    }
  }

  /**
   * POST endpoint to create team
   */
  def createTeam = Action(parse.json).async { implicit request =>
    if (Conf.publicTeamCreation) {
      val reqMembers = (request.body \\ "members").head.as[List[JsValue]]
      val name = (request.body \\ "name").head.as[String]
      val description = (request.body \\ "description").head.as[String]
      val address = (request.body \\ "address").head.as[String].trim
      val tokenId = (request.body \\ "tokenId").head.as[String].trim
      val assetName = (request.body \\ "assetName").head.as[String]
      val res = teams.insert(Team(name, description, address, assetName, tokenId)).map(id => {
        reqMembers.foreach(mem => {
          val nick = (mem \\ "nick").head.as[String]
          val pk = (mem \\ "pk").head.as[String].trim
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
    } else {
      Future {
        BadRequest("Public team creation is not enabled.")
      }
    }
  }

  /**
   * GET endpoint to get teams containing 'name' in their name field
   *
   * @param name team name
   */
  def teamList(name: String) = Action.async { implicit request =>
    if (name.trim.isEmpty)
      teams.all().map(teams => Ok(views.html.team_list(teams)))
    else
      teams.search("%" + name.trim + "%").map(teams => Ok(views.html.team_list(teams)))
  }

  /**
   * POST endpoint to create a new proposal
   */
  def newRequest() = Action(parse.form(requestForm)).async { implicit request =>
    request.body.status = Some(RequestStatus.pendingApproval)
    requests.insert(request.body).map(_ => {
      Redirect(routes.AsyncController.proposalList(request.body.teamId))
    })
  }

  /**
   * GET endpoint to get the proposal html page for a team
   *
   * @param teamId team id
   */
  def proposalList(teamId: Long) = Action.async { implicit request =>
    val team = teams.byId(teamId)
    val reqs = requests.teamProposals(teamId)
    val aggFut = for {
      f <- team
      s <- reqs
    } yield (f, s)
    aggFut.map(res => Ok(views.html.request_list(res._2, res._1)))
  }

  /**
   * POST endpoint to add a commitment for a proposal
   *
   * @param requestId proposal id
   */
  def newCommitment(requestId: Long) = Action(parse.json).async { implicit request =>
    val body = request.body
    val a = Json.stringify((body \ "a").get)
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

  /**
   * POST endpoint to add a partial proof (or simulation) for a proposal
   *
   * @param requestId proposal id
   */
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

  /**
   * POST endpoint to set unsigned transaction for a proposal
   *
   * @param reqId proposal id
   */
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

  /**
   * GET endpoint for getting info about a pk (teams, pending for each team, ...)
   *
   * @param pk public key of the member
   */
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

  /**
   * GET endpoint for getting list of proposal of a team as json
   *
   * @param teamId team id
   * @param pk     public key
   */
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

  /**
   * GET endpoint for getting unsigned transaction for a proposal if set as json
   *
   * @param reqId proposal id
   */
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

  /**
   * GET endpoint for getting lis of members of a team as json
   *
   * @param teamId team id
   */
  def getMembers(teamId: Long) = Action.async { implicit request =>
    teams.getMembers(teamId).map(members => {
      Ok(
        s"""
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

  /**
   * GET endpoint for getting commitments of a proposal as json
   *
   * @param reqId proposal id
   */
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
        Ok(
          s"""
             |[${reqs.map(cmt => cmt.toJson(members.filter(_.id.get == cmt.memberId).head)).mkString(",")}]
             |""".stripMargin)
      })
    }).recover {
      case e: Exception =>
        e.printStackTrace()
        val err = e.getMessage.replace("\"", "").replace("\n", "")
        Future {
          BadRequest(
            s"""{
               |  "success": false,
               |  "message": "$err"
               |}""".stripMargin
          ).as("application/json")
        }
    }
    res.flatten
  }

  /**
   * GET endpoint for getting proofs of a proposal as json
   *
   * @param reqId proposal id
   */
  def getProofs(reqId: Long) = Action.async { implicit request =>
    proofs.getRequestProofs(reqId).map(proofs => {
      Ok(
        s"""
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

  /**
   * GET endpoint for getting approved proposals of a team as json
   *
   * @param teamId team id
   */
  def getApprovedProposals(teamId: Long) = Action.async { implicit request =>
    requests.teamProposals(teamId, Seq(RequestStatus.approved)).map(props => {
      Ok(
        s"""
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

  /**
   * GET endpoint for getting a proposal by id
   *
   * @param reqId proposal id
   */
  def getProposalById(reqId: Long) = Action.async { implicit request =>
    requests.byId(reqId).map(req => {
      teams.byId(req.teamId).map(team => {
        Ok(
          s"""{
            |  "team": ${team.toJson},
            |  "proposal": ${req.toJson()}
            |}""".stripMargin).as("application/json")
      })
    }).flatten.recover {
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

  /**
   * POST endpoint to make the final decision about a proposal
   * call this if enough approvals/rejections has been collected to make the decision
   *
   * @param reqId proposal id
   */
  def proposalDecision(reqId: Long) = Action(parse.json).async { implicit request =>
    val approved = (request.body \\ "decision").head.as[Boolean]
    val res = requests.byId(reqId).map(proposal => {
      if (proposal.status.get == RequestStatus.pendingApproval) {
        val status = if (approved) RequestStatus.approved else RequestStatus.rejected
        if (!approved) transactions.deleteById(reqId)
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
      case e: Exception => Future {
        BadRequest(
          s"""{
             |  "message": "${e.getMessage}",
             |  "success": false
             |}""".stripMargin).as("application/json")
      }
    }
    res.flatten
  }

  /**
   * POST endpoint to set proposal status as paid and also set the confirmed tx id
   * @param requestId proposal id
   */
  def proposalSetPaid(requestId: Long) = Action(parse.json).async { implicit request =>
    val txId = (request.body \\ "txId").head.as[String]
    val res = requests.byId(requestId).map(proposal => {
      if (proposal.status.get == RequestStatus.approved) {
        proposal.status = Some(RequestStatus.paid)
        proposal.txId = Some(txId)
        transactions.deleteById(requestId)
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
      case e: Exception => Future {
        BadRequest(
          s"""{
             |  "message": "${e.getMessage}",
             |  "success": false
             |}""".stripMargin).as("application/json")
      }
    }
    res.flatten
  }
}
