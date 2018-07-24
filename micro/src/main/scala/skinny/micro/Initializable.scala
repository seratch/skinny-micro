package skinny.micro

import scala.language.implicitConversions
import javax.servlet.ServletContext
import skinny.micro.context.ThinServletBaseConfig

import scala.collection.mutable

/**
 * Trait representing an object that can't be fully initialized by its
 * constructor.  Useful for unifying the initialization process of an
 * HttpServlet and a Filter.
 */
trait Initializable {

  trait Config {
    def context: ServletContext
    def initParameters: mutable.Map[String, String]
  }
  protected implicit def configWrapper(config: ThinServletBaseConfig): Config

  /**
   * A hook to initialize the class with some configuration after it has
   * been constructed.
   *
   * Not called init because GenericServlet doesn't override it, and then
   * we get into https://lampsvn.epfl.ch/trac/scala/ticket/2497.
   */
  def initialize(config: ThinServletBaseConfig): Unit

  /**
   * A hook to shutdown the class.  Bridges the gap between servlet's
   * destroy and filter's destroy.
   */
  protected def shutdown(): Unit = {}

}
