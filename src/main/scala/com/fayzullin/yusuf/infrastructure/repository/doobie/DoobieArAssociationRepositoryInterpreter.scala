package com.fayzullin.yusuf.infrastructure.repository.doobie

import cats.data._
import cats.syntax.all._
import doobie._
import doobie.implicits._
import SQLPagination._
import cats.effect.Bracket
import com.fayzullin.yusuf.domain.arAssociations.{ArAssociation, ArAssociationRepositoryAlgebra}

/*
CREATE TABLE AR_ASSOCIATIONS (
  ID BIGSERIAL PRIMARY KEY,
  TARGET VARCHAR NOT NULL,
  RESPONSE  VARCHAR NOT NULL,
); */

private object ARAssociationSQL {
  /* We require type StatusMeta to handle our ADT Status */
//  implicit val StatusMeta: Meta[ArAssociationStatus] =
//    Meta[String].imap(ArAssociationStatus.withName)(_.entryName)

  /* This is used to marshal our sets of strings */
  implicit val SetStringMeta: Meta[Set[String]] =
    Meta[String].imap(_.split(',').toSet)(_.mkString(","))

  def insert(association: ArAssociation): Update0 = sql"""
    INSERT INTO AR_ASSOCIATIONS (TARGET, RESPONSE)
    VALUES (${association.target}, ${association.response})
  """.update

  def update(association: ArAssociation, id: Long): Update0 = sql"""
    UPDATE AR_ASSOCIATIONS
    SET TARGET = ${association.target}, RESPONSE = ${association.response}
    WHERE id = $id
  """.update

  def select(id: Long): Query0[ArAssociation] = sql"""
    SELECT TARGET, RESPONSE, ID
    FROM AR_ASSOCIATIONS
    WHERE ID = $id
  """.query

  def delete(id: Long): Update0 = sql"""
    DELETE FROM AR_ASSOCIATIONS WHERE ID = $id
  """.update

  def selectByTarget(target: String): Query0[ArAssociation] = sql"""
    SELECT TARGET, RESPONSE, ID
    FROM AR_ASSOCIATIONS
    WHERE TARGET = $target
  """.query[ArAssociation]

  def selectAll: Query0[ArAssociation] = sql"""
    SELECT TARGET, RESPONSE, ID
    FROM AR_ASSOCIATIONS
    ORDER BY TARGET
  """.query

//  def selectByStatus(statuses: NonEmptyList[ArAssociationStatus]): Query0[ArAssociation] =
//    (
//      sql"""
//      SELECT NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS, ID
//      FROM AR_ASSOCIATIONS
//      WHERE """ ++ Fragments.in(fr"STATUS", statuses)
//    ).query
//
//  def selectTagLikeString(tags: NonEmptyList[String]): Query0[ArAssociation] = {
//    /* Handle dynamic construction of query based on multiple parameters */
//
//    /* To piggyback off of comment of above reference about tags implementation, findByTag uses LIKE for partial matching
//    since tags is (currently) implemented as a comma-delimited string */
//    val tagLikeString: String = tags.toList.mkString("TAGS LIKE '%", "%' OR TAGS LIKE '%", "%'")
//    (sql"""SELECT NAME, CATEGORY, BIO, STATUS, TAGS, PHOTO_URLS, ID
//         FROM AR_ASSOCIATIONS
//         WHERE """ ++ Fragment.const(tagLikeString))
//      .query[ArAssociation]
//  }
}

class DoobieArAssociationRepositoryInterpreter[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
    extends ArAssociationRepositoryAlgebra[F] {
  import ARAssociationSQL._

  def create(association: ArAssociation): F[ArAssociation] =
    insert(association).withUniqueGeneratedKeys[Long]("ID").map(id => association.copy(id = id.some)).transact(xa)

  def update(association: ArAssociation): F[Option[ArAssociation]] =
    OptionT
      .fromOption[ConnectionIO](association.id)
      .semiflatMap(id => ARAssociationSQL.update(association, id).run.as(association))
      .value
      .transact(xa)

  def get(id: Long): F[Option[ArAssociation]] = select(id).option.transact(xa)

  def delete(id: Long): F[Option[ArAssociation]] =
    OptionT(select(id).option).semiflatMap(association => ARAssociationSQL.delete(id).run.as(association)).value.transact(xa)

//  def findByNameAndCategory(name: String, category: String): F[Set[ArAssociation]] =
//    selectByNameAndCategory(name, category).to[List].transact(xa).map(_.toSet)

  def findByTarget(target: String): F[Set[ArAssociation]] =
    selectByTarget(target).to[List].transact(xa).map(_.toSet)

  def list(pageSize: Int, offset: Int): F[List[ArAssociation]] =
    paginate(pageSize, offset)(selectAll).to[List].transact(xa)

//  def findByStatus(statuses: NonEmptyList[ArAssociationStatus]): F[List[ArAssociation]] =
//    selectByStatus(statuses).to[List].transact(xa)

//  def findByTag(tags: NonEmptyList[String]): F[List[ArAssociation]] =
//    selectTagLikeString(tags).to[List].transact(xa)
}

object DoobieArAssociationRepositoryInterpreter {
  def apply[F[_]: Bracket[*[_], Throwable]](xa: Transactor[F]): DoobieArAssociationRepositoryInterpreter[F] =
    new DoobieArAssociationRepositoryInterpreter(xa)
}
