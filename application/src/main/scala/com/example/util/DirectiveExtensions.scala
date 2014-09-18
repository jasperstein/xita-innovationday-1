package com.example.util

import spray.routing._
import spray.http.HttpCookie
import java.util.UUID

trait DirectiveExtensions extends Directives {

  import shapeless._
  def liftToDirective1[T](f: Directive0, value: T): Directive1[T] = new Directive1[T] {
    def happly(inner: T :: HNil ⇒ Route) = f(inner(value :: HNil))
  }

  def getOrCreateSessionCookie(name: String): Directive1[HttpCookie] = optionalCookie(name).flatMap {
    case Some(c) => provide(c)
    case None => {
      val sessionCookie = HttpCookie(name, UUID.randomUUID().toString())
      liftToDirective1(setCookie(sessionCookie), sessionCookie)
    }
  }
}