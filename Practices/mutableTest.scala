import scala.collection.mutable._

object MutableTest{
  def main(args: Array[String]){
    println("Mutable Test")

    val mArr = mutableArr
    println(mArr.size)
    val mList = mutableList
    println(mList.size)

    //timed("mutableArr append", mutableArr)
    //timed("immutableArr append", immutableArr)
    //timed("mutableList append", mutableList)
    //timed("immutableList append", immutableList)

    timed("mutableArr update", updateArr(mArr))
    timed("mutableList update", updateList(mList))

  }

  /**
   * Append
   */

  // spend 3.0E-6 ms
  def mutableArr():ArrayBuffer[Int] = {
    val arr = ArrayBuffer[Int]()
    for(i <- 0 to 100000){
      arr += i
    }
    arr
  }

  // spend 3.7E-3 ms
  def immutableArr():Array[Int] = {
    var arr = Array[Int]()
    for(i <- 0 to 100000){
      arr :+= i
    }
    arr
  }

  // spend 3.5E-5 ms
  def mutableList():ListBuffer[Int] = {
    val list = ListBuffer[Int]()
    for(i <- 0 to 100000){
      list += i
    }
    list
  }

  // spend 0.07 ms
  def immutableList():List[Int] = {
    var list = List[Int]()
    for(i <- 0 to 100000){
      list :+= i
    }
    list
  }

  /**
   * update
   */

  // spend 1.2E-5 ms
  def updateArr(arr:ArrayBuffer[Int]){
    for(i <- 0 until arr.size){
      arr(i) = i+1
    }
  }

  // update spend 0.018488 ms
  def updateList(list:ListBuffer[Int]){
    for(i <- 0 until list.size){
      list(i) = i+1
    }
  }



  /**
   * Easy Benchmark Tool
   */

  def timed(label:String, code: => Any){
    val start = System.currentTimeMillis
    code
    val time = (System.currentTimeMillis - start)

    println(s"$label spend $time ms")
  }

}
