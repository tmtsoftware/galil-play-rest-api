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
import csw.messages.ccs.commands.CommandResponse.Error
import csw.messages.ccs.commands.CommandResponse

import scala.concurrent.ExecutionContext
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.Await


import play.api.data.Form


case class AxisCountFormInput(axis: String, count: Int)


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

  
  
  private val form: Form[AxisCountFormInput] = {
    import play.api.data.Forms._

    Form(
      mapping(
        "axis" -> nonEmptyText,
        "count" -> number
      )(AxisCountFormInput.apply)(AxisCountFormInput.unapply)
    )
  }

  def process: Action[AnyContent] = {
    GsAction.async { implicit request =>
      processJsonPost()
    }
  }

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
      
        case "/v1/gs/init/" => doInit(Some(ObsId("123")), input.axis(0)).map { response => {
          
          Ok(Json.toJson(response)) }
        }
      
        case "/v1/gs/home/" => doHome(Some(ObsId("123")), input.axis(0)).map { response => Ok(Json.toJson(response)) }
      
        
      }
      
      

    }

    form.bindFromRequest().fold(failure, success)
  }
  
  
  
  
 

  
  
  def doInit(obsId: Option[ObsId], axis: Char): Future[String] = {
  
   
    Future {
      
      val output = new StringBuilder()
      
      try {
    
        val resp1 = Await.result(galilHcdClient.setBrushlessAxis(obsId, axis), 3.seconds)
        
        if (resp1.isInstanceOf[Error]) throw new Exception(s"setBushelessAxis $resp1") else output.append(s"\nsetBrushlessAxis $resp1, ")
        
        Thread.sleep(1000) 
        val resp2 = Await.result(galilHcdClient.setAnalogFeedbackSelect(obsId, axis, 6), 3.seconds)
        
        if (resp2.isInstanceOf[Error]) throw new Exception(s"setAnalogFeedbackSelect $resp2") else output.append(s"\nsetAnalogFeedbackSelect $resp2, ")
        
        Thread.sleep(1000) 
        val resp3 = Await.result(galilHcdClient.setBrushlessModulus(obsId, axis, 52000), 3.seconds)
       
        if (resp3.isInstanceOf[Error]) throw new Exception(s"setBrushlessModulus $resp3") else output.append(s"\nsetBrushlessModulus $resp3, ")
     
        Thread.sleep(1000) 
        val resp4 = Await.result(galilHcdClient.brushlessZero(obsId, axis, 1.0), 3.seconds)
        
        if (resp4.isInstanceOf[Error]) throw new Exception(s"brushlessZero $resp4") else output.append(s"\nbrushlessZero $resp4")
        
        output.toString()
  
      } catch {
        
        case e: Exception => e.getMessage()
        
      }
    
    }
    
  }
  
  def doHome(obsId: Option[ObsId], axis: Char): Future[String] = {
  
   
    Future {
    
      val output = new StringBuilder()
      
      try {

        val resp1 = Await.result(galilHcdClient.setHomingMode(obsId, axis), 3.seconds)
      
        if (resp1.isInstanceOf[Error]) throw new Exception(s"setHomingMode $resp1") else output.append(s"\nsetHomingMode $resp1, ")

        
        val resp2 = Await.result(galilHcdClient.setJogSpeed(obsId, axis, 166), 3.seconds)
        
        if (resp2.isInstanceOf[Error]) throw new Exception(s"setJogSpeed $resp2") else output.append(s"\nsetJogSpeed $resp2, ")

      
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
