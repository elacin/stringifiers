# Stringifiers

A Scala micro library for moving values to and from strings,
 with error handling and abstraction possibilities.

Analogue to `Read` and `Show` type classes, but a bit simpler and more specialized.
The library aims to be a comfortable introduction to type class-based programming.

In the simplest of cases, these `Stringifier`s can be used to DRY
 code which otherwise would ad hoc parse the same types everywhere.

A bit more elaborate use can see them used to derive Json or Xml
 codecs, for parsing Http parameters, for sql, /etc/.


## Example

```scala

scala> import com.olvind.stringifiers._
import com.olvind.stringifiers._

scala> case class UserId(value: Int) extends AnyVal
defined class UserId

/* This picks up the bundled instance for `Int`,
    and uses that to construct one for UserId */
scala> implicit val S = Stringifier.instance(UserId)(_.value)
S: Stringifier[UserId] = ...

/* encoding is modelled as not failing */
scala> Stringifier encode UserId(2)
res0: String = 2

/* while parsing, of course, can fail */
scala> Stringifier decode[UserId] "2"
res0: Either[DecodeFail,UserId] = Right(UserId(2))

/* can also use the instance directly */
scala> S decode "zxc"
res0: Either[DecodeFail,UserId] = Left(ValueNotValid(zxc,Typename(UserId),Some(NumberFormatException: For input string: "zxc")))

/* supports optional values */
scala> Stringifier decode[Option[UserId]] ""
res0: Either[com.olvind.stringifiers.DecodeFail,Option[UserId]] = Right(None)
```

## Setup (sbt)

Available for Scala 2.10/2.11:
```scala
libraryDependencies += "com.olvind" %% "stringifiers" % "0.1"
```

And for Scala.js (not compiled for 2.10 because of a scaladoc issue)
```scala
libraryDependencies += "com.olvind" %%% "stringifiers" % "0.1"
```

# Type class instance derivation

A lot of operations naturally work for `String`. By having defined a way to map back and forth, we
 can provide loads of type class instances with just a few lines of glue code.

So by providing these once:
```scala
object ArgonautSupport {
  import scala.language.implicitConversions

  implicit def toEncoder[T](implicit S: Stringifier[T]): EncodeJson[T] =
    EncodeJson[T](S.encode andThen Argonaut.jString)

  implicit def toDecoder[T](implicit S: Stringifier[T]): DecodeJson[T] =
    DecodeJson[T](
      c => c.as[String](DecodeJson.StringDecodeJson) map Stringifier[T].decode flatMap {
        case Right(t) =>
          DecodeResult ok t
        case Left(ValueNotValid(v, t, oe)) =>
          DecodeResult fail (s"«$v» is not a valid ${t.value}${oe.fold("")(": " + _)}", c.history)
        case Left(ValueNotInSet(v, t, ss)) =>
          DecodeResult fail (s"«$v» is not a valid ${t.value}. Not among ${ss.mkString("[«", "», «", "»]")}", c.history)
      }
    )
}
```

You can have these implicits automagically:
```scala
  case class Secrets(value: Long)
  implicit val S: Stringifier[Secrets] = (Stringifier instance Secrets)(_.value)

  import Argonaut._, ArgonautSupport._
  implicitly[EncodeJson[WrapChar]]
  implicitly[DecodeJson[WrapChar]]
```