package com.fayzullin.yusuf.infrastructure.endpoint

//import cats.data.Validated.Valid
//import cats.data._
import cats.effect.Sync
import cats.syntax.all._
import com.fayzullin.yusuf.domain.{ArAssociationAlreadyExists, ArAssociationNotFoundError}
import com.fayzullin.yusuf.domain.authentication.Auth
import com.fayzullin.yusuf.domain.arAssociations.{ArAssociation, ArAssociationService}
import com.fayzullin.yusuf.domain.users.User
import io.circe.generic.auto._
import io.circe.syntax._
import com.fayzullin.yusuf.domain.authentication.Auth
import org.http4s.circe._
import org.http4s.dsl.Http4sDsl
import org.http4s.{EntityDecoder, HttpRoutes}
//import org.http4s.{EntityDecoder, HttpRoutes, QueryParamDecoder}
import com.fayzullin.yusuf.domain.users.User
import tsec.jwt.algorithms.JWTMacAlgo
import tsec.authentication._

class ArAssociationEndpoints[F[_]: Sync, Auth: JWTMacAlgo] extends Http4sDsl[F] {
  import Pagination._

  /* Parses out status query param which could be multi param */
//  implicit val statusQueryParamDecoder: QueryParamDecoder[ArAssociationStatus] =
//    QueryParamDecoder[String].map(ArAssociationStatus.withName)

  /* Relies on the statusQueryParamDecoder implicit, will parse out a possible multi-value query parameter */
//  object StatusMatcher extends OptionalMultiQueryParamDecoderMatcher[ArAssociationStatus]("status")

  /* Parses out tag query param, which could be multi-value */
//  object TagMatcher extends OptionalMultiQueryParamDecoderMatcher[String]("tags")

  implicit val associationDecoder: EntityDecoder[F, ArAssociation] = jsonOf[F, ArAssociation]

  private def createArAssociationEndpoint(associationService: ArAssociationService[F]): HttpRoutes[F] = {
    HttpRoutes.of[F] {
      case req @ POST -> Root =>
        val action = for {
          association <- req.as[ArAssociation]
          result <- associationService.create(association).value
        } yield result

        action.flatMap {
          case Right(saved) =>
            Ok(saved.asJson)
          case Left(ArAssociationAlreadyExists(existing)) =>
            Conflict(s"The association ${existing.target} of category ${existing.response} already exists")
        }
    }
  }

  private def updateArAssociationEndpoint(associationService: ArAssociationService[F]): AuthEndpoint[F, Auth] = {
    case req @ PUT -> Root / LongVar(_) asAuthed _ =>
      val action = for {
        association <- req.request.as[ArAssociation]
        result <- associationService.update(association).value
      } yield result

      action.flatMap {
        case Right(saved) => Ok(saved.asJson)
        case Left(ArAssociationNotFoundError) => NotFound("The association was not found")
      }
  }

  private def getArAssociationEndpoint(associationService: ArAssociationService[F]): AuthEndpoint[F, Auth] = {
     case GET -> Root / LongVar(id) asAuthed _ =>
      associationService.get(id).value.flatMap {
        case Right(found) => Ok(found.asJson)
        case Left(ArAssociationNotFoundError) => NotFound("The association was not found")
      }
  }

  private def deleteArAssociationEndpoint(associationService: ArAssociationService[F]): AuthEndpoint[F, Auth] = {
    case DELETE -> Root / LongVar(id) asAuthed _ =>
      for {
        _ <- associationService.delete(id)
        resp <- Ok()
      } yield resp
  }

  private def listArAssociationsEndpoint(associationService: ArAssociationService[F]): HttpRoutes[F] = {
        HttpRoutes.of[F] {
          case GET -> Root :? OptionalPageSizeMatcher(pageSize) :? OptionalOffsetMatcher(offset) => {
            println("Received list AR Sessions")
            for {
              retrieved <- associationService.list(pageSize.getOrElse(10), offset.getOrElse(0))
              resp <- Ok(retrieved.asJson)
            } yield resp
          }
        }
  }

//  private def findArAssociationsByStatusEndpoint(associationService: ArAssociationService[F]): AuthEndpoint[F, Auth] = {
//    case GET -> Root / "findByStatus" :? StatusMatcher(Valid(_)) asAuthed _ =>
//      NonEmptyList.fromList(_) match {
//        case None =>
//          // User did not specify any statuses
//          BadRequest("status parameter not specified")
//        case Some(statuses) =>
//          // We have a list of valid statuses, find them and return
//          for {
//            retrieved <- associationService.findByStatus(statuses)
//            resp <- Ok(retrieved.asJson)
//          } yield resp
//      }
//  }
//
//  private def findArAssociationsByTagEndpoint(associationService: ArAssociationService[F]): AuthEndpoint[F, Auth] = {
//    case GET -> Root / "findByTags" :? TagMatcher(Valid(xs)) asAuthed _ =>
//      NonEmptyList.fromList(xs) match {
//        case None =>
//          BadRequest("tag parameter not specified")
//        case Some(tags) =>
//          for {
//            retrieved <- associationService.findByTag(tags)
//            resp <- Ok(retrieved.asJson)
//          } yield resp
//      }
//  }

  def endpoints(
      associationService: ArAssociationService[F],
      auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[F] = {
    val authEndpoints: AuthService[F, Auth] = {
      val allRoles =
          getArAssociationEndpoint(associationService)
//          .orElse(findArAssociationsByStatusEndpoint(associationService))
//          .orElse(findArAssociationsByTagEndpoint(associationService))

      val onlyAdmin =
        deleteArAssociationEndpoint(associationService).orElse(updateArAssociationEndpoint(associationService))

      Auth.allRolesHandler(allRoles)(Auth.adminOnly(onlyAdmin))
    }

    val unauthEndpoint: HttpRoutes[F] = {

      listArAssociationsEndpoint(associationService) <+> createArAssociationEndpoint(associationService)
    }


    unauthEndpoint <+> auth.liftService(authEndpoints)
  }
}

object ArAssociationEndpoints {
  def endpoints[F[_]: Sync, Auth: JWTMacAlgo](
      associationService: ArAssociationService[F],
      auth: SecuredRequestHandler[F, Long, User, AugmentedJWT[Auth, Long]],
  ): HttpRoutes[F] =
    new ArAssociationEndpoints[F, Auth].endpoints(associationService, auth)
}
