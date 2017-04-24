object Recursive extends App{
  println("Hello")
  //val result = breakLoop(0, 10)
  //println(result)
  val primes = List(2,3,5,7,11,13,17,19,23)
  val maximum = 26
  val result2 = semiPrime(0,1,primes,maximum)
  println(result2)

  def breakLoop(index:Int, n:Int): List[Int] = {
    if(index * index > n){
      List[Int]()
    }else{
      index::breakLoop(index + 1, n)
    }
  }	

  def semiPrime(index1:Int, index2:Int, primes:List[Int], maximum:Int): List[Int] = {
    println("-----")
    println(index1)
    println(index2)
    val result = primes(index1) * primes(index2)
    println(result)
    if(result > maximum && index2 == index1 + 1){
      List[Int]()
    }else{ 
      if(index2 < primes.length - 1 && result <= maximum){
        result::semiPrime(index1, index2 + 1, primes, maximum)
      }else{
        result::semiPrime(index1 + 1, index1 + 2, primes, maximum)
      }
    }
  }
}
