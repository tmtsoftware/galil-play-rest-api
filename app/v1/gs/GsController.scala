package v1.gs

import java.net.InetAddress

import javax.inject.Inject
import akka.actor.ActorSystem
import csw.messages.params.models.{ObsId, Prefix}
import csw.proto.galil.client.GalilHcdClient
import csw.services.location.commons.ClusterAwareSettings
import csw.services.logging.scaladsl.LoggingSystemFactory
import play.api.libs.json.Json
import play.api.mvc._
import csw.services.location.scaladsl.LocationServiceFactory
import csw.services.config.api.scaladsl.ConfigService
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.config.api.models.ConfigData
import java.nio.file.{Path, Paths}

import scala.concurrent.{ExecutionContext, Future}
import com.typesafe.config.ConfigRenderOptions
import akka.stream.ActorMaterializer

import scala.async.Async._

/**
  * Takes HTTP requests and produces JSON.
  */
class GsController @Inject()(cc: GsControllerComponents)(implicit ec: ExecutionContext)
  extends GsBaseController(cc) {

  implicit val system: ActorSystem = ClusterAwareSettings.system
  private val locationService = LocationServiceFactory.withSystem(system)
  private val host = InetAddress.getLocalHost.getHostName
  implicit val mat: ActorMaterializer = ActorMaterializer()
  private val prefix = Prefix("test.galil.server")
  private val galilHcdClient = GalilHcdClient(prefix, system, locationService)
  private val adminApi: ConfigService = ConfigClientFactory.adminApi(system, locationService)

  LoggingSystemFactory.start("GalilHcdClientApp", "0.1", host, system)

  def init(obsId: Option[ObsId], axis: Char): Action[AnyContent] = GsAction.async { implicit request =>
    (for {
      resp1 <- galilHcdClient.setBrushlessAxis(obsId, axis)
      resp2 <- galilHcdClient.setAnalogFeedbackSelect(obsId, axis, 6)
      resp3 <- galilHcdClient.setBrushlessModulus(obsId, axis, 52000)
      resp4 <- galilHcdClient.brushlessZero(obsId, axis, 1.0)
    } yield {
      Ok(Json.toJson(s"setBrushlessAxis $resp1\nsetAnalogFeedbackSelect $resp2\nsetBrushlessModulus $resp3\nbrushlessZero $resp4"))
    }).recover {
      case ex => InternalServerError(ex.getMessage)
    }
  }

  def home(obsId: Option[ObsId], axis: Char): Action[AnyContent] = GsAction.async { implicit request =>
    (for {
      resp1 <- galilHcdClient.setHomingMode(obsId, axis)
      resp2 <- galilHcdClient.setJogSpeed(obsId, axis, 166)
      resp3 <- galilHcdClient.beginMotion(obsId, axis)
    } yield {
      Ok(Json.toJson(s"setHomingMode $resp1\nsetJogSpeed $resp2\nbeginMotion $resp3"))
    }).recover {
      case ex => InternalServerError(ex.getMessage)
    }
  }

  def motorOff(obsId: Option[ObsId], axis: Char): Action[AnyContent] = GsAction.async { implicit request =>
    galilHcdClient.motorOff(obsId, axis).map { response =>
      Ok(Json.toJson(s"\nmotorOff $response"))
    }.recover {
      case ex => InternalServerError(ex.getMessage)
    }
  }

  def positionRelative(obsId: Option[ObsId], axis: Char, counts: Int): Action[AnyContent] = GsAction.async { implicit request =>
    (for {
      resp1 <- galilHcdClient.setRelTarget(obsId, axis, counts)
      resp2 <- galilHcdClient.beginMotion(obsId, axis)
    } yield {
      Ok(Json.toJson(s"setRelTarget $resp1\nbeginMotion $resp2"))
    }).recover {
      case ex => InternalServerError(ex.getMessage)
    }
  }

  def positionAbsolute(obsId: Option[ObsId], axis: Char, counts: Int): Action[AnyContent] = GsAction.async { implicit request =>
    (for {
      resp1 <- galilHcdClient.setAbsTarget(obsId, axis, counts)
      resp2 <- galilHcdClient.beginMotion(obsId, axis)
    } yield {
      Ok(Json.toJson(s"setAbsTarget $resp1\nbeginMotion $resp2"))
    }).recover {
      case ex => InternalServerError(ex.getMessage)
    }
  }

  // Configuration Service API
  // (Note: This could also be done using the config service command line or HTTP APIs)
  private def getConfigPath(axis: Char): Path = {
    Paths.get("/tmt/aps/ics/galil/" + axis + "/hcd.conf")
  }

  def getConfig(axis: Char): Action[AnyContent] = GsAction.async { implicit request =>
    val filePath = getConfigPath(axis)
    (for {
      activeFile <- adminApi.getActive(filePath)
      config <- activeFile.get.toConfigObject
    } yield {
      Ok(Json.parse(config.root().render(ConfigRenderOptions.concise())))
    }).recover {
      case ex => InternalServerError(ex.getMessage)
    }
  }

  def setConfig(axis: Char): Action[AnyContent] = GsAction.async { implicit request =>
    val data = request.body.asJson.get.toString()
    doUpdateConfig(axis, data).map { response =>
      Ok(Json.toJson(response))
    }.recover {
      case ex => BadRequest(ex.getMessage)
    }
  }

  private def doUpdateConfig(axis: Char, input: String): Future[String] = async {
    val filePath = getConfigPath(axis)
    val exists = await(adminApi.exists(filePath))
    if (exists) {
      // Note: Using for comp to work around compiler warning
      // "a pure expression does nothing in statement position; multiline expressions might require enclosing parentheses"
      await(for {
        _ <- adminApi.update(filePath, ConfigData.fromString(input), comment = "updated")
        _ <- adminApi.resetActiveVersion(filePath, "latest active")
      } yield "updated")
    } else {
      await(for {
        _ <- adminApi.create(filePath, ConfigData.fromString(input), annex = false, "First commit")
      } yield "created")
    }
  }

  //  // read current values and update inputs (XXX not used)
  //  def readAndMerge(filePath: Path, input: String): String = {
  //    // Read
  //    val activeFile: Option[ConfigData] = Await.result(adminApi.getActive(filePath), 3.seconds)
  //    val configData1 = activeFile.getOrElse(throw new Exception("no result found"))
  //    val currentConfig = Await.result(configData1.toConfigObject, 3.seconds)
  //    val inputConfigData = ConfigData.fromString(input)
  //    val inputConfig = Await.result(inputConfigData.toConfigObject, 3.seconds)
  //
  //    println("currentConfig = " + currentConfig.root().render(ConfigRenderOptions.concise()))
  //
  //    val key = "props.InterpolationCounts"
  //    val updatedConfig = currentConfig.withValue(key, inputConfig.getValue(key))
  //
  //    println("updatedConfig = " + updatedConfig.root().render(ConfigRenderOptions.concise()))
  //
  //    inputConfig.withFallback(currentConfig).root().render(ConfigRenderOptions.concise())
  //  }


  def setRelTarget(obsId: Option[ObsId], axis: Char, count: Int): Action[AnyContent] = GsAction.async { implicit request =>
    galilHcdClient.setRelTarget(obsId, axis, count).map { response =>
      Ok(Json.toJson(response.toString)) // XXX Temp: Need JSON I/O for CommandResponse
    }
  }

  def getRelTarget(obsId: Option[ObsId], axis: Char): Action[AnyContent] = GsAction.async { implicit request =>
    galilHcdClient.getRelTarget(obsId, axis).map { response =>
      Ok(Json.toJson(response.toString)) // XXX Temp: Need JSON I/O for CommandResponse
    }
  }
}
