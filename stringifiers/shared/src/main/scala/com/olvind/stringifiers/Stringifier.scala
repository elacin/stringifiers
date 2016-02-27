package com.olvind.stringifiers

import java.net.URI
import java.util.UUID

import scala.language.implicitConversions
import scala.reflect.ClassTag

trait Stringifier[E] {
  /* Meant as a rendering hint */
  val format: Format

  /* The name of type E, as determined by Class.simpleName */
  val typename: Typename

  /* If E is some sort of Enum type, we can provide
      better rendering and error messages by populating this */
  val restrictedValues: Option[Set[E]]

  /* It's often unwanted to include a value of type <: `Throwable`
      in data objects (like `DecodeFail`), so we let users specify
      which part of the exception data they want to see propagate */
  val throwableFormatter: ThrowableFormatter

  def encode(e: E): String

  def decode(str: String): Either[DecodeFail, E]
}

final class StringifierOps[E](val S: Stringifier[E]) extends AnyVal {
  def xmap[F: ClassTag](to: E ⇒ F)(from: F ⇒ E, throwableFormatter: ThrowableFormatter = S.throwableFormatter): Stringifier[F] =
    new StringifierConverting[E, F](typeName[F], throwableFormatter, S, toTryF(to), from)

  def restricted(es: Set[E]): Stringifier[E] =
    new StringifierRestricted[E](S, es)

  def optional: Stringifier[Option[E]] =
    new StringifierOption(S)
}

object Stringifier {
  def apply[E: Stringifier]: Stringifier[E] =
    implicitly

  def encode[E: Stringifier](e: E): String =
    apply[E] encode e

  def decode[E: Stringifier](s: String): Either[DecodeFail, E] =
    apply[E] decode s

  def instance[E: ClassTag](decode:    String ⇒ E)
                           (encode:    E      ⇒ String,
                            format:    Format             = Format.Text,
                            formatter: ThrowableFormatter = ThrowableFormatter.ClassAndMessage): Stringifier[E] =
    new StringifierSimple[E](typeName[E], format, formatter, toTryF(decode), encode)

  def derive[E: Stringifier, F: ClassTag](to: E ⇒ F)(from: F ⇒ E): Stringifier[F] =
    (apply[E] xmap to)(from)

  implicit def optionStringifier[E: Stringifier]: Stringifier[Option[E]] =
    new StringifierOption(implicitly)

  implicit def stringifierOps[E](c: Stringifier[E]): StringifierOps[E] =
    new StringifierOps(c)

  implicit val SUnit    = instance[Unit   ](_ => ()        )(_ => "()",  Format.Unit,    ThrowableFormatter.ClassAndMessage)
  implicit val SByte    = instance[Byte   ](_.toByte       )(_.toString, Format.Int,     ThrowableFormatter.ClassAndMessage)
  implicit val SBoolean = instance[Boolean](_.toBoolean    )(_.toString, Format.Boolean, ThrowableFormatter.ClassAndMessage)
  implicit val SChar    = instance[Char]   (_.apply(0)     )(_.toString, Format.Text,    ThrowableFormatter.ClassAndMessage)
  implicit val SFloat   = instance[Float]  (_.toFloat      )(_.toString, Format.Float,   ThrowableFormatter.ClassAndMessage)
  implicit val SDouble  = instance[Double] (_.toDouble     )(_.toString, Format.Float,   ThrowableFormatter.ClassAndMessage)
  implicit val SInt     = instance[Int]    (_.toInt        )(_.toString, Format.Int,     ThrowableFormatter.ClassAndMessage)
  implicit val SLong    = instance[Long]   (_.toLong       )(_.toString, Format.Int,     ThrowableFormatter.ClassAndMessage)
  implicit val SString  = instance[String] (nonEmpty       )(identity,   Format.Text,    ThrowableFormatter.ClassAndMessage)
  implicit val SUUID    = instance[UUID]   (UUID.fromString)(_.toString, Format.Uuid,    ThrowableFormatter.ClassAndMessage)
  implicit val SURI     = instance[URI]    (URI.create     )(_.toString, Format.Uri,     ThrowableFormatter.ClassAndMessage)
}