package shop.ext

import io.estatico.newtype.Coercible
import io.estatico.newtype.ops._
import skunk.Codec

object skunkx {

  implicit class CodecOps[B](codec: Codec[B]) {

    /**
      * This is imap for newtypes. Requires instance of Coercible from the newtype to type B.
      * @param ev
      * @tparam A
      * @return
      */
    def cimap[A: Coercible[B, *]](implicit ev: Coercible[A, B]): Codec[A] =
      codec.imap(_.coerce[A])((ev(_)))
  }

}
