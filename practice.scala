object HelloWorld {
  def main(args: Array[String]): Unit = {
    var c3 = Note("C", "Quarter", 3)
    println(symbolDuration(c3))

    var r = Rest("Half")
    println(symbolDuration(r))

    var a = 1
    var b = 10
    println(sum( x => x * x, a, b ))

    println(sqrt(2))

    println(Right(1).filterOrElse(x => x % 2 == 0, "Odd value"))

    def triple(x: Int): Int = 3 * x

    def tripleEither(x: Either[String, Int]): Either[String, Int] =
      x.right.map(triple)

    println(tripleEither(Left("not a number")))
    println(Left("not a number").right.map(triple))
    println(Left(1).map(triple))

    //tuple
    val tuple: (String, Int) = ("hello", 1234)
    println(tuple)
    val (s, i) = tuple
    println(s)
    println(tuple._2)
  }

  //function
  def factorial(n: Int): Int = {
    def facIter(n: Int, result: Int): Int = {
      if(n == 1) result
      else facIter(n-1, n*result)
    }
    facIter(n, 1)
  }

  //class
  sealed trait Symbol

  case class Note(
    name: String,
    duration: String,
    octave: Int
  ) extends Symbol

  case class Rest(
    duration: String
  ) extends Symbol

  def symbolDuration(symbol: Symbol): String = {
    symbol match {
      case Note(name, duration, octave) => duration
      case Rest(duration) => duration
    }
  }

  //high order function
  def sum(f: Int => Int, a: Int, b:Int): Int = {
    def loop(x: Int, acc: Int): Int = {
      if (x > b) acc
      else loop(x + 1, acc + f(x))
    }

    loop(a, 0)
  }

  //either
  def sqrt(x: Double): Either[String, Double] =
    if (x < 0) Left("x must be positive")
    else Right(math.sqrt(x))

}
