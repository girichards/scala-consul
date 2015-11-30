package consul.v1.catalog

import consul.v1.common.CheckStatus._
import consul.v1.common.ConsulRequestBasics._
import consul.v1.common.Types._
import consul.v1.common.{Node, Types}
import consul.v1.ws.WSProvider
import play.api.http.Status
import play.api.libs.json._
import play.api.libs.ws.WSRequestHolder

import scala.concurrent.{ExecutionContext, Future}

case class Deregisterable(Node:NodeId,ServiceID:Option[ServiceId],CheckID:Option[CheckId],Datacenter:Option[DatacenterId])
case class Service(ID:ServiceId,Service:ServiceType,Tags:Set[ServiceTag],Address:Option[String],Port:Option[Int])
case class Check(Node:NodeId,CheckID:CheckId,Name:String,Notes:Option[String],Status:CheckStatus,ServiceID:Option[ServiceId])
case class Registerable(Node:NodeId,Address:String,Service:Option[Service],Check:Option[Check],Datacenter:Option[DatacenterId])

trait CatalogRequests {
  def register(registerable:Registerable):Future[Boolean]
  def deregister(deregisterable:Deregisterable):Future[Boolean]
  def datacenters(): Future[Seq[DatacenterId]]
  def nodes(dc:Option[DatacenterId]=Option.empty): Future[Seq[Node]]
  def node(nodeID:NodeId,dc:Option[DatacenterId]=Option.empty):Future[NodeProvidedServices]
  def services(dc:Option[DatacenterId]=Option.empty):Future[Map[ServiceType,Set[String]]]
  def service(service:ServiceType,tag:Option[ServiceTag]=Option.empty, dc:Option[DatacenterId]=Option.empty):Future[Seq[NodeProvidingService]]

  /*convenience methods*/
  def deregisterNode(node:NodeId,dc:Option[DatacenterId]): Future[Boolean] =
    deregister(Deregisterable(node,Option.empty,Option.empty,dc))
  def deregisterService(service:ServiceId,node:NodeId,dc:Option[DatacenterId]): Future[Boolean] =
    deregister(Deregisterable(node,Option(service),Option.empty,dc))
  def deregisterCheck(check:CheckId,node:NodeId,dc:Option[DatacenterId]): Future[Boolean] =
    deregister(Deregisterable(node,Option.empty,Option(check),dc))

  def Registerable: (Types.NodeId, String, Option[Service], Option[Check], Option[Types.DatacenterId]) => Registerable =
    consul.v1.catalog.Registerable.apply _
  def Check: (Types.NodeId, Types.CheckId, String, Option[String], CheckStatus, Option[Types.ServiceId]) => Check =
    consul.v1.catalog.Check.apply _
  def Service: (Types.ServiceId, Types.ServiceType, Set[Types.ServiceTag], Option[String], Option[Int]) => Service =
    consul.v1.catalog.Service.apply _
  def Deregisterable = consul.v1.catalog.Deregisterable.apply _

}

object CatalogRequests {

  private implicit lazy val deregisterWrites = Json.writes[Deregisterable]
  private implicit lazy val registerWrites   = {
    implicit val serviceWrites = Json.writes[Service]
    implicit val checkWrites = Json.writes[Check]

    Json.writes[Registerable]
  }

  def apply(basePath: String)(implicit executionContext: ExecutionContext, wsProvider: WSProvider): CatalogRequests = new CatalogRequests {

    def register(registerable: Registerable): Future[Boolean] = responseStatusRequestMaker(
      registerPath,
      _.put( Json.toJson(registerable) )
    )(_ == Status.OK)

    def deregister(deregisterable:Deregisterable):Future[Boolean] = responseStatusRequestMaker(
      deregisterPath,
      _.put(Json.toJson(deregisterable))
    )(_ == Status.OK)

    def nodes(dc:Option[DatacenterId]) = erased(
      jsonDcRequestMaker(fullPathFor("nodes"),dc, _.get())(_.validate[Seq[Node]])
    )

    def node(nodeID: NodeId, dc:Option[DatacenterId]) = erased(
      jsonDcRequestMaker(fullPathFor(s"node/$nodeID"),dc, _.get())(_.validate[NodeProvidedServices])
    )

    def service(service: ServiceType, tag:Option[ServiceTag], dc:Option[DatacenterId]) = erased(
      jsonDcRequestMaker(fullPathFor(s"service/$service"),dc,
        (r:WSRequestHolder) => tag.map{ case tag => r.withQueryString("tag"->tag) }.getOrElse(r).get()
      )(_.validate[Seq[NodeProvidingService]])
    )

    def datacenters(): Future[Seq[DatacenterId]] = erased(
      jsonRequestMaker(datacenterPath, _.get() )(_.validate[Seq[DatacenterId]])
    )

    def services(dc:Option[DatacenterId]=Option.empty): Future[Map[Types.ServiceType, Set[String]]] = erased(
      jsonDcRequestMaker(servicesPath, dc, _.get())(
        _.validate[Map[String,Set[String]]].map(_.map{ case (key,value) => ServiceType(key)->value })
      )
    )

    private lazy val datacenterPath = fullPathFor("datacenters")
    private lazy val servicesPath   = fullPathFor("services")
    private lazy val registerPath   = fullPathFor("register")
    private lazy val deregisterPath = fullPathFor("deregister")

    private def fullPathFor(path: String) = s"$basePath/catalog/$path"

  }

}
