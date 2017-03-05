object HelloWorld {
  def main(args: Array[String]): Unit = {
    var a = "abcde".take(2)
    println(exec(time()))
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

}
