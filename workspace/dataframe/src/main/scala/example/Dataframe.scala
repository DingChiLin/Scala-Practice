package example

import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.SQLContext._
import org.apache.spark.sql
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.functions._
  
import org.apache.spark.rdd.RDD
import org.scalameter._
import scala.collection.mutable.ListBuffer

import org.apache.spark.sql.catalyst.encoders.ExpressionEncoder
import org.apache.spark.sql.Encoder

object DataFrames {

  def main(args: Array[String]) {
    println("DataFrame!")
    
    val conf: SparkConf = new SparkConf().setMaster("local[4]").setAppName("RddTest")
    val sc: SparkContext = new SparkContext(conf)
    
    val spark = SparkSession
      .builder()
      .appName("Spark SQL basic example")
      .config("spark.some.config.option", "some-value")
      .getOrCreate()

    spark.sparkContext.setLogLevel("ERROR")  
    
    import spark.implicits._
    import org.apache.spark.sql.types._

    val rdd = sc.parallelize(List((1, "Arthur", 15, "APP", "new"), (2, "Molly", 20, "WEB", "new"), (3, "Stone", 45, "WEB", "new"), (4, "Keith", 45, "WEB", "old"), (5, "James", 2, "APP", "old")))
    val df = rdd.toDF("id", "name", "age", "team", "seniority") 
    df.createOrReplaceTempView("people")
    
    df.show()
    df.union(df).show()
   
    val rdd2 = sc.parallelize(List((1, "Amy", 150, "APP", "new"), (2, "Molly", 200, "WEB", "new")))
    val df2 = rdd2.toDF("id", "name", "age", "team", "seniority") 
    
//    df2.show()
    val rdd3 = sc.parallelize(List[Person]())
    val df3 = rdd3.toDF("id", "name", "age", "team", "seniority") 
    
    df.join(df2.select("id", "name"), Seq("id", "name")).union(df2).show()
    df3.join(df2.select("id", "name"), Seq("id", "name")).union(df2).show()

 
//    val demoRdd = sc.parallelize(List(("Arthur", "Taiwan"), ("Molly", "US"), ("Stone", "Taiwan"), ("Keith", "US"), ("Kevin", null)))
//    val demoDF = demoRdd.toDF("name", "country")
//
//    val ds = df.as[Person]
//    println(ds)
//    println(ds.groupBy($"team").avg("age").show()) 
//    println(ds.collect()(0).name)
//    
//    import org.apache.spark.sql.expressions.scalalang.typed
//    val result = 
//      ds.groupByKey(x => (x.team, x.seniority))
//        .agg(round(typed.avg[Person](_.id),0).as(Encoders.DOUBLE), round(typed.avg[Person](_.age),0).as(Encoders.DOUBLE))
//        .map{ case ((team, seniority), id, age) => Person(id.toInt, "s", age.toInt, team, seniority) }
//  
//    result.show()
  }
  
  case class Person(
    id: Int, 
    name: String, 
    age: Int, 
    team: String,  
    seniority: String
  )
}