object Hello extends App {
  println("Hello World!")

  var counter = 0
  val x = new AnyRef{}

  val t1 = new HelloThread
  t1.start()

  val t2 = new HelloThread
  t2.start()

  class HelloThread extends Thread{
    override def run() = x.synchronized{
      for(i <- 1 to 10){
        counter += 1
        println(counter)
      }
    }
  }

}

