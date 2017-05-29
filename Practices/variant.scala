object Variant{


  def main(args: Array[String]){
    val d = new Dog()
    val a:Animal = d
    d.bar
    a.bar

    val l = (new MyList[String]("str"))
    l.print
    val l2 = (new MyList[Int](555))
    l2.print

    //val l3 = l2.update(666)
    //l3.print

    //val l3 = (new MyList[Bird](new Bird))
    //println(l3.getClass)
    //l3.print

    //val l4: MyList[Animal] = l3
    //println(l4.getClass)
    //l4.print
  }

  class MyList[+T](_e:T){
    val e = List[T](_e)
    //def update[T](x:T) = {
      //new MyList[T](x)
    //}
    def print = { println(e) }
  }




  class Animal{
    def bar = {println("Aniaml bar")}
    override def toString = {"Animal"}
  }

  class Dog extends Animal{
    override def bar = {println("Dog bar")}
    override def toString = {"Dog"}
  }

  class Bird extends Animal{
    override def bar = {println("Bird bar")}
    override def toString = {"BIRD"}
  }
}
