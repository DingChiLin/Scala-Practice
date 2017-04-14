import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import org.apache.spark.rdd.RDD
import org.scalameter._

object Main {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(225); 
  println("Welcome to the Scala worksheet");$skip(87); 
  
  val conf: SparkConf = new SparkConf().setMaster("local[4]").setAppName("RddTest");System.out.println("""conf  : org.apache.spark.SparkConf = """ + $show(conf ));$skip(48); 
  val sc: SparkContext = new SparkContext(conf);System.out.println("""sc  : org.apache.spark.SparkContext = """ + $show(sc ));$skip(65); 
    
  val list1 = Array((1, "a"), (1, "z"), (2, "b"), (3, "c"));System.out.println("""list1  : Array[(Int, String)] = """ + $show(list1 ));$skip(27); val res$0 = 
  list1.groupBy(x => x._1);System.out.println("""res0: scala.collection.immutable.Map[Int,Array[(Int, String)]] = """ + $show(res$0));$skip(80); 
                                            
  val rdd1 = sc.parallelize(list1);System.out.println("""rdd1  : org.apache.spark.rdd.RDD[(Int, String)] = """ + $show(rdd1 ));$skip(35); val res$1 = 
  rdd1.groupByKey().collect.toList;System.out.println("""res1: List[(Int, Iterable[String])] = """ + $show(res$1));$skip(64); val res$2 = 
                                                  
  rdd1.count;System.out.println("""res2: Long = """ + $show(res$2))}
}
