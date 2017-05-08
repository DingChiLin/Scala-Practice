object Hello extends App {
  println("Hello World!")

  // Synchronization
  var counter = 0
  val x = new AnyRef{}

  val t1 = new HelloThread
  //t1.start()

  val t2 = new HelloThread
  //t2.start()

  class HelloThread extends Thread{
    override def run() = x.synchronized{
      for(i <- 1 to 10){
        counter += 1
        println(counter)
      }
    }
  }

  // Implement Parallel Programming Function
  def say_hello(): Int = {
    var sum = 0
    for(i <- 1 to 10){
      Thread.sleep(100)
      sum += i
      println("Hello: " + sum)
    }
    return sum
  }

  val (resultA, resultB) = parallel(say_hello, say_hello)
  println(resultA)
  println(resultB)

  def parallel[A, B](taskA: => A, taskB: => B): (A, B) = {
    val m1 = new MyThread(taskA)
    val m2 = new MyThread(taskB)
    m1.start()
    m2.start()
    m1.join()
    m2.join()
    return(m1.getValue(), m2.getValue())
  }

}

// Function as Parameters in Thread
class MyThread[T](task: => T) extends Thread {
  var value:T = _
  override def run() = {
    value = task
  }
  def getValue(): T = {
    value
  }
}

