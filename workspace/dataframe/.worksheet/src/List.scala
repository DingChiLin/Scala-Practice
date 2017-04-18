import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import org.apache.spark.rdd.RDD
import org.scalameter._

object List {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(225); 
  println("Welcome to the Scala worksheet");$skip(148); 
  
	//val conf: SparkConf = new SparkConf().setMaster("local[4]").setAppName("RddTest")
	//val sc: SparkContext = new SparkContext(conf)
	val x = 1;System.out.println("""x  : Int = """ + $show(x ));$skip(24); 
	val list = List(1,2,3);System.out.println("""list  : <error> = """ + $show(list ))}
	//val list1 = List((1, "a"), (1, "z"), (2, "b"), (3, "c"))
	//val rdd1 = sc.parallelize(list1)
	//println(rdd1.groupByKey().collect.toList)
	//println(list1.groupBy(x => x._1))
}
