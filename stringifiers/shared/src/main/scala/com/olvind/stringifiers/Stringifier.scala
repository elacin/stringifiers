package com.olvind.stringifiers

import java.net.URI
import java.util.UUID

import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util.{Try, Failure, Success}

final case class Stringifier[E](
  decodeT:            String => Try[E],
  encode:             E => String,
  typename:           Typename,
  renderHint:         RenderHint,
  /* It's often unwanted to include a value of type <: `Throwable`
      in data objects (like `DecodeFail`), so we let users specify
      which part of the exception data they want to keep */
  throwableFormatter: ThrowableFormatter,
  /* If E is some sort of Enum type, we can provide
      better rendering and error messages by populating this */
  enumValuesOpt:      Option[Set[E]]) {

  def decode(str: String): Either[DecodeFail, E] =
    decodeT(str) match {
      case Success(e)  =>
        enumValuesOpt match {
          case Some(enumValues) if !enumValues(e) =>
            Left(ValueNotInSet(str, typename, enumValues map encode))
          case _ =>
            Right(e)
        }
      case Failure(th) =>
        Left(ValueNotValid(str, typename, throwableFormatter(th)))
    }

  def withFormat(f: RenderHint): Stringifier[E] =
    copy(renderHint = f)

  def withThrowableFormatter(tf: ThrowableFormatter): Stringifier[E] =
    copy(throwableFormatter = tf)

  def withTypename(t: Typename): Stringifier[E] =
    copy(typename = t)

  def withEnumValues(vs: Set[E]) =
    copy(enumValuesOpt = Some(vs))
}

final class StringifierOps[E](val S: Stringifier[E]) extends AnyVal {
  def xmap[F: ClassTag](to: E ⇒ F)(from: F ⇒ E): Stringifier[F] =
    Stringifier.xmap[E, F](to)(from)(S, implicitly)

  def optional: Stringifier[Option[E]] =
    Stringifier toOpt S
}

object Stringifier {
  /* summon an instance of `Stringifier[E]` */
  def apply[E: Stringifier]: Stringifier[E] =
    implicitly

  def encode[E: Stringifier](e: E): String =
    apply[E] encode e

  def decode[E: Stringifier](s: String): Either[DecodeFail, E] =
    apply[E] decode s

  /**
    * Define an instance of a `Stringifier` from scratch
    */
  def instance[E: ClassTag](
    decode:        String ⇒ E)
   (encode:        E      ⇒ String,
    hint:          RenderHint         = RenderHint.Text,
    formatter:     ThrowableFormatter = ThrowableFormatter.ClassAndMessage,
    enumValuesOpt: Option[Set[E]]     = None): Stringifier[E] =

    Stringifier[E](toTryF(decode), encode, Typename[E], hint, formatter, None)

  /**
    * Derive an instance of a `Stringifier` for `F` based on an
    *  existing instance for `E`
    */
  def xmap[E: Stringifier, F: ClassTag](to: E ⇒ F)(from: F ⇒ E): Stringifier[F] = {
    val S = implicitly[Stringifier[E]]

    new Stringifier[F](
      str => S decodeT str flatMap toTryF(to),
      from andThen S.encode,
      Typename[F],
      S.renderHint,
      S.throwableFormatter,
      S.enumValuesOpt map (_ map toTryF(to) collect { case Success(s) => s })
    )
  }

  /**
    * Derive an instance of a `Stringifier[Option[E]]` based on an
    *  existing instance of `Stringifier[E]`
    */
  def toOpt[E](S: Stringifier[E]): Stringifier[Option[E]] = {
    val decodeOpt: String => Try[Option[E]] =
      str =>
        Option(str) filter (_.nonEmpty) match {
          case Some(value) => S.decodeT(value) map Some.apply
          case _           => Success(None)
        }

    new Stringifier[Option[E]](
      decodeOpt,
      oe => oe map S.encode getOrElse "",
      S.typename,
      S.renderHint,
      S.throwableFormatter,
      S.enumValuesOpt map (_ map Some.apply)
    )
  }

  /* automatically upgrade to `Stringifier[E]` to `Stringifier[Option[E]]` */
  implicit def optionStringifier[E: Stringifier]: Stringifier[Option[E]] =
    toOpt(implicitly)

  /* Provide syntax for `xmap` */
  implicit def stringifierOps[E](c: Stringifier[E]): StringifierOps[E] =
    new StringifierOps(c)

  /**
    * Built-in instances
    */
  implicit val SUnit    = instance[Unit   ](_ => ()        )(_ => "()",  RenderHint.Unit,    ThrowableFormatter.ClassAndMessage)
  implicit val SByte    = instance[Byte   ](_.toByte       )(_.toString, RenderHint.Int,     ThrowableFormatter.ClassAndMessage)
  implicit val SBoolean = instance[Boolean](_.toBoolean    )(_.toString, RenderHint.Boolean, ThrowableFormatter.ClassAndMessage)
  implicit val SChar    = instance[Char]   (_.apply(0)     )(_.toString, RenderHint.Text,    ThrowableFormatter.ClassAndMessage)
  implicit val SFloat   = instance[Float]  (_.toFloat      )(_.toString, RenderHint.Float,   ThrowableFormatter.ClassAndMessage)
  implicit val SDouble  = instance[Double] (_.toDouble     )(_.toString, RenderHint.Float,   ThrowableFormatter.ClassAndMessage)
  implicit val SInt     = instance[Int]    (_.toInt        )(_.toString, RenderHint.Int,     ThrowableFormatter.ClassAndMessage)
  implicit val SLong    = instance[Long]   (_.toLong       )(_.toString, RenderHint.Int,     ThrowableFormatter.ClassAndMessage)
  implicit val SString  = instance[String] (nonEmpty       )(identity,   RenderHint.Text,    ThrowableFormatter.ClassAndMessage)
  implicit val SUUID    = instance[UUID]   (UUID.fromString)(_.toString, RenderHint.Uuid,    ThrowableFormatter.ClassAndMessage)
  implicit val SURI     = instance[URI]    (URI.create     )(_.toString, RenderHint.Uri,     ThrowableFormatter.ClassAndMessage)
}