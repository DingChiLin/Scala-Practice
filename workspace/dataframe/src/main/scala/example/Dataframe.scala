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
    
//    println(df.printSchema())
//    println(df.show())
//    println(df.select("name").show())
//    println(df.filter($"age" > 17).show())
//    println(df.filter("age > 17").orderBy($"age", $"name").show())
//    println(df.groupBy($"team".as[String]).avg("age").orderBy($"avg(age)".desc).show())
 
    val demoRdd = sc.parallelize(List(("Arthur", "Taiwan"), ("Molly", "US"), ("Stone", "Taiwan"), ("Keith", "US"), ("Kevin", null)))
    val demoDF = demoRdd.toDF("name", "country")
    
//    println(demoDF.na.drop.show())
//    println(demoDF.na.fill("Taiwan").show())
//    println(demoDF.count)
//    println(demoDF.take(2).toList)
//    println(demoDF.take(2).toList(1)(1).getClass)
//    println(df.take(2).toList)
//    println(df.take(2).toList(0)(0).getClass)
//    println(df.take(2).getClass)
//    println(df.join(demoDF, df("name") === demoDF("name")).show())
    
//    val ds = rdd.toDS
//    println(ds.groupBy($"_4").avg("_3").show()) 
//    println(ds.groupByKey(_._4).mapValues(_._2).reduceGroups(_+_).show())
      
//    println(df.select("name").show())
    
//    val legalProjection: Column = when($"age" === 45, "special").when($"age" >= 18, "adult").when($"age" < 3, "baby").otherwise("children").as("legal")
//    val titleColumns = List(new Column("id"), new Column("age"))
//    val titleProjection: Column = titleColumns.reduce(_+_).divide(3).as("title")
//    println(df.select(legalProjection, titleProjection).show())

      val ds = df.as[Person]
      println(ds)
      println(ds.groupBy($"team").avg("age").show()) 
      println(ds.collect()(0).name)
      
      import org.apache.spark.sql.expressions.scalalang.typed
      val result = 
        ds.groupByKey(x => (x.team, x.seniority))
          .agg(round(typed.avg[Person](_.id),0).as(Encoders.DOUBLE), round(typed.avg[Person](_.age),0).as(Encoders.DOUBLE))
          .map{ case ((team, seniority), id, age) => Person(id.toInt, "s", age.toInt, team, seniority) }
    
      result.show()
    
    
//    import org.apache.spark.sql.expressions.Aggregator
    
    
//    val sqlDF = spark.sql("""SELECT * FROM people WHERE age > 17""")
//    println(sqlDF.show())
//    val sqlDF2 = spark.sql("""SELECT * FROM people WHERE name IS NULL""")
//    println(sqlDF2.show())
  
  }
  
  case class Person(
    id: Int, 
    name: String, 
    age: Int, 
    team: String,  
    seniority: String
  )
}