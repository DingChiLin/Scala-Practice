import java.io._

object HelloWorld extends App{
  println("Hello World App")
  val foo = new Foo("Hello")
  // baz field is only calculated once
  println(foo.baz)
  println(foo.baz)
  foo.test


  val bo = new ByteArrayOutputStream
  val o = new ObjectOutputStream(bo)
  o.writeObject(foo)
  val bytes = bo.toByteArray

  // Deserialize foo
  val bi = new ByteArrayInputStream(bytes)
  val i = new ObjectInputStream(bi)
  val foo2 = i.readObject.asInstanceOf[Foo]

  println(foo2.baz)
  
  foo2.test
  
  
}


class Foo(val bar: String) extends Serializable {
  @transient lazy val baz: String = {
    println("Calculate baz")
    bar + " world"
  }

  def test(){
    println("HELLOHELLO")
  }
}


