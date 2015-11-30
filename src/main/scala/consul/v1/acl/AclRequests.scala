package consul.v1.acl

import consul.v1.common.ConsulRequestBasics._
import consul.v1.ws.WSProvider
import play.api.http.Status
import play.api.libs.json.{JsNull, Json}

import scala.concurrent.{ExecutionContext, Future}

trait AclRequests {
  def create(Name:Option[String]=Option.empty,Type:Option[String]=Option.empty,Rules:Option[String]=Option.empty):Future[AclIdResponse]
  def create(acl:AclCreate):Future[AclIdResponse]
  def update(ID:AclId,Name:Option[String]=Option.empty,Type:Option[String]=Option.empty,Rules:Option[String]=Option.empty):Future[Boolean]
  def update(acl:AclUpdate):Future[Boolean]
  def destroy(id:AclId):Future[Boolean]
  def info(id:AclId):Future[Option[AclInfo]]
  def clone(id:AclId): Future[AclIdResponse]
  def list():Future[Seq[AclInfo]]

  def AclCreate = consul.v1.acl.AclCreate.apply _
  def AclUpdate = consul.v1.acl.AclUpdate.apply _
  def AclId     = consul.v1.acl.AclId
}

object AclRequests{

  def apply(basePath: String)(implicit executionContext: ExecutionContext, wsProvider: WSProvider): AclRequests = new AclRequests{

    def create(acl:AclCreate):Future[AclIdResponse] = erased(
      jsonRequestMaker(createPath,_.put(Json.toJson(acl)))(_.validate[AclIdResponse])
    )

    def create(Name:Option[String],Type:Option[String],Rules:Option[String]):Future[AclIdResponse] = create(
      AclCreate(Name,Type,Rules)
    )

    def update(acl: AclUpdate): Future[Boolean] =
      responseStatusRequestMaker(updatePath,_.put(Json.toJson(acl)))(_ == Status.OK)

    def update(ID:AclId,Name:Option[String],Type:Option[String],Rules:Option[String]): Future[Boolean] =
      update(AclUpdate(ID,Name,Type,Rules))

    def destroy(id:AclId):Future[Boolean] =
      responseStatusRequestMaker(fullPathFor(s"destroy/$id"),_.put(JsNull))(_ == Status.OK)

    def list():Future[Seq[AclInfo]] = erased(
      jsonRequestMaker(listPath,_.get())(_.validate[Seq[AclInfo]])
    )

    def info(id:AclId): Future[Option[AclInfo]] = erased(
      jsonRequestMaker(fullPathFor(s"info/$id"),_.get())(_.validate[Option[AclInfo]])
    )

    def clone(id:AclId): Future[AclIdResponse] = erased(
      jsonRequestMaker(fullPathFor(s"clone/$id"),_.put(JsNull))(_.validate[AclIdResponse])
    )

    private lazy val createPath = fullPathFor("create")
    private lazy val updatePath = fullPathFor("update")
    private lazy val listPath   = fullPathFor("list")

    private def fullPathFor(path: String) = s"$basePath/acl/$path"

  }
}