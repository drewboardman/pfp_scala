package shop.ext

import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import skunk.Codec

object skunkx {

  implicit class CodecOps[B](codec: Codec[B]) {

    /**
      * This is imap for newtypes. Requires instance of Coercible from the newtype to type B.
      * Here is an example of different ways to construct a Codec:
      *
      * val uName: Codec[UserName] = varchar.cimap[UserName]
      *
      * val uNameD: Decoder[UserName] = varchar.map(UserName)
      *
      * val uNameE: Encoder[UserName] = varchar.contramap(_.value)
      *
      * val uNameC: Codec[UserName] = varchar.imap(UserName)(_.value)
      *
      * @param ev
      * @tparam A
      * @return
      */
    def cimap[A: Coercible[B, *]](implicit ev: Coercible[A, B]): Codec[A] =
      codec.imap(_.coerce[A])((ev(_)))
  }

}
