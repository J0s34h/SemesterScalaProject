//package com.fayzullin.yusuf.infrastructure.endpoint
//
//import cats.data.NonEmptyList
//import cats.effect._
//import com.fayzullin.yusuf.ArAssociationStoreArbitraries
//import com.fayzullin.yusuf.domain.arAssociations._
//import com.fayzullin.yusuf.domain.users.User
//import com.fayzullin.yusuf.infrastructure.repository.inmemory.{ArAssociationRepositoryInMemoryInterpreter, UserRepositoryInMemoryInterpreter}
//import io.circe.generic.auto._
//import org.http4s._
//import org.http4s.implicits._
//import org.http4s.dsl._
//import org.http4s.circe._
//import org.http4s.client.dsl.Http4sClientDsl
//import org.http4s.server.Router
//import org.scalatest.funsuite.AnyFunSuite
//import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
//import tsec.mac.jca.HMACSHA256
//import org.scalatest.matchers.should.Matchers
//
//class ArAssociationEndpointsSpec
//    extends AnyFunSuite
//    with Matchers
//    with ScalaCheckPropertyChecks
//    with ArAssociationStoreArbitraries
//    with Http4sDsl[IO]
//    with Http4sClientDsl[IO] {
//  implicit val petEnc: EntityEncoder[IO, ArAssociation] = jsonEncoderOf
//  implicit val petDec: EntityDecoder[IO, ArAssociation] = jsonOf
//
//  def getTestResources(): (AuthTest[IO], HttpApp[IO], ArAssociationRepositoryInMemoryInterpreter[IO]) = {
//    val userRepo = UserRepositoryInMemoryInterpreter[IO]()
//    val petRepo = ArAssociationRepositoryInMemoryInterpreter[IO]()
//    val petValidation = ArAssociationValidationInterpreter[IO](petRepo)
//    val petService = ArAssociationService[IO](petRepo, petValidation)
//    val auth = new AuthTest[IO](userRepo)
//    val petEndpoint = ArAssociationEndpoints.endpoints[IO, HMACSHA256](petService, auth.securedRqHandler)
//    val petRoutes = Router(("/pets", petEndpoint)).orNotFound
//    (auth, petRoutes, petRepo)
//  }
//
//  test("create pet") {
//    val (auth, petRoutes, _) = getTestResources()
//
//    forAll { pet: ArAssociation =>
//      (for {
//        request <- POST(pet, uri"/pets")
//        response <- petRoutes.run(request)
//      } yield response.status shouldEqual Unauthorized).unsafeRunSync()
//    }
//
//    forAll { (pet: ArAssociation, user: User) =>
//      (for {
//        request <- POST(pet, uri"/pets")
//          .flatMap(auth.embedToken(user, _))
//        response <- petRoutes.run(request)
//      } yield response.status shouldEqual Ok).unsafeRunSync()
//    }
//
//    forAll { (pet: ArAssociation, user: User) =>
//      (for {
//        createRq <- POST(pet, uri"/pets")
//          .flatMap(auth.embedToken(user, _))
//        response <- petRoutes.run(createRq)
//        createdArAssociation <- response.as[ArAssociation]
//        getRq <- GET(Uri.unsafeFromString(s"/pets/${createdArAssociation.id.get}"))
//          .flatMap(auth.embedToken(user, _))
//        response2 <- petRoutes.run(getRq)
//      } yield {
//        response.status shouldEqual Ok
//        response2.status shouldEqual Ok
//      }).unsafeRunSync()
//    }
//  }
//
//  test("update pet") {
//    val (auth, petRoutes, _) = getTestResources()
//
//    forAll { (pet: ArAssociation, user: AdminUser) =>
//      (for {
//        createRequest <- POST(pet, uri"/pets")
//          .flatMap(auth.embedToken(user.value, _))
//        createResponse <- petRoutes.run(createRequest)
//        createdArAssociation <- createResponse.as[ArAssociation]
//        petToUpdate = createdArAssociation.copy(name = createdArAssociation.name.reverse)
//        updateRequest <- PUT(petToUpdate, Uri.unsafeFromString(s"/pets/${petToUpdate.id.get}"))
//          .flatMap(auth.embedToken(user.value, _))
//        updateResponse <- petRoutes.run(updateRequest)
//        updatedArAssociation <- updateResponse.as[ArAssociation]
//      } yield updatedArAssociation.name shouldEqual pet.name.reverse).unsafeRunSync()
//    }
//  }
//
//  test("find by tag") {
//    val (auth, petRoutes, petRepo) = getTestResources()
//
//    forAll { (pet: ArAssociation, user: AdminUser) =>
//      (for {
//        createRequest <- POST(pet, uri"/pets")
//          .flatMap(auth.embedToken(user.value, _))
//        createResponse <- petRoutes.run(createRequest)
//        createdArAssociation <- createResponse.as[ArAssociation]
//      } yield createdArAssociation.tags.toList.headOption match {
//        case Some(tag) =>
//          val petsFoundByTag = petRepo.findByTag(NonEmptyList.of(tag)).unsafeRunSync()
//          petsFoundByTag.contains(createdArAssociation) shouldEqual true
//        case _ => ()
//      }).unsafeRunSync()
//    }
//  }
//}
