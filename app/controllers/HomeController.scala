package controllers

import javax.inject.Inject

import play.api.mvc._
import akka.stream.scaladsl._
import akka.NotUsed
import scala.concurrent.duration._

import play.api.libs.json._

/**
  * A very small controller that renders a home page.
  */
class HomeController @Inject()(cc: ControllerComponents) extends AbstractController(cc) {

  def index = Action { implicit request =>
    Ok(views.html.index())
  }
  
  def ws = WebSocket.accept[String, String] { request =>

    // Log events to the console
    val in = Sink.foreach[String](println)
    
    val out = Source.tick(1.second, 1.second, NotUsed).map(_ => buildMessage())

    Flow.fromSinkAndSource(in, out)

  }
  
  case class AxisInfo(axis:String, status:String, position:Int, positionError:Int)
  case class AxesInfo(axes: Seq[AxisInfo])
  
  implicit val axisInfoWrites = new Writes[AxisInfo] {
    def writes(axisInfo: AxisInfo) = Json.obj(
      "axis" -> axisInfo.axis,
      "status" -> axisInfo.status,
      "position" -> axisInfo.position,
      "positionError" -> axisInfo.positionError
    )
  }
  
  implicit val axesInfoWrites = new Writes[AxesInfo] {
  def writes(axesInfo: AxesInfo) = Json.obj(
    "axes" -> axesInfo.axes
  )
}
  
  
  
  
  def buildMessage(): String = {
    val v = "Hello"
    
    val start = -2000
    val end   = 3000
    val rnd = new scala.util.Random
 
    
    val axesInfo = AxesInfo(
    Seq(
        AxisInfo("A", "Good", start + rnd.nextInt( (end - start) + 1 ), start + rnd.nextInt( (end - start) + 1 )),
        AxisInfo("B", "Bad", start + rnd.nextInt( (end - start) + 1 ), 0),
        AxisInfo("C", "In Progress", start + rnd.nextInt( (end - start) + 1 ), start + rnd.nextInt( (end - start) + 1 )),
        AxisInfo("D", "Init", start + rnd.nextInt( (end - start) + 1 ), start + rnd.nextInt( (end - start) + 1 )),
        AxisInfo("E", "Good", 400, 0),
        AxisInfo("F", "Good", 600, 453),
        AxisInfo("G", "Good", 12, 21),
        AxisInfo("H", "Good", -987, 12)
      )
    )

   val message = Json.toJson(axesInfo)
   message.toString()
  }
  
  
  
  
  
}


