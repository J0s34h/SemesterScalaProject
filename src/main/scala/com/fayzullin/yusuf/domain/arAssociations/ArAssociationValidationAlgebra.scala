package com.fayzullin.yusuf.domain.arAssociations

import cats.data.EitherT
import com.fayzullin.yusuf.domain.{ArAssociationAlreadyExists, ArAssociationNotFoundError}

trait ArAssociationValidationAlgebra[F[_]] {
  /* Fails with a PetAlreadyExistsError */
  def doesNotExist(assocation: ArAssociation): EitherT[F, ArAssociationAlreadyExists, Unit]

  /* Fails with a PetNotFoundError if the pet id does not exist or if it is none */
  def exists(petId: Option[Long]): EitherT[F, ArAssociationNotFoundError.type, Unit]
}
