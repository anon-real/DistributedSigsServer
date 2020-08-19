package services

import java.time.{Clock, Instant}

import akka.actor.ActorSystem
import javax.inject._
import play.api.Logger
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.inject.ApplicationLifecycle
import slick.jdbc.JdbcProfile
import akka.actor._
import scala.concurrent.duration._

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class StartupService @Inject()(protected val dbConfigProvider: DatabaseConfigProvider, appLifecycle: ApplicationLifecycle,
                               system: ActorSystem)
                              (implicit ec: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  private val logger: Logger = Logger(this.getClass)

  logger.info("App started!")

  appLifecycle.addStopHook { () =>
    logger.info("App stopped!")
    Future.successful(())
  }
}
