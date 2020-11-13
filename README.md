#### Notes
* The reason to use `State` vs `Ref`
    - the `State` monad is inherently sequential (there is no way to run two State actions in parallel and have both changes applied to the initial state).
    - `Ref` is a purely functional model of a concurrent mutable reference, provided by Cats
      Effect. Its atomic update and modify functions allow compositionality and concurrency
      safety that would otherwise be hard to get right.
      
* Tagless Final: Interpreters v Algebras v Programs
    - He uses different nomenclature in this book
    - *Algebra*: it's a type-parameterized trait that looks exactly the same as
      a typeclass. The only difference is that typeclasses should/must be
      coherent (have only one instance). Algebras can have multiple instances.

```scala
trait Counter[F[_]] {
  def incr: F[Unit]
  def get: F[Int]
}
```

  - *Interpreter*: instances of an algebra. Here is where the state is
    actually encapsulated.
    * In this example, all of the information about Redis lives in the
      interpreter. It doesn't leak into programs or algebras - they have no
      idea what the guts of the implementation are.

```scala
class LiveCounter[F[_]: Functor] private (
  key: RedisKey,
  cmd: RedisCommands[F, String, Int]
) extends Counter[F] { ... }

object LiveCounter {
  def make[F[_]: Sync]: Resource[F, Counter[F]] =
    cmdApi.map { cmd =>
      new LiveCounter(RedisKey("myKey"), cmd)
  }
}
  private val cmdApi: : Resource[IO, RedisCommands[IO, String, Int]] = ???
}
```

  - *Programs*: something that uses algebras (via intepreters).

```scala
class ItemsProgram[F[_]: Apply](
  counter: Counter[F],
  items: Items[F]
) {
  def addItem(item: Item): F[Unit] =
    items.add(item) *>
      counter.incr
}
```
