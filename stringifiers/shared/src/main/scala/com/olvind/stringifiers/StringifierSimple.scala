package com.olvind.stringifiers

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

private[stringifiers] final class StringifierSimple[E: ClassTag](
  override val typename:           Typename,
  override val format:             Format,
  override val throwableFormatter: ThrowableFormatter,
               _decode:            String => Try[E],
               _encode:            E => String) extends Stringifier[E] {

  override val enumValues: Option[Set[E]] =
    None

  def encode(e: E): String =
    _encode(e)

  def decode(str: String): Either[DecodeFail, E] =
    _decode(str) match {
      case Success(e)  => Right(e)
      case Failure(th) => Left(ValueNotValid(str, typename, throwableFormatter(th)))
    }
}
