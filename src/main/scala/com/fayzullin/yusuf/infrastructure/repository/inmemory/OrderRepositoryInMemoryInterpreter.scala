package com.fayzullin.yusuf.infrastructure.repository.inmemory

import scala.collection.concurrent.TrieMap
import scala.util.Random
import cats._
import cats.syntax.all._
import com.fayzullin.yusuf.domain.orders
import com.fayzullin.yusuf.domain.orders.OrderRepositoryAlgebra

class OrderRepositoryInMemoryInterpreter[F[_]: Applicative] extends OrderRepositoryAlgebra[F] {
  private val cache = new TrieMap[Long, orders.Order]

  private val random = new Random

  def create(order: orders.Order): F[orders.Order] = {
    val toSave = order.copy(id = order.id.orElse(random.nextLong().some))
    toSave.id.foreach(cache.put(_, toSave))
    toSave.pure[F]
  }

  def get(orderId: Long): F[Option[orders.Order]] =
    cache.get(orderId).pure[F]

  def delete(orderId: Long): F[Option[orders.Order]] =
    cache.remove(orderId).pure[F]
}

object OrderRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new OrderRepositoryInMemoryInterpreter[F]()
}
