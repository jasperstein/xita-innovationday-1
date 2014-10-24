package com.example.analytics

import akka.actor.{Props, Actor}
import java.util.UUID
import akka.contrib.pattern.DistributedPubSubExtension
import akka.contrib.pattern.DistributedPubSubMediator.{SubscribeAck, Subscribe}
import AnalyticsActor._
import com.example.PersistentCartActor.ItemAddedEvent
import spray.json.DefaultJsonProtocol

object AnalyticsActor {
  val topic = "cart-event"
  def name = "analytics-actor"
  def props = Props[AnalyticsActor]
  case object LastAdded
  
  case class LastItems(items: Seq[String])
  object LastItems extends DefaultJsonProtocol {
    implicit val format = jsonFormat1(LastItems.apply)
  }
}


class AnalyticsActor extends Actor {
  val uuid = UUID.randomUUID()

  val mediator = DistributedPubSubExtension(context.system).mediator
  val maxLastAddedItems = 10
  var lastAddedItems = Seq.empty[String]

  override def preStart() = mediator ! Subscribe(topic, None, self)

  override def receive: Receive = {
    case sa@SubscribeAck(_) => println(s"subscribed: '$uuid' - $sa")
    case msg@ItemAddedEvent(id) => println("iae" + msg) ; updateLastAdded(id)
    case msg@LastAdded => println("lastadded" + sender() + msg) ; sender ! LastItems(lastAddedItems)
    case m@_ => println(s"analytics actor '$uuid' - message unhandled: $m")
  }

  private def updateLastAdded(id: String) = {
    lastAddedItems = id +: lastAddedItems
    if (lastAddedItems.size > maxLastAddedItems) {
      lastAddedItems = lastAddedItems.init
    }
  }
}
