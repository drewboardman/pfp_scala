package suite

import org.scalatest.funsuite.AsyncFunSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

trait PureTestSuite extends AsyncFunSuite with ScalaCheckDrivenPropertyChecks with CatsEquality {}
