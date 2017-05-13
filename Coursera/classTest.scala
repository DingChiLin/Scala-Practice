object ClassTest{
  def main(args: Array[String]){
    println("Class Test")
    val test = new Test(3,2)
    test.hi

    val sub = new Sub()
    println(sub.bar)
    sub.bar2
    sub.sth

    println(sub.a)
    println(sub.a)
    println(sub.b)
    println(sub.b)
    println(sub.c)
    println(sub.c)
  }
}

class Test(x:Int, y:Int){
  def hi(){
    println(x+y)
  }
}

class Sub extends Base{
  val a = 1
  lazy val b = {println("lazy");bar()}
  def c = {println("def");bar()}

  def bar() = {
    fooo
  }

  def bar2() {
    println("bar2:" + foo)
  }

  override def sth = { 
    println("ssssssth!")
  }
}

abstract class Base {
  val fooo = 3
  def foo = 1
  def bar: Int
  def sth = {
    println("something")
  }
}
