package org.scalatra
package servlet

import scala.collection.{ Map => CMap }
import scala.collection.DefaultMap
import scala.collection.JavaConverters._
import scala.io.Source
import java.net.URI
import javax.servlet.http.HttpServletRequest
import java.io.InputStream
import util.{ MultiMap, MultiMapHeadView }
import util.RicherString._

object RichRequest {
  private val cachedBodyKey = "org.scalatra.RichRequest.cachedBody"
}

case class RichRequest(r: HttpServletRequest) extends AttributesMap {
  import RichRequest.cachedBodyKey

  /**
   * The version of the protocol the client used to send the request.
   * Typically this will be something like "HTTP/1.0"  or "HTTP/1.1" and may
   * be used by the application to determine how to treat any HTTP request
   * headers.
   */
  def serverProtocol = r.getProtocol match {
    case "HTTP/1.1" => Http11
    case "HTTP/1.0" => Http10
  }

  def uri = new URI(r.getRequestURL.toString)

  /**
   * Http or Https, depending on the request URL.
   */
  def urlScheme = r.getScheme match {
    case "http" => Http
    case "https" => Https
  }

  /**
   * The HTTP request method, such as GET or POST
   */
  def requestMethod = HttpMethod(r.getMethod)

  // Moved to conform with what similar specs call it
  @deprecated("Use requestMethod", "2.1.0")
  def method = requestMethod

  /**
   * The remainder of the request URL's "path", designating the virtual
   * "location" of the request's target within the application. This may be
   * an empty string, if the request URL targets the application root and
   * does not have a trailing slash.
   */
  def pathInfo: String = Option(r.getPathInfo) getOrElse ""

  /**
   * The initial portion of the request URL's "path" that corresponds to
   * the application object, so that the application knows its virtual
   * "location". This may be an empty string, if the application corresponds
   * to the "root" of the server.
   */
  def scriptName: String = r.getServletPath

  /**
   * The portion of the request URL that follows the ?, if any. May be
   * empty, but is always required!
   */
  def queryString: String = Option(r.getQueryString) getOrElse ""

  /**
   * A Map of the parameters of this request. Parameters are contained in
   * the query string or posted form data.
   */
  def multiParameters: MultiParams = {
    val bodyParams: Map[String, Seq[String]] = {
      if (r.getParameterMap.isEmpty
        && r.getMethod != null
        && !HttpMethod(r.getMethod).isSafe
        && r.getHeader("Content-Type") != null
        && r.getHeader("Content-Type").split(";")(0).equalsIgnoreCase("APPLICATION/X-WWW-FORM-URLENCODED")) {
        rl.MapQueryString.parseString(body)
      } else {
        Map.empty
      }
    }
    // At the very least in jetty 8 we see problems under load related to this
    if (r.getQueryString.nonBlank && r.getParameterMap.isEmpty) {
      val queryStringParams: Map[String, Seq[String]] = rl.MapQueryString.parseString(r.getQueryString)
      queryStringParams ++ bodyParams
    } else {
      val paramMap = r.getParameterMap.asScala.toMap.transform { (k, v) => v: Seq[String] }
      paramMap ++ bodyParams
    }
  }

  object parameters extends MultiMapHeadView[String, String] {
    protected def multiMap = multiParameters
  }

  /**
   * A map of headers.  Multiple header values are separated by a ','
   * character.  The keys of this map are case-insensitive.
   */
  object headers extends DefaultMap[String, String] {
    def get(name: String): Option[String] = Option(r.getHeader(name))

    private[scalatra] def getMulti(key: String): Seq[String] =
      get(key).map(_.split(",").toSeq.map(_.trim)).getOrElse(Seq.empty)

    def iterator: Iterator[(String, String)] =
      r.getHeaderNames.asScala map { name => (name, r.getHeader(name)) }

    // Since Scala 2.13.0-M5
    //// These two methods are not in MapOps so that MapView is not forced to implement them
    //@deprecated("Use - or remove on an immutable Map", "2.13.0")
    //def - (key: K): Map[K, V]
    //@deprecated("Use -- or removeAll on an immutable Map", "2.13.0")
    //def - (key1: K, key2: K, keys: K*): Map[K, V]

    private[this] def headerNames = r.getHeaderNames.asScala

    override def -(key: String): collection.Map[String, String] = {
      headerNames
        .filter(_ != key)
        .map(name => (name -> this(name))).toMap
    }

    override def -(key1: String, key2: String, keys: String*): collection.Map[String, String] = {
      headerNames
        .filter(name => name != key1 && name != key2 && keys.contains(name) == false)
        .map(name => (name -> this(name))).toMap
    }

  }

  def header(name: String): Option[String] =
    Option(r.getHeader(name))

  /**
   * Returns the name of the character encoding of the body, or None if no
   * character encoding is specified.
   */
  def characterEncoding: Option[String] =
    Option(r.getCharacterEncoding)

  def characterEncoding_=(encoding: Option[String]): Unit = {
    r.setCharacterEncoding(encoding getOrElse null)
  }

  /**
   * The content of the Content-Type header, or None if absent.
   */
  def contentType: Option[String] =
    Option(r.getContentType)

  /**
   * Returns the length, in bytes, of the body, or None if not known.
   */
  def contentLength: Option[Long] = r.getContentLength match {
    case -1 => None
    case length => Some(length)
  }

  /**
   * When combined with scriptName, pathInfo, and serverPort, can be used to
   * complete the URL.  Note, however, that the "Host" header, if present,
   * should be used in preference to serverName for reconstructing the request
   * URL.
   */
  def serverName = r.getServerName

  @deprecated(message = "Use HttpServletRequest.serverName instead", since = "2.0.0")
  def host = serverName

  /**
   * When combined with scriptName, pathInfo, and serverName, can be used to
   * complete the URL.  See serverName for more details.
   */
  def serverPort = r.getServerPort

  @deprecated(message = "Use HttpServletRequest.serverPort instead", since = "2.0.0")
  def port = Integer.toString(r.getServerPort)

  /**
   * Optionally returns the HTTP referrer.
   *
   * @return the `Referer` header, or None if not set
   */
  def referrer: Option[String] = r.getHeader("Referer") match {
    case s: String => Some(s)
    case null => None
  }

  @deprecated("Use referrer", "2.0.0")
  def referer: Option[String] = referrer

  /**
   * Caches and returns the body of the response.  The method is idempotent
   * for any given request.  The result is cached in memory regardless of size,
   * so be careful.  Calling this method consumes the request's input stream.
   *
   * @return the message body as a string according to the request's encoding
   * (defult ISO-8859-1).
   */
  def body: String = {
    cachedBody getOrElse {
      val encoding = r.getCharacterEncoding
      val enc = if (encoding == null || encoding.trim.length == 0) {
        if (contentType.exists(_ equalsIgnoreCase "application/json")) "UTF-8" else "ISO-8859-1"
      } else encoding
      val body: String = try {
        Source.fromInputStream(r.getInputStream, enc).mkString
      } catch { case e: java.io.IOException => "" }
      update(cachedBodyKey, body)
      body
    }
  }

  private def cachedBody: Option[String] =
    get(cachedBodyKey).flatMap(_.asInstanceOf[String].blankOption)

  /**
   * Returns true if the request is an AJAX request
   */
  def isAjax: Boolean = r.getHeader("X-Requested-With") != null

  /**
   * Returns true if the request's method is not "safe" per RFC 2616.
   */
  def isWrite: Boolean = !HttpMethod(r.getMethod).isSafe

  /**
   * Returns a map of cookie names to lists of their values.  The default
   * value of the map is the empty sequence.
   */
  def multiCookies: MultiMap = {
    val rr = Option(r.getCookies).getOrElse(Array()).toSeq.
      groupBy { _.getName }.
      transform { case (k, v) => v map { _.getValue } }.
      withDefaultValue(Seq.empty)
    MultiMap(rr)
  }

  /**
   * Returns a map of cookie names to values.  If multiple values are present
   * for a given cookie, the value is the first cookie of that name.
   */
  def cookies: CMap[String, String] = new MultiMapHeadView[String, String] { protected def multiMap = multiCookies }

  protected[scalatra] def attributes = r

  /**
   * The input stream is an InputStream which contains the raw HTTP POST
   * data.  The caller should not close this stream.
   *
   * In contrast to Rack, this stream is not rewindable.
   */
  def inputStream: InputStream = r.getInputStream

  /**
   * The remote address the client is connected from.
   * This takes the load balancing header X-Forwarded-For into account
   * @return the client ip address
   */
  def remoteAddress = header("X-FORWARDED-FOR").flatMap(_.blankOption) getOrElse r.getRemoteAddr

  def locale = r.getLocale

  def locales = r.getLocales

}
