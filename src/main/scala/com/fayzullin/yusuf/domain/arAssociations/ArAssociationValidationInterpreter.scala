//package com.fayzullin.yusuf.domain.arAssociations
//
//import cats.Applicative
//import cats.data.EitherT
//import cats.syntax.all._
//import cats.implicits._
//
//import com.fayzullin.yusuf.domain.{ArAssociationAlreadyExists, ArAssociationNotFoundError}
////
////class ArAssociationValidationInterpreter[F[_] : Applicative](repository: ArAssociationRepositoryAlgebra[F])
////  extends ArAssociationValidationAlgebra[F] {
////
////  def doesNotExist(association: ArAssociation): EitherT[F, ArAssociationAlreadyExists, Unit] = {
////
////    repository.findByTarget(association.target).map { matches =>
////      if (matches.forall(possibleMatch => possibleMatch.target != association.target)) {
////        Right(())
////      } else {
////        Left(ArAssociationAlreadyExists(association))
////      }
////    }
////  }
////
////  def exists(associationId: Option[Long]): EitherT[F, ArAssociationNotFoundError.type, Unit] = {
////      associationId match {
////        case Some(id) =>
////          // Ensure is a little tough to follow, it says "make sure this condition is true, otherwise throw the error specified
////          // In this example, we make sure that the option returned has a value, otherwise the pet was not found
////          repository.get(id) {
////            case Some(_) => Right(())
////            case _ => Left(ArAssociationNotFoundError)
////          }
////        case _ =>
////          Either.left[ArAssociationNotFoundError.type, Unit](ArAssociationNotFoundError).pure[F]
////      }
////    }
////
////}
////
////object ArAssociationValidationInterpreter {
////  def apply[F[_] : Applicative](repository: ArAssociationRepositoryAlgebra[F]) =
////    new ArAssociationValidationInterpreter[F](repository)
////}
