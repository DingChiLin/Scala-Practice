import scala.collection.mutable._
import org.scalameter._
import scala.util.Random

object mutableBenchmark{
  
  val LOOPTIME = 50000
  
  def main(args: Array[String]){
    println("Mutable Test")

    val arrayBuffer = appendArrayBuffer
    val array = appendArray
    val listBuffer = appendListBuffer
    val list = appendList
    val vector = appendVector

    // append
//    timed("Array append", appendArray)
//    timed("ArrayBuffer append", appendArrayBuffer)
////    timed("List append", appendList)
//    timed("ListBuffer append", appendListBuffer)
//    timed("Vector append", appendVector)
//    timed("List append recursively", recursivelyAppendList())
//    timed("Vector append recursively", recursivelyAppendVector())


//    // update
//    timed("Array update", updateArrBuffer(arrBuffer))
//    timed("Array update", updateArr(arr))
//    timed("List update", updateListBuffer(mList))
    
    // concat

    timed("ListBuffer ++ ListBuffer", listBuffer ++ listBuffer)
    timed("List ++ List", list ++ list)    
    timed("ArrayBuffer ++ ArrayBuffer", arrayBuffer ++ arrayBuffer)
    timed("Array ++ Array", array ++ array)
    timed("Vector ++ Vector" , vector ++ vector)
    
    val smallList = List[Int](1,2,3)    
    timed("small List ++ large List", smallList ++ list)
    timed("large List ++ small List", list ++ smallList)

    val smallArray = Array[Int](1,2,3)
    timed("small Array ++ large Array", smallArray ++ array)
    timed("large Array ++ small Array", array ++ smallArray)
    
    val smallVector = Vector[Int](1,2,3)
    timed("small Vector ++ large Vector", smallVector ++ vector)
    timed("large Vector ++ small Vector", vector ++ smallVector)   
    
//    timed("List ++ List ++ List", list ++ list ++ list)
//    timed("List ::: List ::: List", list ::: list ::: list)
//    
    timed("small List append right", smallList :+ 1) 
    timed("large List append right", list :+ 1)    
    timed("large List append left", 1::list)    
   
    timed("small Array append right", smallArray :+ 1) 
    timed("large Array append right", array :+ 1) 
    timed("large Array append left", 1 +: array) 

    timed("large Vector append right", vector :+ 1)    
    timed("large Vector prepend left", 1 +: vector)
    
    // Random Access Elements
//    timed("random access list", randomAccessList(list))
//    timed("random access array", randomAccessArray(array))
//    timed("random access vector", randomAccessVector(vector))
    
    // sort
    timed("array sort", array.sorted)
    timed("list sort", list.sorted)
    timed("vector sort", vector.sorted)
    

  }

  /**
   * Append
   */
   
  def appendArrayBuffer():ArrayBuffer[Int] = {
    val arr = ArrayBuffer[Int]()
    for(i <- 0 until LOOPTIME){
      arr += i
    }
    arr
  }

  def appendArray():Array[Int] = {
    var arr = Array[Int]()
    for(i <- 0 until LOOPTIME){
      arr :+= i
    }
    arr
  }

  def appendListBuffer():ListBuffer[Int] = {
    val list = ListBuffer[Int]()
    for(i <- 0 until LOOPTIME){
      list += i
    }
    list
  }

  def appendList():List[Int] = {
    var list = List[Int]()
    for(i <- 0 until LOOPTIME){
      list :+= i
    }
    list
  }
  
  def appendVector():Vector[Int] = {
     var vector = Vector[Int]()
     for(i <- 0 until LOOPTIME){
       vector :+= i
     }
     vector
  }
  
  def recursivelyAppendList(i:Int = 0, list:List[Int] = List[Int]()): List[Int] = {
    if(i >= LOOPTIME){
      list
    }else{
      recursivelyAppendList(i+1, i::list)
    }
  }
  
  def recursivelyAppendVector(i:Int = 0, vector:Vector[Int] = Vector[Int]()): Vector[Int] = {
    if(i >= LOOPTIME){
      vector
    }else{
      recursivelyAppendVector(i+1, i +: vector)
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
   * random access elements
   */
  
   def randomAccessList(list:List[Int]){
     val size = list.size
     for(i <- 0 to size){ 
       list(Random.nextInt(size))
     }
   }

   def randomAccessArray(array:Array[Int]){
     val size = array.size
     for(i <- 0 to size){ 
       array(Random.nextInt(size))
     }
   }
   
   def randomAccessVector(vector:Vector[Int]){
     val size = vector.size
     for(i <- 0 to size){ 
       vector(Random.nextInt(size))
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
    val ROUND = 10000.0
    val timeString = time.toString
    val roundtime = (timeString.slice(0, timeString.length-3).toDouble*ROUND).round/ROUND
    println(s"$label spend time: $roundtime")
  }

}
