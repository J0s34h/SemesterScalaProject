import cats._
import cats.effect._
import cats.implicits._
import io.circe.generic.auto._
import io.circe.syntax._
import org.http4s._
import org.http4s.circe._
import org.http4s.dsl._
import org.http4s.dsl.impl._
import org.http4s.headers._
import org.http4s.implicits._
import org.http4s.server._
import org.http4s.server.blaze.BlazeServerBuilder

import java.time.Year
import java.util.UUID
import scala.collection.mutable
import scala.util.Try

object Http4sTutorial extends IOApp {

  type Actor = String

  case class Movie(id: String, title: String, year: Int, actors: List[Actor], director: String)

  case class Director(firstName: String, lastName: String) {
    override def toString: Actor = s"$firstName $lastName"
  }

  case class DirectorDetails(firstName: String, lastName: String, genre: String)

  // internal database
  val moviesDB: Movie = Movie(
    "6bcbca1e-efd3-411d-9f7c-14b872444fce",
    "Joker",
    2021,
    List("Actor 1", "Actor 2", "Actor 3", "Actor 4"),
    "Josef"
  )

  val movies: Map[String, Movie] = Map(moviesDB.id -> moviesDB)

  // "business logic"
  private def findMovieById(movieId: UUID) =
    movies.get(movieId.toString)

  private def findMoviesByDirector(director: String): List[Movie] =
    movies.values.filter(_.director == director).toList



  /*
    - Get all movies for a director under a given year
    - Get all actors for a movie
    - Post add a new director
  */

  // Request -> F<Option<Response>>
  // HTTPRoutes<F>

  implicit val yearQueryParamDecoder: QueryParamDecoder[Year] =
    QueryParamDecoder[Int].emap { yearInt =>

      Try(Year.of(yearInt))
        .toEither
        .leftMap { e =>
          ParseFailure(e.getMessage, e.getMessage)
        }

    }


  object DirectorQueryParamMatcher extends QueryParamDecoderMatcher[String]("director")

  object YearQueryParamMatcher extends OptionalValidatingQueryParamDecoderMatcher[Year]("year")

  // Get /movies?director=Zack%20snyder&year=2021
  def movieRoutes[F[_] : Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "movies" :? DirectorQueryParamMatcher(director) +& YearQueryParamMatcher(maybeYear) =>
        val movieByDirector = findMoviesByDirector(director)
        maybeYear match {
          case Some(validateYear) =>
            validateYear.fold(
              _ => BadRequest("Wrong year format"),
              year => {
                val moviesByDirector = findMoviesByDirector(director)
                val moviesByDirectorAndYear = moviesByDirector.filter(_.year == year.getValue)

                Ok(moviesByDirectorAndYear.asJson)
              }
            )
          case None => Ok(movieByDirector.asJson)
        }

      case GET -> Root / "movies" / UUIDVar(movieId) / "actors" =>
        findMovieById(movieId).map(_.actors) match {
          case Some(actors) => Ok(actors.asJson)
          case _ => NotFound(s"No movies found withd id '$movieId'")
        }
    }
  }

  //Get Director

  object DirectorPath {
    def unapply(str: String): Option[Director] = {
      Try {
        val tokens = str.split(" ")
        Director(tokens(0), tokens(1))
      }.toOption
    }
  }

  var directorDetailsDB: mutable.Map[Director, DirectorDetails] =
    mutable.Map(Director("Zack", "Snyder") -> DirectorDetails("Zack", "lastName", "superhero"))

  def directorRoutes[F[_] : Monad]: HttpRoutes[F] = {
    val dsl = Http4sDsl[F]
    import dsl._

    HttpRoutes.of[F] {
      case GET -> Root / "directors" / DirectorPath(director) =>
        directorDetailsDB.get(director) match {
          case Some(dirDetails) => Ok(dirDetails.asJson)
          case _ => NotFound(s"NO director '$director' found")
        }
    }
  }

  def allRoutes[F[_] : Monad]: HttpRoutes[F] =
    movieRoutes[F] <+> directorRoutes[F]

  def allRoutesComplete[F[_] : Monad]: HttpApp[F] =
    allRoutes[F].orNotFound


  override def run(args: List[String]): IO[ExitCode] = {
    val apis = Router(
      "/api" -> movieRoutes[IO],
      "/api/admin" -> directorRoutes[IO]
    ).orNotFound

    BlazeServerBuilder[IO](runtime.compute)
      .bindHttp(9090, "localhost")
      .withHttpApp(allRoutesComplete)
      .resource
      .use(_ => IO.never)
      .as(ExitCode.Success)
  }
}