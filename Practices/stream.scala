object HelloWorld {
  def main(args: Array[String]): Unit = {
    var rec = 0
    def streamRange(lo: Int, hi: Int): Stream[Int] = {
      println("hi stream")
      println(lo)
      println(hi)
      rec = rec + 1
      if (lo >= hi) Stream.empty
      else Stream.cons(lo, streamRange(lo + 1, hi))
    }
    streamRange(1, 10)
    println(rec)
  }


}
