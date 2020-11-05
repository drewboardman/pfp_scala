#### Notes
* The reason to use `State` vs `Ref`
    - the `State` monad is inherently sequential (there is no way to run two State actions in parallel and have both changes applied to the initial state).
    - `Ref` is a purely functional model of a concurrent mutable reference, provided by Cats
      Effect. Its atomic update and modify functions allow compositionality and concurrency
      safety that would otherwise be hard to get right.