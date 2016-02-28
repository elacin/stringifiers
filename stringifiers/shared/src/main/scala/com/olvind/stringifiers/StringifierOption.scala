package com.olvind.stringifiers

private[stringifiers] final class StringifierOption[E](
  wrapped: Stringifier[E]) extends Stringifier[Option[E]] {

  override val throwableFormatter: ThrowableFormatter =
    wrapped.throwableFormatter

  override val format: Format =
    wrapped.format

  override val typename: Typename =
    Typename(s"Option[${wrapped.typename.value}]")

  override val enumValues: Option[Set[Option[E]]] =
    wrapped.enumValues map (_ map Option.apply)

  override def decode(str: String): Either[DecodeFail, Option[E]] =
    Option(str) filter (_.nonEmpty) match {
      case Some(value) => (wrapped decode value).right map Some.apply
      case _           => Right(None)
    }

  override def encode(oe: Option[E]): String =
    oe map wrapped.encode getOrElse ""
}
