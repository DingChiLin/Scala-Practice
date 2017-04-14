package example

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import org.apache.spark.rdd.RDD
import org.scalameter._
import scala.collection.mutable.ListBuffer
 
object Lists {

  def main(args: Array[String]) {

    val conf: SparkConf = new SparkConf().setMaster("local[4]").setAppName("RddTest")
    val sc: SparkContext = new SparkContext(conf)   
    
//    var rdd = sc.parallelize(List(1,1,2,3)) 
//    val rdd2 = rdd.map((_, "b"))
//    val rdd3 = rdd.map((_, "c"))
//    println(rdd2.cogroup(rdd3).collect.toList)  
//    println(rdd2.groupWith(rdd3).collect.toList)
    
    val list1 = List((1, "a"), (1, "z"), (2, "b"), (3, "c"), (4, "d"))
    val rdd1 = sc.parallelize(list1).filter(x => x._1 < 4)
    println(list1.foldLeft(0){(sum, x) => sum + x._1})
    println(rdd1.groupByKey().collect.toList)  // List((1,CompactBuffer(a, z)), (2,CompactBuffer(b)), (3,CompactBuffer(c)))
    println(list1.groupBy(x => x._1))  // Map(2 -> List((2,b)), 1 -> List((1,a), (1,z)), 3 -> List((3,c)))
        
    val list2 = List((1, 33), (1, 77), (3, 88))
    val rdd2 = sc.parallelize(list2)
    
    val rdd3 = rdd2.map(x => (x._1, x._2*x._2 ))
    val rdd4 = rdd1.join(rdd3)
    val rdd = rdd4.groupBy(x => x._1)
    println(rdd.collect.toList)
    println(rdd.dependencies)
    println(rdd.toDebugString)
//    println(rdd2.collectAsMap)
    
//    val longArr = sc.parallelize((1 to 1000000))
//    val time = config() withWarmer{new Warmer.Default} measure {
//        val result = longArr.map(x => x*2).collect.toList
//        //println(result) 
//    }
//    println(time)
    
//    val arr = Array(6,7,8,9,10)
//    val scArr = sc.parallelize((0 to 4))
//    scArr.foreach{
//      x => arr(x) = x; println(x)
//    }
//    println(arr.toList)
//    

  }
  
  /**
   * This method computes the sum of all elements in the list xs. There are
   * multiple techniques that can be used for implementing this method, and
   * you will learn during the class.
   *
   * For this example assignment you can use the following methods in class
   * `List`:
   *
   *  - `xs.isEmpty: Boolean` returns `true` if the list `xs` is empty
   *  - `xs.head: Int` returns the head element of the list `xs`. If the list
   *    is empty an exception is thrown
   *  - `xs.tail: List[Int]` returns the tail of the list `xs`, i.e. the the
   *    list `xs` without its `head` element
   *
   *  ''Hint:'' instead of writing a `for` or `while` loop, think of a recursive
   *  solution.
   *
   * @param xs A list of natural numbers
   * @return The sum of all elements in `xs`
   */
    def sum(xs: List[Int]): Int = xs.sum
  
  /**
   * This method returns the largest element in a list of integers. If the
   * list `xs` is empty it throws a `java.util.NoSuchElementException`.
   *
   * You can use the same methods of the class `List` as mentioned above.
   *
   * ''Hint:'' Again, think of a recursive solution instead of using looping
   * constructs. You might need to define an auxiliary method.
   *
   * @param xs A list of natural numbers
   * @return The largest element in `xs`
   * @throws java.util.NoSuchElementException if `xs` is an empty list
   */
    def max(xs: List[Int]): Int = xs.max
  }
