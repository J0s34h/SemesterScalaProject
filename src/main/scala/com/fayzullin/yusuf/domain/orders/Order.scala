package com.fayzullin.yusuf.domain.orders

import java.time.Instant

case class Order(
    petId: Long,
    shipDate: Option[Instant] = None,
    status: OrderStatus = OrderStatus.Placed,
    complete: Boolean = false,
    id: Option[Long] = None,
    userId: Option[Long],
)
