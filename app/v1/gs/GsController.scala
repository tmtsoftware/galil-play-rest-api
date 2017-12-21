package v1.gs

import java.net.InetAddress
import javax.inject.Inject

import akka.actor.ActorSystem
import csw.messages.ccs.commands.CommandName
import csw.messages.params.models.{ObsId, Prefix}
import csw.proto.galil.client.GalilHcdClient
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.scaladsl.LoggingSystemFactory
import play.api.libs.json.Json
import play.api.mvc._

import scala.concurrent.ExecutionContext

case class GsFormInput(title: String, body: String)

/**
  * Takes HTTP requests and produces JSON.
  */
class GsController @Inject()(cc: GsControllerComponents)(implicit ec: ExecutionContext)
  extends GsBaseController(cc) {

  //  private val logger = Logger(getClass)
  private val system: ActorSystem = ClusterAwareSettings.system
  private val host = InetAddress.getLocalHost.getHostName
  private val prefix = Prefix("test.galil.server")
  private val galilHcdClient = GalilHcdClient(prefix, system)

  LoggingSystemFactory.start("GalilHcdClientApp", "0.1", host, system)

  def setRelTarget(obsId: Option[ObsId], axis: Char, count: Int): Action[AnyContent] = GsAction.async { implicit request =>
    galilHcdClient.setRelTarget(obsId, axis, count).map { response =>
      Ok(Json.toJson(response.toString)) // XXX Temp: Need JSON I/O for CommandResponse?
    }
  }

  def getRelTarget(obsId: Option[ObsId], axis: Char): Action[AnyContent] = GsAction.async { implicit request =>
    galilHcdClient.getRelTarget(obsId, axis).map { response =>
      Ok(Json.toJson(response.toString)) // XXX Temp: Need JSON I/O for CommandResponse?
    }
  }
}
