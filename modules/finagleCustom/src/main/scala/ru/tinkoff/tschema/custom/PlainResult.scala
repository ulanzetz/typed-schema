package ru.tinkoff.tschema.custom
import cats.syntax.applicative._
import cats.syntax.functor._
import cats.{Applicative, Functor}
import ru.tinkoff.tschema.finagle.{Complete, LiftHttp}
import ru.tinkoff.tschema.swagger.{MkSwagger, SwaggerPrimitive}
import ru.tinkoff.tschema.typeDSL
class PlainResult[A]

object PlainResult {
  implicit def plainComplete[F[_]: Applicative, A: AsResponse.Plain]: Complete[F, PlainResult[A], A] =
    a => AsResponse.plain(a).pure[F]

  implicit def plainFComplete[F[_], G[_]: Functor, A: AsResponse.Plain](
      implicit lift: LiftHttp[F, G]
  ): Complete[F, PlainResult[A], G[A]] = gs => lift(gs.map(AsResponse.plain[A]))

  implicit def plainSwagger[R]: MkSwagger[typeDSL.Complete[PlainResult[R]]] =
    MkSwagger
      .summon[typeDSL.Complete[String]]
      .addDescribedResponse(200, SwaggerPrimitive.string.withMediaType("text/plain"))
      .as[typeDSL.Complete[PlainResult[R]]]
}