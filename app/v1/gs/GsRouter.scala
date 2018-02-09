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
    case POST(p"/init" ? q_o"obsId=$maybeObsId" & q"axis=$axisStr") =>
      controller.init(maybeObsId.map(ObsId(_)), axisStr(0))
      
    // home axis
    case POST(p"/home" ? q_o"obsId=$maybeObsId" & q"axis=$axisStr") =>
      controller.home(maybeObsId.map(ObsId(_)), axisStr(0))
      
    // motor off an axis
     case POST(p"/motorOff" ? q_o"obsId=$maybeObsId" & q"axis=$axisStr") =>
      controller.motorOff(maybeObsId.map(ObsId(_)), axisStr(0))
     
    // position an axis relative
    case POST(p"/positionRelative" ? q_o"obsId=$maybeObsId" & q"axis=$axisStr" & q"count=$countStr") =>
      controller.positionRelative(maybeObsId.map(ObsId(_)), axisStr(0), countStr.toInt)
      
    // position an axis absolute
     case POST(p"/positionAbsolute" ? q_o"obsId=$maybeObsId" & q"axis=$axisStr" & q"count=$countStr") =>
      controller.positionAbsolute(maybeObsId.map(ObsId(_)), axisStr(0), countStr.toInt)
     
    // get a config
     case GET(p"/getConfig" ? q"axis=$axisStr") =>
       controller.getConfig(axisStr(0))
     
    // create or update a config
     case POST(p"/setConfig" ? q"axis=$axisStr") =>
       controller.setConfig(axisStr(0))

    // Example: http://localhost:9000/v1/gs/setRelTarget?axis=A&count=2
    case POST(p"/setRelTarget" ? q_o"obsId=$maybeObsId" & q"axis=$axisStr" & q"count=$countStr") =>
      controller.setRelTarget(maybeObsId.map(ObsId(_)), axisStr(0), countStr.toInt)

    // Example: http://localhost:9000/v1/gs/getRelTarget?axis=A
    case GET(p"/getRelTarget" ? q_o"obsId=$maybeObsId" & q"axis=$axisStr") =>
      controller.getRelTarget(maybeObsId.map(ObsId(_)), axisStr(0))
  }
}
