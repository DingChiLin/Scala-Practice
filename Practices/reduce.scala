object Recude {
  def main(args: Array[String]): Unit = {
    val A = Array(1,2,3,4,5,6,7,8,9,10)
    val add = (x:Int, y:Int) => (x+y)
    val result = myReduce(A, add)
    println(result)
  }

  def myReduce(arr: Array[Int], f: (Int, Int) => Int): Int = {
    if(arr.length == 1){
      arr(0)
    }else{
      val len = arr.length
      var mid = len/2
      f( myReduce(arr.slice(0,mid), f), myReduce(arr.slice(mid, len), f) )
    }
  }

}
