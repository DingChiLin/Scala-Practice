package example

object Hello extends Greeting with App {
  println(greeting)
  
  def test(x: Int): Int = {
    return x*2
  }
}

trait Greeting {
  lazy val greeting: String = "hello"
}
