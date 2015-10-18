package com.olvind.stringifiers

import java.util.UUID

import scala.language.implicitConversions
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

sealed trait Stringifier[E] {
  val typename: String
  def encode(e: E): String
  def decode(str: String): Either[Failed, E]
}

private final class SimpleStringifier[E: ClassTag](
  rawDecode: String => E, rawEncode: E => String) extends Stringifier[E] {

  override val typename = implicitly[ClassTag[E]].runtimeClass.getClass.getSimpleName

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
  E: Stringifier[E], restricted: Set[E]) extends Stringifier[E] {

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

  def apply[E: Stringifier]: Stringifier[E] = implicitly

  def instance[E: ClassTag](_decode:  String ⇒ E)
                           (_encode:  E      ⇒ String): Stringifier[E] =
    new SimpleStringifier[E](_decode, _encode)

  def instanceVia[E, F: ClassTag](to: E ⇒ F)(from: F ⇒ E)(implicit E: Stringifier[E]): Stringifier[F] =
    new ConvertingStringifier[E, F](E, to, from)

  implicit def optionStringifier[E](implicit E: Stringifier[E]): Stringifier[Option[E]] =
    new OptionStringifier(E)

  implicit def stringifierOps[E](c: Stringifier[E]): StringifierOps[E] =
    new StringifierOps(c)

  implicit val SUnit    = instance[Unit   ](_ => ())        (_ => "()")
  implicit val SByte    = instance[Byte   ](_.toByte)       (_.toString)
  implicit val SBoolean = instance[Boolean](_.toBoolean)    (_.toString)
  implicit val SChar    = instance[Char]   (_.apply(0))     (_.toString)
  implicit val SFloat   = instance[Float]  (_.toFloat)      (_.toString)
  implicit val SDouble  = instance[Double] (_.toDouble)     (_.toString)
  implicit val SInt     = instance[Int]    (_.toInt)        (_.toString)
  implicit val SLong    = instance[Long]   (_.toLong)       (_.toString)
  implicit val SString  = instance[String] (nonEmpty)       (identity)
  implicit val SUUID    = instance[UUID]   (UUID.fromString)(_.toString)
}