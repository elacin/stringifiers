package com.olvind.stringifiers

private[stringifiers] final class StringifierEnum[E](
  wrapped: Stringifier[E],
  enumSet: Set[E]) extends Stringifier[E] {

  val enumMap: Map[String, E] =
    (enumSet foldLeft Map.empty[String, E]){
      case (map, e) => map + ((wrapped encode e) -> e)
    }

  val stringSet: Set[String] =
    enumSet map wrapped.encode

  override val throwableFormatter: ThrowableFormatter =
    wrapped.throwableFormatter

  override val format: Format =
    Format.Enum

  override val typename: Typename =
    wrapped.typename

  override val enumValues: Option[Set[E]] =
    Some(enumSet)

  override def decode(str: String): Either[DecodeFail, E] =
    enumMap get str toRight ValueNotInSet(str, typename, stringSet)

  override def encode(e: E): String =
    wrapped encode e
}
