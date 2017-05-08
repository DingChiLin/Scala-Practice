import scala.collection.mutable._
import org.scalameter._

object mutableBenchmark{
  
  val LOOPTIME = 50000
  
  def main(args: Array[String]){
    println("Mutable Test")

    val arrayBuffer = appendArrayBuffer
    val array = appendArray
    val listBuffer = appendListBuffer
    val list = appendList

    // append
    timed("Array append", appendArrayBuffer)
    timed("ArrayBuffer append", appendArrayBuffer)
    timed("List append", appendListBuffer)
    timed("ListBuffer append", appendList)
    timed("List append recursively", recursivelyAppendList())

//    // update
//    timed("Array update", updateArrBuffer(arrBuffer))
//    timed("Array update", updateArr(arr))
//    timed("List update", updateListBuffer(mList))
    
    // concat

    timed("ListBuffer ++ ListBuffer", listBuffer ++ listBuffer)
    timed("List ++ List", list ++ list)    
    timed("ArrayBuffer ++ ArrayBuffer", arrayBuffer ++ arrayBuffer)
    timed("Array ++ Array", array ++ array)
    
    val smallList = List[Int](1,2,3)    
    timed("small List ++ large List", smallList ++ list)
    timed("large List ++ small List", list ++ smallList)

    val smallArray = Array[Int](1,2,3)
    timed("small Array ++ large Array", smallArray ++ array)
    timed("large Array ++ small Array", array ++ smallArray)
    
    timed("List ++ List ++ List", list ++ list ++ list)
    timed("List ::: List ::: List", list ::: list ::: list)
    
    timed("small List append right", smallList :+ 1) 
    timed("large List append right", list :+ 1)    
    timed("large List append left", 1::list)    

    timed("small Array append right", smallArray :+ 1) 
    timed("large Array append right", array :+ 1)    

  }

  /**
   * Append
   */
  
   

  // spend 3.0E-6 ms
  def appendArrayBuffer():ArrayBuffer[Int] = {
    val arr = ArrayBuffer[Int]()
    for(i <- 0 until LOOPTIME){
      arr += i
    }
    arr
  }

  // spend 3.7E-3 ms
  def appendArray():Array[Int] = {
    var arr = Array[Int]()
    for(i <- 0 until LOOPTIME){
      arr :+= i
    }
    arr
  }

  // spend 3.5E-5 ms
  def appendListBuffer():ListBuffer[Int] = {
    val list = ListBuffer[Int]()
    for(i <- 0 until LOOPTIME){
      list += i
    }
    list
  }

  // spend 0.07 ms
  def appendList():List[Int] = {
    var list = List[Int]()
    for(i <- 0 until LOOPTIME){
      list :+= i
    }
    list
  }
  
  def recursivelyAppendList(i:Int = 0, list:List[Int] = List[Int]()): List[Int] = {
    if(i >= LOOPTIME){
      list
    }else{
      recursivelyAppendList(i+1, i::list)
    }
  }

  /**
   * update
   */

  // spend 1.2E-5 ms
  def updateArrBuffer(arr:ArrayBuffer[Int]){
    for(i <- 0 until arr.size){
      arr(i) = i+1
    }
  }

  def updateArr(arr:Array[Int]){
    for(i <- 0 until arr.size){
      arr(i) = i+1
    }
  }
    
  // update spend 0.018488 ms
  def updateListBuffer(list:ListBuffer[Int]){
    for(i <- 0 until list.size){
      list(i) = i+1
    }
  }



  /**
   * Easy Benchmark Tool
   */

  def timed(label:String, code: => Any){
    val time = config(
      Key.exec.benchRuns -> 10
    ) withWarmer {
      new Warmer.Default
    } measure {
      code
    }
    val ROUND = 1000.0
    val timeString = time.toString
    val roundtime = (timeString.slice(0, timeString.length-3).toDouble*ROUND).round/ROUND
    println(s"$label spend time: $roundtime")
  }

}
