package controllers

import javax.inject._
import akka.actor.ActorSystem
import dao.TeamDAO
import play.api.mvc._
import play.api.data._
import models.Forms._

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
class AsyncController @Inject()(teams: TeamDAO, cc: ControllerComponents, actorSystem: ActorSystem)(implicit exec: ExecutionContext) extends AbstractController(cc) {

  def createTeamFrom = Action.async { implicit request =>
    getFutureMessage(0.second).map { msg =>
      Ok(views.html.create_team())
    }
  }

  def createTeam = Action(parse.form(teamForm)).async { implicit request =>
    teams.insert(request.body).map(_ => {
      Redirect(routes.AsyncController.teamList(request.body.name))
    })
  }

  def teamList(name: String) = Action.async { implicit request =>
    if (name.isBlank)
      teams.all().map(teams => Ok(views.html.team_list(teams)))
    else
      teams.search("%" + name + "%").map(teams => Ok(views.html.team_list(teams)))
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
