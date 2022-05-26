package com.fayzullin.yusuf.domain.orders

import cats.Functor
import cats.data.EitherT
import cats.syntax.all._
import com.fayzullin.yusuf.domain.OrderNotFoundError

class OrderService[F[_]](orderRepo: OrderRepositoryAlgebra[F]) {
  def placeOrder(order: Order): F[Order] =
    orderRepo.create(order)

  def get(id: Long)(implicit F: Functor[F]): EitherT[F, OrderNotFoundError.type, Order] =
    EitherT.fromOptionF(orderRepo.get(id), OrderNotFoundError)

  def delete(id: Long)(implicit F: Functor[F]): F[Unit] =
    orderRepo.delete(id).as(())
}

object OrderService {
  def apply[F[_]](orderRepo: OrderRepositoryAlgebra[F]): OrderService[F] =
    new OrderService(orderRepo)
}
