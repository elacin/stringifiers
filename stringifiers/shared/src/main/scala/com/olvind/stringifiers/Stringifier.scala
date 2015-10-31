package com.olvind.stringifiers

import java.net.URI
import java.util.UUID

import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

sealed trait Stringifier[E] {
  val format: Format
  val typename: String
  def encode(e: E): String
  def decode(str: String): Either[Failed, E]
}

private final class SimpleStringifier[E: ClassTag](
               rawDecode: String => E,
               rawEncode: E => String,
  override val format:    Format) extends Stringifier[E] {

  override val typename = implicitly[ClassTag[E]].runtimeClass.getSimpleName

  def encode(e: E) = rawEncode(e)

  def decode(str: String): Either[Failed, E] = {
    val trimmed = str.trim
    Try(rawDecode(trimmed)) match {
      case Success(e)  => Right(e)
      case Failure(th) => Left(FailedParsing(str, typename, th))
    }
  }
}

private final class RestrictedStringifier[E](
  E:          Stringifier[E],
  restricted: Set[E]) extends Stringifier[E] {
  override val format   = Format.Enum
  override val typename = E.typename

  val restrictedValues =
    (restricted foldLeft Map.empty[String, E]){
      case (map, e) => map + (E.encode(e) -> e)
    }

  val restrictedStrings = restricted map E.encode

  override def decode(str: String) =
    restrictedValues get str toRight FailedNotInSet(str, typename, restrictedStrings)

  override def encode(e: E): String =
    E encode e
}

private final class OptionStringifier[E](E: Stringifier[E]) extends Stringifier[Option[E]] {
  override val format   = E.format
  override val typename = s"Option[${E.typename}"

  override def decode(str: String) =
    Some(str.trim) filter (_.nonEmpty) match {
      case Some(value) => (E decode value).right map Some.apply
      case _           => Right(None)
    }

  override def encode(oe: Option[E]): String =
    oe map E.encode getOrElse ""
}

private final class ConvertingStringifier[E, F: ClassTag](
  E: Stringifier[E], to: E => F, from: F => E) extends Stringifier[F] {

  override val format   = E.format
  override val typename = implicitly[ClassTag[F]].runtimeClass.getSimpleName

  override def decode(str: String) =
    (E decode str).right flatMap {
      e => Try(to(e)) match {
        case Success(f)  => Right(f)
        case Failure(th) => Left(FailedParsing(str, typename, th))
      }
    }

  override def encode(f: F): String =
    E encode from(f)
}


final class StringifierOps[E](val E: Stringifier[E]) extends AnyVal {
  def xmap[F: ClassTag](to: E ⇒ F)(from: F ⇒ E): Stringifier[F] =
    new ConvertingStringifier[E, F](E, to, from)

  def restricted(es: Set[E]): Stringifier[E] =
    new RestrictedStringifier[E](E, es)

  def optional: Stringifier[Option[E]] =
    new OptionStringifier(E)
}

object Stringifier{
  val nonEmpty: String ⇒ String = _.ensuring(_.nonEmpty)

  def encode[E](e: E)(implicit E: Stringifier[E]): String =
    E.encode(e)

  def decode[E](s: String)(implicit E: Stringifier[E]): Either[Failed, E] =
    E.decode(s)

  def apply[E: ClassTag](_decode:  String ⇒ E, format: Format = Format.Text)
                        (_encode:  E      ⇒ String): Stringifier[E] =
    new SimpleStringifier[E](_decode, _encode, format)

  def apply[E, F: ClassTag](to: E ⇒ F)(from: F ⇒ E)(implicit E: Stringifier[E]): Stringifier[F] =
    new ConvertingStringifier[E, F](E, to, from)

  implicit def optionStringifier[E](implicit E: Stringifier[E]): Stringifier[Option[E]] =
    new OptionStringifier(E)

  implicit def stringifierOps[E](c: Stringifier[E]): StringifierOps[E] =
    new StringifierOps(c)

  implicit val SUnit    = apply[Unit   ](_ => (),         Format.Unit)   (_ => "()")
  implicit val SByte    = apply[Byte   ](_.toByte,        Format.Int)    (_.toString)
  implicit val SBoolean = apply[Boolean](_.toBoolean,     Format.Boolean)(_.toString)
  implicit val SChar    = apply[Char]   (_.apply(0),      Format.Text)   (_.toString)
  implicit val SFloat   = apply[Float]  (_.toFloat,       Format.Float)  (_.toString)
  implicit val SDouble  = apply[Double] (_.toDouble,      Format.Float)  (_.toString)
  implicit val SInt     = apply[Int]    (_.toInt,         Format.Int)    (_.toString)
  implicit val SLong    = apply[Long]   (_.toLong,        Format.Int)    (_.toString)
  implicit val SString  = apply[String] (nonEmpty,        Format.Text)   (identity)
  implicit val SUUID    = apply[UUID]   (UUID.fromString, Format.Uuid)   (_.toString)
  implicit val SURI     = apply[URI]    (URI.create,      Format.Uri)    (_.toString)
}