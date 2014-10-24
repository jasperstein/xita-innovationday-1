package com.example

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success
import akka.actor.Actor
import akka.actor.ActorRef
import akka.event.Logging
import akka.util.Timeout
import spray.http._
import spray.httpx.SprayJsonSupport._
import spray.routing._
import spray.routing.directives.LogEntry
import akka.actor.Props
import com.example.util.DirectiveExtensions
import com.example.analytics.AnalyticsActor._
import com.example.analytics.Analytics

object ECommerceActor {
  //def props(cartHandlerProps: Props) = Props(new ECommerceActor(cartHandlerProps))
   def props() = Props(new ECommerceActor())
}
// we don't implement our route structure directly in the service actor because
// we want to be able to test it independently, without having to spin up an actor
class ECommerceActor extends Actor with ECommerceRoute with ClusterCartManagerActorProvider {
//class ECommerceActor(val cartHandlerProps: Props) extends Actor with ECommerceRoute with ClusterCartManagerActorProvider {

  def actorRefFactory = context
  //override val cartHandler = context.actorOf(cartHandlerProps, "cart-manager")
  def receive = runRoute(myRoute)

  override val analyticsActor: ActorRef = Analytics(context.system)
}

// this trait defines our service behavior independently from the service actor
trait ECommerceRoute extends HttpService with StaticResources with Api with AnalyticsRoutes {
  val cartHandler: ActorRef
  val analyticsActor: ActorRef

  def showPath(req: HttpRequest) = req.method match {
    case HttpMethods.POST => LogEntry("Method = %s, Path = %s, Data = %s" format (req.method, req.uri, req.entity), Logging.InfoLevel)
    case _ => LogEntry("Method = %s, Path = %s" format (req.method, req.uri), Logging.InfoLevel)
  }

  val myRoute =
    logRequest(showPath _) {
      shoppingCartRoutes ~ analyticsRoutes ~ staticResources
    }
}

//REST Api
trait Api extends HttpService with DirectiveExtensions{
  import akka.pattern.ask
  import ExecutionContext.Implicits.global
  import CartMessages._
  import RequestMessages._
  import OrderMessages._
  implicit val timeout = Timeout(20 seconds)
  
  val cartHandler: ActorRef

  //custom directive to retrieve cookie
  val sessionId:Directive1[String] = getOrCreateSessionCookie("session-id").flatMap {
    case c:HttpCookie => provide(c.content)
  }
  val shoppingCartRoutes =
    pathPrefix("cart") {
        (post & sessionId) {sessionId  =>  
          entity(as[AddToCartRequest]) { addMsg =>
            handleCartRequest(Envelope(sessionId, addMsg))
          }
        } ~
          delete {
            (parameter('itemId) & sessionId) {(itemId, sessionId) =>
              handleCartRequest(Envelope(sessionId, RemoveFromCartRequest(itemId)))
            }
          } ~
          (get & sessionId) { sessionId => 
            handleCartRequest(Envelope(sessionId, GetCartRequest))
          }
    } ~ path("order") {
        (put & sessionId) { sessionId => 
          handleOrderRequest(sessionId)
        }
    }
  private def handleCartRequest[T](reqCtx: Envelope[T]) = {
    val respFuture = cartHandler.ask(reqCtx).mapTo[Seq[ShoppingCartItem]]
    onComplete(respFuture) {
      case Success(res) => complete(res)
      case Failure(e) => completeWithError(e)
    }
  }

  private def handleOrderRequest(sessionId: String) = {
    val processingStateFuture = cartHandler.ask(Envelope(sessionId, OrderRequest))
    onComplete(processingStateFuture) {
      case Success(resp) => resp match {
        case ok @ OrderProcessed(orderId) => complete(OrderStateResponse(ok.getClass.getSimpleName(), Some(orderId)))
        case nok => complete(OrderStateResponse(nok.toString))
      }
      case Failure(e) => completeWithError(e)
    }
  }
  private def completeWithError(e: Throwable) = complete(StatusCodes.InternalServerError, e.getMessage())

}

trait AnalyticsRoutes extends HttpService with DirectiveExtensions {
  import akka.pattern.ask
  import ExecutionContext.Implicits.global

  val analyticsActor: ActorRef

  val analyticsRoutes = {
    path("last10items") {
      get {
        val last10Items = (analyticsActor.ask(LastAdded)(10 seconds)).mapTo[LastItems]
        onComplete(last10Items) {
          case Success(resLast10Items)    => complete(resLast10Items)
          case Failure(e)                 => complete(StatusCodes.InternalServerError, e.getMessage())
        }
      }
    }
  }

}

// Trait for serving static resources
// Sends 404 for 'favicon.icon' requests and serves static resources in 'bootstrap' folder.
trait StaticResources extends HttpService {

  val staticResources =
    get {
      path("") {
        redirect("/index.html", StatusCodes.PermanentRedirect)
      } ~
        path("favicon.ico") {
          complete(StatusCodes.NotFound)
        } ~
        path(Rest) { path =>
          getFromResource("root/%s" format path)
        }
    }
}