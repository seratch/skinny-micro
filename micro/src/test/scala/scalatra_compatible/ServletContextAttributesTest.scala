package scalatra_compatible

import org.scalatra.test.scalatest.ScalatraFunSuite

class ServletContextAttributesTest extends ScalatraFunSuite with AttributesTest {
  addServlet(new AttributesServlet {
    def attributesMap = servletContext
  }, "/*")
}

