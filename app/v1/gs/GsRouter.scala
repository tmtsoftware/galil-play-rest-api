package v1.gs

import javax.inject.Inject

import csw.messages.params.models.ObsId
import play.api.routing.Router.Routes
import play.api.routing.SimpleRouter
import play.api.routing.sird._

/**
  * Routes and URLs.
  */
class GsRouter @Inject()(controller: GsController) extends SimpleRouter {
  val prefix = "/v1/gs"

  override def routes: Routes = {
      
    // initialize axis
    case POST(p"/init") =>
      controller.process()
      
    // home axis
    case POST(p"/home") =>
      controller.process()
      
    // motor off an axis
     case POST(p"/motorOff") =>
      controller.process()
     
    // position an axis relative
    case POST(p"/positionRelative") =>
      controller.process()
      
    // position an axis absolute
     case POST(p"/positionAbsolute") =>
      controller.process()
     
            
      
    // Example: http://localhost:9000/v1/gs/setRelTarget?axis=A&count=2
    case POST(p"/setRelTarget" ? q_o"obsId=$maybeObsId" & q"axis=$axisStr" & q"count=$countStr") =>
      controller.setRelTarget(maybeObsId.map(ObsId(_)), axisStr(0), countStr.toInt)

    // Example: http://localhost:9000/v1/gs/getRelTarget?axis=A
    case GET(p"/getRelTarget" ? q_o"obsId=$maybeObsId" & q"axis=$axisStr") =>
      controller.getRelTarget(maybeObsId.map(ObsId(_)), axisStr(0))
 
      
      
      
  }
}
