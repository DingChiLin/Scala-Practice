object ClassPractice {
  def main(args: Array[String]): Unit = {
    val r1 = new Rational(2,3)
    val r2 = new Rational(5,6)
    val r3 = r1 + r2
    println(r3.numer)
    println(r3.denom)

    val r4 = new Rational(2,3)
    val r5 = r1
    println(r1 == r4) // false
    println(r1 == r5) // true

    val h1 = new Hello()
    val h2 = new Hello()
    println(h1 == h2) //false

    val b = new Sub()
    println(b.foo)

    // can rewrite
    var a = 3
    a = 2
    println(a)
  }

  class Hello(){
    def hi(){
      println("hi")
    }
  }

  class Rational(x: Int, y: Int){
    require(y > 0, "denominator should be positive")
    private def gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
    private val g = gcd(x,y)
    val numer = x / g
    val denom = y / g
    def + (r: Rational): Rational = {
      val n = (r.numer * denom + r.denom * numer)
      val d = r.denom * denom
      return new Rational(n, d)
    }
  }

  abstract class Base {
    def foo = 1
    def bar: Int
  }

  class Sub extends Base {
    override def foo = 2
    def bar = 3
  }

}
