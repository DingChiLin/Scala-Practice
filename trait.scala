object HelloWorld {
  def main(args: Array[String]): Unit = {
    val hello = new Hello
    hello.print
    hello.p
    hello.hello
    hello.hi
    hello.func_a

    val a:A = hello
    a.print()
    a.func_a
  }

  trait A{
    def print(){
      println("test A")
    }

    def func_a
  }

  trait B{
    def p(){
      println("test B")
    }
  }

  class Hi{
    def hi(){
      println("Hi")
    }
  }

  class Hello extends Hi with A with B{
    def hello(){
      println("Hello!")
    }

    def func_a(){
      println("override A")
    }
  }
}
