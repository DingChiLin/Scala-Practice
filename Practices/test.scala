object HelloWorld {
  def main(args: Array[String]): Unit = {
    var a = "abcde".take(2)
    println(exec(time()))
    default_value()

    var arr = Array(1,2,3,4,5)
    println(sum(arr))
  }

  def sum(x: Array[Int]) = {
    x.reduce(_ + _)
  }

  def time(): Long = {
      println("In time()")
      System.nanoTime
  }

  def exec(t: => Long): Long = {
      println("Entered exec, calling t ...")
      println("1t = " + t)
      println("Calling t again ...")
      println("2t = " + t)
      t
  }

  def default_value(value: Int = 1) = {
    println(value)
  }

}
