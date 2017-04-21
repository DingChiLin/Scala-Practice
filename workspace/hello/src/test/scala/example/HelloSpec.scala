package example

import org.scalatest._

class HelloSpec extends FlatSpec with Matchers {
  "The Hello object" should "say hello" in {
    Hello.greeting shouldEqual "hello"
  }
  
  "Test" should "pass" in {
    Hello.test(2) shouldEqual 4
  }
}
