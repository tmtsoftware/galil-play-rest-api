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
import play.api.{Logger, MarkerContext}
import csw.messages.ccs.commands.CommandResponse.Error
import csw.messages.ccs.commands.CommandResponse

import csw.services.location.scaladsl.{LocationService, LocationServiceFactory}
import csw.services.config.api.scaladsl.{ConfigClientService, ConfigService}
import csw.services.config.client.scaladsl.ConfigClientFactory
import csw.services.config.server.ServerWiring
import csw.services.config.api.models.{ConfigData, ConfigId, ConfigMetadata, FileType}
import java.nio.file.{Path, Paths}


import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.Await

import scala.async.Async._

import csw.services.config.client.internal.ActorRuntime

import com.typesafe.config.ConfigRenderOptions


import akka.stream.ActorMaterializer
import play.api.data.Form
import com.typesafe.config.Config


case class AxisCountFormInput(axis: String, count: Int, analogFeedbackSelect: Int, brushlessModulus: Int, brushlessZeroVolts: Double, 
    homingJogSpeed: Int, axisType: String, homingMethod: String, axisLimitHigh: Int, axisLimitLow: Int)


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
  
  private val logger = Logger(this.getClass)
  
  private val locationService = LocationServiceFactory.withSettings(ClusterAwareSettings.onPort(3552))
  
  implicit val ActorSystem = system
  

  
  private val adminApi: ConfigService = ConfigClientFactory.adminApi(system, locationService)
  
  
  LoggingSystemFactory.start("GalilHcdClientApp", "0.1", host, system)

  
  
  private val form: Form[AxisCountFormInput] = {
    import play.api.data.Forms._
    import play.api.data.format.Formats._

    Form(
      mapping(
        "axis" -> nonEmptyText,
        "count" -> number, 
        "analogFeedbackSelect" -> number,
        "brushlessModulus" -> number,
        "brushlessZeroVolts" -> of[Double], 
        "homingJogSpeed" -> number,
        "axisType" -> nonEmptyText, 
        "homingMethod" -> nonEmptyText, 
        "axisLimitHigh" -> number,
        "axisLimitLow" -> number
      )(AxisCountFormInput.apply)(AxisCountFormInput.unapply)
    )
  }

  def process: Action[AnyContent] = {
    GsAction.async { implicit request =>
      processJsonPost()
    }
  }
  
  // TODO: axisType (servo or stepper) is not yet used.  Current code only supports servo.

  private def processJsonPost[A]()(implicit request: GsRequest[A]):  Future[Result] = {
    def failure(badForm: Form[AxisCountFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: AxisCountFormInput) = {
            
      request.target.path match  {
        
        case "/v1/gs/positionAbsolute/" => doPositionAbsolute(Some(ObsId("123")), input.axis(0), input.count).map { response => {
           Ok(Json.toJson(response)) }
        }
        
        case "/v1/gs/positionRelative/" => doPositionRelative(Some(ObsId("123")), input.axis(0), input.count).map { response => Ok(Json.toJson(response)) }
      
        case "/v1/gs/motorOff/" => doMotorOff(Some(ObsId("123")), input.axis(0)).map { response => Ok(Json.toJson(response)) }
      
        case "/v1/gs/init/" => doInit(Some(ObsId("123")), input.axis(0), input.analogFeedbackSelect, input.brushlessModulus, input.brushlessZeroVolts).map { response => {
          
          Ok(Json.toJson(response)) }
        }
      
        case "/v1/gs/home/" => doHome(Some(ObsId("123")), input.axis(0), input.homingJogSpeed, input.homingMethod).map { response => Ok(Json.toJson(response)) }
      
        
      }
      
    }

    form.bindFromRequest().fold(failure, success)
  }
  
  
  
  
  def doInit(obsId: Option[ObsId], axis: Char, analogFeedbackSelect: Int, brushlessModulus: Int, brushlessZeroVolts: Double): Future[String] = {
  
    println("DO INIT")
    val config = getConfigSync(axis)
    println(s"config = $config")
   
    Future {
      
      val output = new StringBuilder()
      
      try {
    
        val resp1 = Await.result(galilHcdClient.setBrushlessAxis(obsId, axis), 3.seconds)
        
        if (resp1.isInstanceOf[Error]) throw new Exception(s"setBushelessAxis $resp1") else output.append(s"\nsetBrushlessAxis $resp1, ")
        
        Thread.sleep(1000) 
        val resp2 = Await.result(galilHcdClient.setAnalogFeedbackSelect(obsId, axis, analogFeedbackSelect), 3.seconds)
        
        if (resp2.isInstanceOf[Error]) throw new Exception(s"setAnalogFeedbackSelect $resp2") else output.append(s"\nsetAnalogFeedbackSelect($analogFeedbackSelect) $resp2, ")
        
        Thread.sleep(1000) 
        val resp3 = Await.result(galilHcdClient.setBrushlessModulus(obsId, axis, brushlessModulus), 3.seconds)
       
        if (resp3.isInstanceOf[Error]) throw new Exception(s"setBrushlessModulus $resp3") else output.append(s"\nsetBrushlessModulus($brushlessModulus) $resp3, ")
     
        Thread.sleep(1000) 
        val resp4 = Await.result(galilHcdClient.brushlessZero(obsId, axis, brushlessZeroVolts), 3.seconds)
        
        if (resp4.isInstanceOf[Error]) throw new Exception(s"brushlessZero $resp4") else output.append(s"\nbrushlessZero($brushlessZeroVolts) $resp4")
        
        output.toString()
  
      } catch {
        
        case e: Exception => e.getMessage()
        
      }
    
    }
    
  }
  
  // TODO: homingMethod is currently only coded for full homing sequence, not index only
  // passed 'homingMethod' should be used to branch to correct homing sequence
  def doHome(obsId: Option[ObsId], axis: Char, homingJogSpeed: Int, homingMethod: String): Future[String] = {
  
   
    Future {
    
      val output = new StringBuilder()
      
      try {

        val resp1 = Await.result(galilHcdClient.setHomingMode(obsId, axis), 3.seconds)
      
        if (resp1.isInstanceOf[Error]) throw new Exception(s"setHomingMode $resp1") else output.append(s"\nsetHomingMode $resp1, ")

        
        val resp2 = Await.result(galilHcdClient.setJogSpeed(obsId, axis, homingJogSpeed), 3.seconds)
        
        if (resp2.isInstanceOf[Error]) throw new Exception(s"setJogSpeed $resp2") else output.append(s"\nsetJogSpeed($homingJogSpeed) $resp2, ")

      
        val resp3 = Await.result(galilHcdClient.beginMotion(obsId, axis), 3.seconds)
        
        if (resp3.isInstanceOf[Error]) throw new Exception(s"beginMotion $resp3") else output.append(s"\nbeginMotion $resp3, ")

      
        output.toString()
  
      } catch {
        
        case e: Exception => e.getMessage()
        
      }
      
    }
    
  }
  
  def doMotorOff(obsId: Option[ObsId], axis: Char): Future[String] = {
 
    Future {
      
      val output = new StringBuilder()
      
      try {

      
        val resp1 = Await.result(galilHcdClient.motorOff(obsId, axis), 3.seconds)
        
        if (resp1.isInstanceOf[Error]) throw new Exception(s"motorOff $resp1") else output.append(s"\nmotorOff $resp1, ")
      
        output.toString()
  
      } catch {
        
        case e: Exception => e.getMessage()
        
      }
      
    }
  }
  
  def doPositionRelative(obsId: Option[ObsId], axis: Char, counts: Int): Future[String] = {
 
    Future {
    
      val output = new StringBuilder()
      
      try {

      
        val resp1 = Await.result(galilHcdClient.setRelTarget(obsId, axis, counts), 3.seconds)
      
        if (resp1.isInstanceOf[Error]) throw new Exception(s"setRelTarget $resp1") else output.append(s"\nsetRelTarget $resp1, ")
        
        val resp2 = Await.result(galilHcdClient.beginMotion(obsId, axis), 3.seconds)
        
        if (resp2.isInstanceOf[Error]) throw new Exception(s"beginMotion $resp2") else output.append(s"\nbeginMotion $resp2, ")

      
        output.toString()
  
      } catch {
        
        case e: Exception => e.getMessage()
        
      }
    }
  }
  
  def doPositionAbsolute(obsId: Option[ObsId], axis: Char, counts: Int): Future[String] = {
 
    Future {
    
      val output = new StringBuilder()
      
      try {

        val resp1 = Await.result(galilHcdClient.setAbsTarget(obsId, axis, counts), 3.seconds)
      
        if (resp1.isInstanceOf[Error]) throw new Exception(s"setAbsTarget $resp1") else output.append(s"\nsetAbsTarget $resp1, ")

        val resp2 = Await.result(galilHcdClient.beginMotion(obsId, axis), 3.seconds)
        
        if (resp2.isInstanceOf[Error]) throw new Exception(s"beginMotion $resp2") else output.append(s"\nbeginMotion $resp2, ")

      
        output.toString()
  
      } catch {
        
        case e: Exception => e.getMessage()
        
      }
      
      
    }
  }
  
  //*******************************************************//
  // Configuration Service API                             //
  //*******************************************************//
  
  def getConfig(axis: Char): Action[AnyContent] = GsAction.async { implicit request =>
    doGetConfig(axis).map { response => {
     
      if (response.startsWith("Futures")) {
        Ok("")
      
      } else {
      
        Ok(Json.parse(response)) 
      }
    }
      
    }
  }
  
  
  
  
 
          
 
  
  def doGetConfig(axis: Char): Future[String] = {
      
    logger.debug("CONFIG")
    
    implicit val mat: ActorMaterializer = ActorMaterializer();
    
    Future {
      // construct the path
      val filePath = Paths.get("/tmt/aps/ics/galil/" + axis + "/hcd.conf")

      try {
        

        
        val activeFile: Option[ConfigData] = Await.result(adminApi.getActive(filePath), 3.seconds)
        
        val configData1 = activeFile.getOrElse(throw new Exception("no result found"))
        
        val config = Await.result(configData1.toConfigObject, 3.seconds)
        
        logger.debug(s"config = $config")
        
        config.root().render( ConfigRenderOptions.concise() )
        
 
      } catch {
        case e: Exception => e.getMessage()
      }
    }
  }
  
  def getConfigSync(axis: Char): Config = {
          
      implicit val mat: ActorMaterializer = ActorMaterializer();
      val filePath = Paths.get("/tmt/aps/ics/galil/" + axis + "/hcd.conf")

        
      val activeFile: Option[ConfigData] = Await.result(adminApi.getActive(filePath), 3.seconds)
        
      val configData1 = activeFile.getOrElse(throw new Exception("no result found"))
        
      Await.result(configData1.toConfigObject, 3.seconds)
                
  }
  
  
  case class ConfigFormInput(data: String)

  private val configForm: Form[ConfigFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "data" -> nonEmptyText
      )(ConfigFormInput.apply)(ConfigFormInput.unapply)
    )
  }

  def setConfig(axis: Char): Action[AnyContent] = {
    logger.info(s"setConfig::$axis")
    
    GsAction.async { implicit request =>
      processUpdateConfig(axis)
    }
  }

  private def processUpdateConfig[A](axis: Char)(implicit request: GsRequest[A]):  Future[Result] = {
    def failure(badForm: Form[ConfigFormInput]) = {
      Future.successful(BadRequest(badForm.errorsAsJson))
    }

    def success(input: ConfigFormInput) = {
            
      doUpdateConfig(axis, input.data).map { response => Ok(Json.toJson(response)) }
      
    }

    configForm.bindFromRequest().fold(failure, success)
  }
  
    def doUpdateConfig(axis: Char, input: String): Future[String] = {
      
    Future {
      // construct the path
      val filePath = Paths.get("/tmt/aps/ics/galil/" + axis + "/hcd.conf")

      try {
        
        logger.info("doUpdateConfig")
        
        val exists: Boolean = Await.result(adminApi.exists(filePath), 3.seconds)
        
        if (exists) {
                   
            Await.result(adminApi.update(filePath, ConfigData.fromString(input), comment = "updated"), 3.seconds)
            Await.result(adminApi.resetActiveVersion(filePath, "latest active"), 3.seconds)
            "updated"
        } else {
            Await.result(adminApi.create(filePath, ConfigData.fromString(input), annex = false, "First commit"), 3.seconds)
            "created"
        }
 
      } catch {
        
        case e: Exception => {
          logger.error(s"exception: ${e.getMessage()}")
          e.getMessage()
        }
      }
    }
  }
  
  // read current values and update inputs
  def readAndMerge(filePath: Path, input: String): String = {
    
    implicit val mat: ActorMaterializer = ActorMaterializer();
           
    // Read
    val activeFile: Option[ConfigData] = Await.result(adminApi.getActive(filePath), 3.seconds)
    
    val configData1 = activeFile.getOrElse(throw new Exception("no result found"))
    
    val currentConfig = Await.result(configData1.toConfigObject, 3.seconds)
           
    val inputConfigData = ConfigData.fromString(input)
    val inputConfig = Await.result(inputConfigData.toConfigObject, 3.seconds)
       
    println("currentConfig = " + currentConfig.root().render( ConfigRenderOptions.concise()))
    
    val key = "props.InterpolationCounts"
    val updatedConfig = currentConfig.withValue(key, inputConfig.getValue(key)) 
    
    println("updatedConfig = " + updatedConfig.root().render( ConfigRenderOptions.concise()))
    
    inputConfig.withFallback(currentConfig).root().render( ConfigRenderOptions.concise() )
    
    
  }
  
  
  
  
  
  
  
  def setRelTarget(obsId: Option[ObsId], axis: Char, count: Int): Action[AnyContent] = GsAction.async  { implicit request =>
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
