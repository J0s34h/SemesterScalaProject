package com.fayzullin.yusuf.infrastructure.repository.doobie

import org.scalatest.funsuite.AnyFunSuite
import cats.effect.IO
import doobie.scalatest.IOChecker
import doobie.util.transactor.Transactor

import com.fayzullin.yusuf.PetStoreArbitraries._
import org.scalatest.matchers.should.Matchers

class OrderQueryTypeCheckSpec extends AnyFunSuite with Matchers with IOChecker {
  import OrderSQL._

  override val transactor: Transactor[IO] = testTransactor

  test("Typecheck order queries") {
    check(delete(1L))
    check(select(1L))

    order(Some(1L)).arbitrary.sample.map(o => check(insert(o)))
  }
}
