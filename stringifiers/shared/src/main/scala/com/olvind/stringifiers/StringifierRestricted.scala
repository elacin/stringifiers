package com.olvind.stringifiers

private[stringifiers] final class StringifierRestricted[E](
  wrapped:    Stringifier[E],
  restricted: Set[E]) extends Stringifier[E] {

  val restrictedMap: Map[String, E] =
    (restricted foldLeft Map.empty[String, E]){
      case (map, e) => map + ((wrapped encode e) -> e)
    }

  val restrictedStrings: Set[String] =
    restricted map wrapped.encode

  override val throwableFormatter: ThrowableFormatter =
    wrapped.throwableFormatter

  override val format: Format =
    Format.Enum

  override val typename: Typename =
    wrapped.typename

  override val restrictedValues: Option[Set[E]] =
    Some(restricted)

  override def decode(str: String): Either[DecodeFail, E] =
    restrictedMap get str toRight
      ValueNotInSet(str, typename, restrictedStrings)

  override def encode(e: E): String =
    wrapped encode e
}
