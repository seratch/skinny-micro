package rl
package tests

import skinny.micro.rl._
import org.specs2.Specification

class UriNormalizationSpec extends Specification {

  def is =
    "Normalizing a uri should" ^
      "lowercase the scheme" ! {
        val uri = Uri("HTTP://localhost:8080")
        uri.normalize.scheme must_== Scheme("http")
      } ^
      "internationalize the domain name with punycode" ! {
        val uri = Uri("http://詹姆斯.org/path/to")
        uri.normalize.authority.get.host.value must_== "xn--8ws00zhy3a.org"
      } ^
      "expand domain, subdomain and public suffix for the host" ! {
        val host = Uri("https://builds.mojolly.com").normalize.authority.get.host
        host must beAnInstanceOf[UriHost with UriHostDomains] and {
          val hwd = host.asInstanceOf[UriHost with UriHostDomains]
          (hwd.domain must_== "mojolly") and (hwd.subdomain must_== "builds") and (hwd.publicSuffix must_== "com")
        }
      } ^
      "remove www from the domain" ! {
        Uri("http://www.example.org/path/to").normalize(true).authority.get.host.value must_== "example.org"
      } ^
      "sort the query string parameters alphabetically" ! {
        val uri = Uri("http://localhost/?zzz=123&kkk=495&www=9485&aaa=958")
        val expected = "?aaa=958&kkk=495&www=9485&zzz=123"
        uri.normalize.query.uriPart must_== expected
      } ^
      "remove common query string parameters" ! {
        val uri = Uri("http://localhost/?zzz=123&kkk=495&www=9485&aaa=958&utm_source=3958")
        val expected = "?aaa=958&kkk=495&www=9485&zzz=123"
        uri.normalize.query.uriPart must_== expected
      } ^ end
}
