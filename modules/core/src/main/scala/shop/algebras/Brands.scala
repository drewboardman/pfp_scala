package shop.algebras

import shop.domain.Brand.{ Brand, BrandName }

/**
  * This is an algebra, not a typeclass.
  * Although they look the same, the main difference is that algebras do not need coherence, they
  * can have multiple instances for the same type. This is not checked by the compiler.
  */
trait Brands[F[_]] {
  def findAll: F[List[Brand]]
  def create(name: BrandName): F[Unit]
}
