package com.fayzullin.yusuf.domain.arAssociations

trait ArAssociationRepositoryAlgebra[F[_]] {
  def create(assocation: ArAssociation): F[ArAssociation]

  def update(assocation: ArAssociation): F[Option[ArAssociation]]

  def get(id: Long): F[Option[ArAssociation]]

  def delete(id: Long): F[Option[ArAssociation]]

  def findByTarget(name: String): F[Set[ArAssociation]]

  def list(pageSize: Int, offset: Int): F[List[ArAssociation]]
}
