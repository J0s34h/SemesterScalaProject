package com.fayzullin.yusuf.domain.arAssociations

import cats.Functor
import cats.data._
import cats.Monad
import cats.syntax.all._
import com.fayzullin.yusuf.domain.{ArAssociationAlreadyExists, ArAssociationNotFoundError}

/**
  * The entry point to our domain, works with repositories and validations to implement behavior
  * @param repository where we get our data
  * @param validation something that provides validations to the service
  * @tparam F - this is the container for the things we work with, could be scala.concurrent.Future, Option, anything
  *           as long as it is a Monad
  */
class ArAssociationService[F[_]](
                                  repository: ArAssociationRepositoryAlgebra[F],
) {
  def create(assocation: ArAssociation)(implicit M: Monad[F]): EitherT[F, ArAssociationAlreadyExists, ArAssociation] =
    for {
      saved <- EitherT.liftF(repository.create(assocation))
    } yield saved

  /* Could argue that we could make this idempotent on put and not check if the assocation exists */
  def update(assocation: ArAssociation)(implicit M: Monad[F]): EitherT[F, ArAssociationNotFoundError.type, ArAssociation] =
    for {
      saved <- EitherT.fromOptionF(repository.update(assocation), ArAssociationNotFoundError)
    } yield saved

  def get(id: Long)(implicit F: Functor[F]): EitherT[F, ArAssociationNotFoundError.type, ArAssociation] =
    EitherT.fromOptionF(repository.get(id), ArAssociationNotFoundError)

  /* In some circumstances we may care if we actually delete the assocation; here we are idempotent and do not care */
  def delete(id: Long)(implicit F: Functor[F]): F[Unit] =
    repository.delete(id).as(())

  def list(pageSize: Int, offset: Int): F[List[ArAssociation]] =
    repository.list(pageSize, offset)

  def findByTarget(target: String): F[Set[ArAssociation]] =
    repository.findByTarget(target)
}

object ArAssociationService {
  def apply[F[_]](
                   repository: ArAssociationRepositoryAlgebra[F],
  ): ArAssociationService[F] =
    new ArAssociationService[F](repository)
}
