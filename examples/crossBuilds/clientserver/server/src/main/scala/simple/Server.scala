package simple

import akka.actor.ActorSystem
import spray.http.{HttpEntity, MediaTypes}
import spray.routing.SimpleRoutingApp

object Server extends SimpleRoutingApp{
  def main(args: Array[String]): Unit = {
    implicit val system = ActorSystem()
    startServer("localhost", port = 8080){
      get{
        pathSingleSlash{
          complete{
            HttpEntity(
              MediaTypes.`text/html`,
              Page.skeleton.render
            )
          }
        } ~
        getFromResourceDirectory("")
      } ~
      post{
        path("ajax" / "list"){
          extract(_.request.entity.asString) { e =>
            complete {
              upickle.write(list(e))
            }
          }
        }
      }
    }
  }
  def list(path: String) = {
    val (dir, last) = path.splitAt(path.lastIndexOf("/") + 1)
    val files =
      Option(new java.io.File("./" + dir).listFiles())
        .toSeq.flatten
    for{
      f <- files
      if f.getName.startsWith(last)
    } yield FileData(f.getName, f.length())
  }
}