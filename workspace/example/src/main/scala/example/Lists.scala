package example

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import org.apache.spark.rdd.RDD

object Lists {

  def main(args: Array[String]) {

    val conf: SparkConf = new SparkConf().setMaster("local[4]").setAppName("RddTest")
    val sc: SparkContext = new SparkContext(conf)   

    val list1 = List((1, "a"), (1, "z"), (2, "b"), (3, "c"))
    val rdd1 = sc.parallelize(list1)

    val list2 = List((1, 33), (1, 77), (3, 88))
    val rdd2 = sc.parallelize(list2)
    
    val rdd = rdd1.join(rdd2)
    println(rdd.collect().toList)

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
