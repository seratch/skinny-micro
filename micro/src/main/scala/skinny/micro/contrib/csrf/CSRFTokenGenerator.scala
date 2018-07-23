package skinny.micro.contrib.csrf

import java.security.SecureRandom

/**
 * CSRF token value generator.
 */
object CSRFTokenGenerator {

  def apply(): String = generateCsrfToken()

  private[this] def hexEncode(bytes: Array[Byte]): String = {
    (bytes.foldLeft(new StringBuilder(bytes.length * 2)) { (sb, b) =>
      if ((b.toInt & 0xff) < 0x10) sb.append("0")
      sb.append(Integer.toString(b.toInt & 0xff, 16))
    }).toString
  }

  protected def generateCsrfToken(): String = {
    val tokenVal = new Array[Byte](20)
    (new SecureRandom).nextBytes(tokenVal)
    hexEncode(tokenVal)
  }

}
