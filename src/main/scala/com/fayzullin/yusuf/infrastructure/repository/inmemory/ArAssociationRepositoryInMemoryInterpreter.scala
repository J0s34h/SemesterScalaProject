package com.fayzullin.yusuf.infrastructure.repository.inmemory

import scala.collection.concurrent.TrieMap
import scala.util.Random
import cats._
//import cats.data.NonEmptyList
import cats.implicits._
import com.fayzullin.yusuf.domain.arAssociations.{ArAssociation, ArAssociationRepositoryAlgebra}

class ArAssociationRepositoryInMemoryInterpreter[F[_]: Applicative] extends ArAssociationRepositoryAlgebra[F] {
  private val cache = new TrieMap[Long, ArAssociation]

  private val random = new Random

  def create(association: ArAssociation): F[ArAssociation] = {
    val id = random.nextLong()
    val toSave = association.copy(id = id.some)
    cache += (id -> association.copy(id = id.some))
    toSave.pure[F]
  }

  def update(association: ArAssociation): F[Option[ArAssociation]] = association.id.traverse { id =>
    cache.update(id, association)
    association.pure[F]
  }

  def get(id: Long): F[Option[ArAssociation]] = cache.get(id).pure[F]

  def delete(id: Long): F[Option[ArAssociation]] = cache.remove(id).pure[F]

  def findByTarget(target: String): F[Set[ArAssociation]] =
    cache.values
      .filter(p => p.target == target)
      .toSet
      .pure[F]

  def list(pageSize: Int, offset: Int): F[List[ArAssociation]] =
    cache.values.toList.sortBy(_.target).slice(offset, offset + pageSize).pure[F]

//  def findByStatus(statuses: NonEmptyList[ArAssociationStatus]): F[List[ArAssociation]] =
//    cache.values.filter(p => statuses.exists(_ == p.status)).toList.pure[F]
//
//  def findByTag(tags: NonEmptyList[String]): F[List[ArAssociation]] = {
//    val tagSet = tags.toNes
//    cache.values.filter(_.tags.exists(tagSet.contains(_))).toList.pure[F]
//  }
}

object ArAssociationRepositoryInMemoryInterpreter {
  def apply[F[_]: Applicative]() = new ArAssociationRepositoryInMemoryInterpreter[F]()
}
