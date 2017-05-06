object HelloWorld {
  def main(args: Array[String]): Unit = {
    val (h1, h2) = parallel(hello(), hello())
  }

  def hello(): String = {
    for( i <- 1 to 10){
      println("Hello: " + i)
    }
    return "HELLO"
  }

  def parallel[A, B](taskA: A, taskB: B): (A, B) = {
    val tA = new MyThread(taskA)
    val tB = new MyThread(taskB)

    class MyThread extends Thread{
      override def run(){
        task()
      }
    }

    val resultA = tA.start()
    val resultB = tB.start()
    (resultA, resultB)
  }
}


