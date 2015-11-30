package consul.v1.common

import consul.PlayApplicationWSProvider
import consul.v1.common.Types.DatacenterId
import consul.v1.ws.WSProvider
import play.api.libs.json._
import play.api.libs.ws.{WS, WSRequestHolder, WSResponse}
import play.api.Play.current
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

object ConsulRequestBasics {

  def jsonRequestMaker[A](path: String, httpFunc: WSRequestHolder => Future[WSResponse])(body: JsValue => A)(implicit executionContext: ExecutionContext, wsProvider: WSProvider): Future[A] = {
    genRequestMaker(path,httpFunc)(_.json)(body)
  }

  def jsonDcRequestMaker[A](path: String, dc:Option[DatacenterId], httpFunc: WSRequestHolder => Future[WSResponse])(body: JsValue => A)(implicit executionContext: ExecutionContext, wsProvider: WSProvider): Future[A] = {
    jsonRequestMaker(path, withDc(httpFunc,dc))(body)
  }

  def responseStatusRequestMaker[A](path: String, httpFunc: WSRequestHolder => Future[WSResponse])(body: Int => A)(implicit executionContext: ExecutionContext, wsProvider: WSProvider): Future[A] = {
    genRequestMaker(path,httpFunc)(_.status)(body)
  }

  def responseStatusDcRequestMaker[A](path: String, dc:Option[DatacenterId], httpFunc: WSRequestHolder => Future[WSResponse])(body: Int => A)(implicit executionContext: ExecutionContext, wsProvider: WSProvider): Future[A] = {
    responseStatusRequestMaker(path, withDc(httpFunc,dc))(body)
  }

  def stringRequestMaker[A](path: String, httpFunc: WSRequestHolder => Future[WSResponse])(body: String => A)(implicit executionContext: ExecutionContext, wsProvider: WSProvider): Future[A] = {
    genRequestMaker(path,httpFunc)(_.body)(body)
  }

  def erased[A](future:Future[JsResult[A]])(implicit executionContext: ExecutionContext): Future[A] = {
    future.flatMap(_ match{
      case err:JsError      => Future.failed(Types.ConsulResponseParseException(err))
      case JsSuccess(res,_) => Future.successful(res)
    })
  }


  private def genRequestMaker[A,B](path: String, httpFunc: WSRequestHolder => Future[WSResponse])(responseTransformer: WSResponse => B)(body: B => A)(implicit executionContext: ExecutionContext, wsProvider: WSProvider): Future[A] = {

    Try(httpFunc(wsProvider.url(path))) match {
      case Failure(exception) => Future.failed(exception)
      case Success(resultF)   => resultF.map( responseTransformer andThen body )
    }
  }

  private def withDc(httpFunc: WSRequestHolder => Future[WSResponse],dc:Option[DatacenterId]) = {
    (req:WSRequestHolder) => httpFunc(dc.map{ case dc => req.withQueryString("dc"->dc.toString) }.getOrElse(req))
  }
}