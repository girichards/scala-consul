package consul.v1.ws

import play.api.libs.ws.{WS, WSRequestHolder}

trait WSProvider {
  def url(path: String): WSRequestHolder
}

object PlayApplicationWSProvider extends WSProvider {

  import play.api.Play.current

  override def url(path: String): WSRequestHolder = WS.url(path)
}

object GeneralWSProvider extends WSProvider {

  lazy val builder = new com.ning.http.client.AsyncHttpClientConfig.Builder()
  lazy val client = new play.api.libs.ws.ning.NingWSClient(builder.build())

  override def url(path: String): WSRequestHolder = client.url(path)

}