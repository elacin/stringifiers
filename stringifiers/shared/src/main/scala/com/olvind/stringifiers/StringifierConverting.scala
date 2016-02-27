package com.olvind.stringifiers

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

private[stringifiers] final class StringifierConverting[E, F: ClassTag](
  override val typename:           Typename,
  override val throwableFormatter: ThrowableFormatter,
               wrapped:            Stringifier[E],
               to:                 E => Try[F],
               from:               F => E) extends Stringifier[F] {

  override val restrictedValues: Option[Set[F]] =
    wrapped.restrictedValues map (_ map to collect { case Success(s) => s })

  override val format: Format =
    wrapped.format

  override def decode(str: String) =
    (wrapped decode str).right flatMap {
      e => to(e) match {
        case Success(f)  => Right(f)
        case Failure(th) => Left(ValueNotValid(str, typename, throwableFormatter(th)))
      }
    }

  override def encode(f: F): String =
    wrapped encode from(f)
}
