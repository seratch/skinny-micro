# Skinny Micro [![Build Status](https://travis-ci.org/skinny-framework/skinny-micro.svg)](https://travis-ci.org/skinny-framework/skinny-micro)

Skinny Micro is at once a micro Web framework to build Servlet applications in Scala and the core part of [Skinny Framework](http://skinny-framework.org/) 2.

Skinny Micro started as a fork of [Scalatra](http://scalatra.org/). After that, many improvements have been made to be safer and more efficient when working with Scala Future values upon it.

Basically, Skinny Micro's DSLs are source compatible with Scalatra 2.3's ones. But names of base traits and packages are mostly renamed and the structure of internal modules are re-designed.

## Getting Started

```scala
lazy val skinnyMicroVersion = "0.9.5"

libraryDependencies ++= Seq(
  // micro Web framework
  "org.skinny-framework" %% "skinny-micro"         % skinnyMicroVersion,
  // json4s integration for skinny-micro
  "org.skinny-framework" %% "skinny-micro-json"    % skinnyMicroVersion,
  // Scalate integration for skinny-micro
  "org.skinny-framework" %% "skinny-micro-scalate" % skinnyMicroVersion,
  // Standalone Web server (Jetty 9.2 / Servlet 3.1)
  "org.skinny-framework" %% "skinny-micro-server"  % skinnyMicroVersion
)
```

## Minimum Examples

We'd love to show you some simple but working examples briefly.

Please also see more examples under [samples](https://github.com/skinny-framework/skinny-micro/tree/master/samples) and [scalas-samples](https://github.com/skinny-framework/skinny-micro/tree/master/scalas-samples).

### Simple Application

The following is a minimum simple Servlet example. `skinny.micro.SkinnyListener` initializes Skinny Micro's environment.

As same as Scalatra, `_root_.Bootstrap` class (instead of `ScalatraBootstrap` for Scalatra) is detected by default. Of course, you can change the name of the Bootstrap class by configuring with an init parameter.

Also take a look at [sbt-servlet-plugin](https://github.com/skinny-framework/sbt-servlet-plugin). The plugin will help you much when building Skinny applications.

See [samples](https://github.com/skinny-framework/skinny-micro/tree/master/samples) too for details.

#### src/main/scala/app.scala

```scala
import javax.servlet._
import skinny.micro._

object Hello extends WebApp {
  get("/say-hello") {
    s"Hello, ${params.getOrElse("name", "Anonymous")}!\n"
  }
}

class Bootstrap extends LifeCycle {
  override def init(ctx: ServletContext) {
    Hello.mount(ctx)
  }
}
```

#### src/main/webapp/WEB-INF/web.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<web-app xmlns="http://java.sun.com/xml/ns/javaee"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://java.sun.com/xml/ns/javaee http://java.sun.com/xml/ns/javaee/web-app_3_0.xsd"
         version="3.0">
    <listener>
        <listener-class>skinny.micro.SkinnyListener</listener-class>
    </listener>
</web-app>
```

### Async Native Application

Skinny Micro newly privides original base traits that named as `AsyncWebApp (AsyncSkinnyMicorFilter)` and `AsyncSingleWebApp (AsyncSkinnyMicroServlet)`.

They are natively suitable for building Future-wired async operations. You will no longer unwantedly feel stressed when working with Future-wired operations.

```scala
case class Message(id: Long, text: String)

object Messages {
  def search(keyword: Option[String])(implicit ctx: ExecutionContext): Future[Seq[Message]]
}

object AsyncMessagesApp extends AsyncWebApp with DefaultJSONStringOps {

  post("/messages/search") { implicit ctx =>
    // You don't need to explicitly wrap results with AsyncResult
    // Of course, doing so is also fine
    Messages.search(params.get("keyword"))
      .map(ms => Ok(toJSONString(ms))) // returns Future[ActionResult]
  }
}
```

### Scalas Example

By using scalas, script runnner from sbt, you can easily run small Scala applications.

http://www.scala-sbt.org/0.13/docs/Scripts.html

```scala
#!/usr/bin/env scalas
// or ./scalas app.scala
/***
scalaVersion := "2.11.7"
resolvers += "sonatype releases" at "https://oss.sonatype.org/content/repositories/releases"
libraryDependencies += "org.skinny-framework" %% "skinny-micro-server" % "0.9.5"
*/
import skinny.micro._
object HelloApp extends WebApp {
  get("/say-hello") {
    s"Hello, ${params.getOrElse("name", "Anonymous")}!\n"
  }
}
WebServer.mount(HelloApp).port(4567).start()

println
println("Try: curl -v 'localhost:4567/say-hello?name=Martin'")
println
```

### Other Examples

More working examples are available under [samples](https://github.com/skinny-framework/skinny-micro/tree/master/samples) and [scalas-samples](https://github.com/skinny-framework/skinny-micro/tree/master/scalas-samples).

## License

(The MIT License)

Copyright (c) skinny-framework.org

