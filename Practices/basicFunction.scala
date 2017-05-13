object BasicFunction extends App{

  // weak 2 - 1 

  def product(x:Int, y:Int):Int = {if(x>y) 1 else x*product(x+1, y)}
  println(product(3,5))
  def factorial(x:Int) = product(1, x)
  val factorial2:(Int => Int) = product(1, _) // cool!
  println(factorial(5))
  println(factorial2(5))

  def sum(f: Int => Int)(x:Int, y:Int):Int = {if(x>y) 0 else f(x) + sum(f)(x+1, y)}
  println(sum(x => x*x)(3,8))

  def tailsum(f:Int => Int)(x:Int, y:Int):Int = {
    def loop(a:Int, acc:Int):Int = {
      if(a > y){
        return acc
      }else{
        loop(a+1, acc + f(a))
      }
    }
    loop(x, 0)
  }
  println(tailsum(x => x*x)(3,8))

  def shortSum(f:Int => Int): (Int, Int) => Int = {
    def sum(a:Int, b:Int):Int = {
      if (a>b) 0 else f(a) + sum(a+1, b)
    }
    sum
  }

  def sumSquare = shortSum(x => x*x)
  println(sumSquare(3,7))

  // weak 2 - 2

  println(mapReduce(x => x*x, (x,y) => x+y, 0)(1,5))
  def product(f:Int => Int)(a:Int, b:Int):Int = mapReduce(f, (x, y) => x*y, 1)(a, b)
  println(product(x => x*x)(1,5))

  def fact(n:Int) = product(x => x)(1, n)
  println(fact(5))

  def mapReduce(f: Int => Int, combine: (Int, Int) => Int, zero:Int)(a:Int, b:Int):Int = {
    if(a>b) zero
    else combine(f(a), mapReduce(f, combine, zero)(a+1, b))
  }

  // weak 2 - 3
  
  def averageDamp(f:Double => Double)(x: Double):Double = (x + f(x)) / 2
  def s(x:Double): Double => Double = {
    averageDamp(y => x/y)
  }
  println(s(10)(5))

  

}
