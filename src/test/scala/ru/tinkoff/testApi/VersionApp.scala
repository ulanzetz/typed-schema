package ru.tinkoff.testApi

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.{Directive, Rejection}
import akka.stream.ActorMaterializer
import ru.tinkoff.tschema.akkaHttp.{MkRoute, Serve}
import ru.tinkoff.tschema.swagger.{SwaggerMapper, _}
import ru.tinkoff.tschema.syntax._
import ru.tinkoff.tschema.typeDSL.{:>, DSLAtom, Key, Prefix}
import shapeless.{HList, Witness}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport._

object VersionApp extends App {
  def api = prefix('service) |> (
    (version('v1) |> get[String]) <>
    (version('v2) |> get[Map[String, Int]]) <>
    (version('v3) |> get[Vector[String]])
    )

  object service {
    def v1 = "Ololo"
    def v2 = Map("Olol" -> 0)
    def v3 = Vector("Olo", "lo")
  }

  implicit val system = ActorSystem()
  implicit val mat = ActorMaterializer()

  import akka.http.scaladsl.server.Directives._
  import system.dispatcher

  val route = pathPrefix("api") { MkRoute(api)(service) } ~
              pathPrefix("swagger")(get(
                complete(api.mkSwagger.make(OpenApiInfo()))
              ))

  for (_ <- Http().bindAndHandle(route, "localhost", 8080))
    println("server started at http://localhost:8080")
}

final class version[v] extends DSLAtom

object version {

  import akka.http.scaladsl.server.Directives._

  case class WrongVersionRejection(shouldBe: String, passed: String) extends Rejection

  def apply[v <: Symbol](v: Witness.Lt[v]): version[v] :> Key[v] = new version[v] :> key(v)

  implicit def versionServe[v <: Symbol, In <: HList](implicit w: Witness.Aux[v]): Serve.Aux[version[v], In, In] = Serve.serveCheck {
    Directive { f =>
      parameter("version") { v =>
        if (v == w.value.name) f(())
        else reject(WrongVersionRejection(w.value.name, v))
      } ~
      pathPrefix(w.value.name) {
        f(())
      }
    }
  }
  implicit def versionSwagger[v <: Symbol](implicit w: Witness.Aux[v]): SwaggerMapper[version[v]] = SwaggerMapper[Prefix[v]].as[version[v]]
}