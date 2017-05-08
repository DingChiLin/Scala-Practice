package example

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._

import java.io.File
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.rdd.RDD  //can skip if not use
import scala.util.matching.Regex
//import com.github.nscala_time.time.Imports._
import org.scalameter._
import scala.util.parsing.json._
//import play.api.libs.json._
//import net.liftweb.json._

import java.sql.DriverManager
import java.sql.Connection

import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.expressions.Aggregator
import org.apache.spark.sql.expressions.scalalang.typed
import org.apache.spark.sql.Encoder

object Hello{

  val INVALID_GEO_ID = -321
  val GENERAL_REQUIRED_KEYS = List("type", "time")
    
  case class Data(crystal_id:String, device_id:String, event_type:String, st: Long, time:Long, geo_id:Long, props:scala.collection.Map[String, String])
  case class AdreqData(crystal_id:String, geo_id:Long, placement:String, request_count:Int, result_count:Int)

  def main(args: Array[String]) {
    
    val spark: SparkSession =
      SparkSession
        .builder()
        .appName("Time Usage")
//        .config("spark.master", "local[4]")
        .getOrCreate()
      
    spark.sparkContext.setLogLevel("ERROR")     
    import spark.implicits._
     
    println("Hello") 
//    val source_bucket = "/Users/arthurlin/Desktop/aws_s3_files" 
    val source_bucket = "s3://ce-production-raw-event-logs/ADREQ"
    val current_time = "year=2017/month=04/day=20/hour=01" //new DateTime("2017-04-20T00:34:56").minute(0).second(0) //TODO:should be today
    
    val data = loadDFByHour(spark, current_time, source_bucket).get
    data.show()
//    timed("loadDFData", data.count)
        
    val ds = data.as[Data]
    ds.cache
    ds.count
    bench("final result ds using string", generate_adreq_analytic_result_ds_string(spark, ds).count, 2)


//    println(ds.count)
//    
//    timed("final result ds using object", generate_adreq_analytic_result(spark, ds).count)
//    timed("final result ds using string", generate_adreq_analytic_result_ds_string(spark, ds).count)
   
    val rdd = ds.rdd.cache//.map(x => convert_to_pure_string(x)).cache // convert to pure string to do benchmark
    rdd.count
//    timed("final result rdd", generate_adreq_analytic_result_rdd(rdd).count)
    bench("final result rdd", generate_adreq_analytic_result_rdd(rdd).count, 2)
//    bench("final result ds using object", generate_adreq_analytic_result(spark, ds).count, 2)


  }
  
  def convert_to_pure_string(data: Data):String = {
    try{
      (data.crystal_id + "|" + data.geo_id + "|" + data.event_type + "|" + scala.util.parsing.json.JSONObject(data.props.toMap)).replace("\\","").replace("\"{", "{").replace("}\"","}").replace("\"[", "[").replace("]\"","]")
    }catch{
      case e:Exception => ""
    }
  }
  
  // RDD 
//  def generate_adreq_analytic_result_rdd2(data: RDD[String]):RDD[(String, (Int, Int))] = {
//    
//    val adreq_initial_state = (0,0) 
//    def _merge_adreq_stat(stat1:(Int, Int), stat2:(Int, Int)): (Int, Int) = {
//      (stat1._1 + stat2._1, stat1._2 + stat2._2)
//    }
//    
//    def _update_adreq_stat(stat:(Int, Int), r:String): (Int, Int) = {
//      val results = r.split("""\|""")
//      val req_or_res = results(0)
//      (stat._1 + results(1).toInt, stat._2)
//    }
//    
//    val adreq = data
//      .filter(_.contains("ad_request"))
//      .flatMap(decode_adreq_type_rdd)
//      .aggregateByKey(adreq_initial_state)(_update_adreq_stat, _merge_adreq_stat)
//      
//    val adreq2 = adreq.union(adreq)
//    val result = adreq2.reduceByKey(_merge_adreq_stat)
//      
//    return result
//  }
  
  def generate_adreq_analytic_result_rdd(data: RDD[Data]):RDD[(String, (Int, Int))] = {
    
    val adreq_initial_state = (0,0) 
    def _merge_adreq_stat(stat1:(Int, Int), stat2:(Int, Int)): (Int, Int) = {
      (stat1._1 + stat2._1, stat1._2 + stat2._2)
    }
    
    def _update_adreq_stat(stat:(Int, Int), r:String): (Int, Int) = {
      val results = r.split("""\|""")
      val req_or_res = results(0)
      (stat._1 + results(1).toInt, stat._2)
    }
    
    val adreq = data
      .filter(_.event_type == "ad_request")
      .flatMap(decode_adreq_type_rdd)
      .aggregateByKey(adreq_initial_state)(_update_adreq_stat, _merge_adreq_stat)
      
    val adreq2 = adreq.union(adreq)
    val result = adreq2.reduceByKey(_merge_adreq_stat)
      
    return result
  }
  
  def decode_adreq_type_rdd(data:Data):List[(String, String)] = {
    val crystal_id = data.crystal_id
    val geo_id = data.geo_id.toInt - (data.geo_id % 1000000)
//    val requests = (Json.parse(values(3)) \ "requests").asOpt[Map[String, Map[String, List[String]]]].getOrElse( Map("STREAM_C_ROADBLOCK_2" -> Map("1" -> "3"))  )    
    val requests = JSON.parseFull(data.props("requests")).get.asInstanceOf[Map[String, Map[String, Any]]]
    return genRequestListRdd(requests, requests.keys, crystal_id, geo_id.toString)
  }
  
  def genRequestListRdd(requests:Map[String, Map[String, Any]], placements:Iterable[String], crystal_id:String, geo_id:String):List[(String, String)] = {
    if(placements.size == 0){
      List[(String, String)]()
    }else{
      val placement = placements.head
      val places = requests(placement)
      val request_count = places.values.foldLeft(0){
        (sum, x) => 
        val count = x match{
          case x:Int => x
          case x:List[Any] => x.size
          case _ => 0
        }
        sum + count
      }
      val key = crystal_id + "|" + geo_id + "|" + placement
      val value = "req|" + request_count
      val adreqData = (key, value)
      adreqData::genRequestListRdd(requests, placements.tail, crystal_id, geo_id)
    }
  }
  
    // Data set as String 
  def generate_adreq_analytic_result_ds_string(spark:SparkSession, data: Dataset[Data]): Dataset[(String, (Int, Int))] = {
    import spark.implicits._

    def _update_adreq_stat(count:(Int, Int) , data:(String, String)): (Int, Int) = {
      val results = data._2.split("""\|""")
      val req_or_res = results(0)
      (count._1 + results(1).toInt, count._2)
    }

    val aggregator = new Aggregator[(String, String), (Int, Int), (Int, Int)]{
      def zero: (Int, Int) = (0, 0)
      def reduce(count:(Int, Int) , data:(String, String)) = _update_adreq_stat(count, data)
      def merge(count1:(Int, Int), count2:(Int, Int)) = (count1._1 + count2._1, count1._2 + count2._2)
      def finish(count:(Int, Int)) = count
      override def bufferEncoder: Encoder[(Int, Int)] = Encoders.tuple(Encoders.scalaInt, Encoders.scalaInt)
      override def outputEncoder: Encoder[(Int, Int)] = Encoders.tuple(Encoders.scalaInt, Encoders.scalaInt)
    }.toColumn
    
    val adreq = data
      .filter(_.event_type == "ad_request")
      .flatMap(x => decode_adreq_type_as_string(x))
      .groupByKey(_._1)
      .agg(aggregator)
      
    val adreq2 = adreq.union(adreq)       
    val finalresult = adreq2.groupByKey((_._1)).reduceGroups((sum, x) => (sum._1, (sum._2._1 + x._2._1, sum._2._2 + x._2._2))).map(_._2)
    finalresult
  }
  
  def decode_adreq_type_as_string(data:Data):List[(String, String)] = {
    val props = data.props
    val requests = JSON.parseFull(props("requests")).get.asInstanceOf[Map[String, Map[String, Any]]]
    return genRequestListRddAsString(requests, requests.keys, data)
  }
  
  def genRequestListRddAsString(requests:Map[String, Map[String, Any]], placements:Iterable[String], data: Data):List[(String, String)] = {
    if(placements.size == 0){
      List[(String, String)]()
    }else{
      val placement = placements.head
      val places = requests(placement)
      val request_count = places.values.foldLeft(0){
        (sum, x) => 
        val count = x match{
          case x:Int => x
          case x:List[Any] => x.size
          case _ => 0
        }
        sum + count
      }
      val geo_id = data.geo_id - (data.geo_id % 1000000)
      val key = data.crystal_id + "|" + geo_id + "|" + placement
      val value = "req|" + request_count
      val adreqData = (key, value)
      adreqData::genRequestListRddAsString(requests, placements.tail, data)
    }
  }
  
  
  // Dataset
  def generate_adreq_analytic_result(spark:SparkSession, data: Dataset[Data]): Dataset[((String, Long, String), (Int, Int))] = {
    import spark.implicits._

    try{  
      val aggregator = new Aggregator[AdreqData, (Int, Int), (Int, Int)]{
        def zero: (Int, Int) = (0, 0)
        def reduce(count:(Int, Int) ,data:AdreqData) = (count._1 + data.request_count, count._2 + data.result_count)
        def merge(count1:(Int, Int), count2:(Int, Int)) = (count1._1 + count2._1, count1._2 + count2._2)
        def finish(count:(Int, Int)) = count
        override def bufferEncoder: Encoder[(Int, Int)] = Encoders.tuple(Encoders.scalaInt, Encoders.scalaInt)
        override def outputEncoder: Encoder[(Int, Int)] = Encoders.tuple(Encoders.scalaInt, Encoders.scalaInt)
      }.toColumn
      
      val adreq = data
        .filter(_.event_type == "ad_request")
        .flatMap(decode_adreq_type)
        .groupByKey(x => (x.crystal_id, x.geo_id, x.placement))
        .agg(aggregator)
        
      val adreq2 = adreq.union(adreq)       
      val finalresult = adreq2.groupByKey((_._1)).reduceGroups((sum, x) => (sum._1, (sum._2._1 + x._2._1, sum._2._2 + x._2._2))).map(_._2)
      finalresult
    
    }catch{
      case e: Exception => {
        println(s"generate_adreq_analytic_result error: $e") 
        return null
      }     
    }
 
  }
  
  def decode_adreq_type(data:Data):List[AdreqData] = {
    val props = data.props
    val requests = JSON.parseFull(props("requests")).get.asInstanceOf[Map[String, Map[String, Any]]]
    // TODO: doesn't process requests yet
  //  val results = JSON.parseFull(props("result")).get.asInstanceOf[Map[String, Map[String, Any]]]
    // TODO: how to concat two list? time complexity?
    return genRequestList(requests, requests.keys, data)
  }
  
  def genRequestList(requests:Map[String, Map[String, Any]], placements:Iterable[String], data: Data):List[AdreqData] = {
    if(placements.size == 0){
      List[AdreqData]()
    }else{
      val placement = placements.head
      val places = requests(placement)
      val request_count = places.values.foldLeft(0){
        (sum, x) => 
        val count = x match{
          case x:Int => x
          case x:List[Any] => x.size
          case _ => 0
        }
        sum + count
      }
      val geo_id = data.geo_id - (data.geo_id % 1000000)
      val adreqData = AdreqData(data.crystal_id, geo_id, placement, request_count, 0)
      adreqData::genRequestList(requests, placements.tail, data)
    }
  }
    
  // load by Dataframe
  def buildSchema(): StructType = {
    
    /**
    * Real Event Format
    * {"st":1492646405,"device_id":"1bb98ec719994e99963c6979f2a0dfa9","crystal_id":"169b7928a122421f851bbb6b166d5230","nt":2,"cat":"ADREQ","time":1492646354190,"geo_id":1076000000,"ug":"6","app_version":"5.16.6","sdk_version":30190200,"type":"ad_request","idfa":"de4f5e7d-28a1-4008-af4f-ba15d2e0b9ad","props":{"results":{"SCREEN_SAVER":{"9":1}},"requests":{"SCREEN_SAVER":{"1":["1492646102614"]}},"elapsed_time":300103},"version":18}
    */ 
    
    StructType(
      Array(
        StructField("app_version", StringType, false),
        StructField("cat", StringType, true),
        StructField("crystal_id", StringType, true),
        StructField("device_id", StringType, false),
        StructField("geo_id", LongType, true),
        StructField("nt", LongType, true),
        StructField("sdk_version", LongType, true),
        StructField("st", LongType, true),
        StructField("time", LongType, true),
        StructField("type", StringType, true),
        StructField("version", LongType, true),
        StructField("props", MapType(StringType,
          StringType, true
        ), false)      
      )
    )  
  }
 
  def loadDFByHour(spark:SparkSession, refTime:String, source_bucket:String): Option[DataFrame] = {
    val path = refTime
    val url = source_bucket + "/" + path + "/*.gz"
    
    println("load url: " + url)
    
    val win = Window.partitionBy("crystal_id", "device_id", "time", "type", "geo_id").orderBy("st") 
    try{
      val df = spark.read.schema(buildSchema()).json(url)
        .na.drop(List("crystal_id", "device_id", "st", "time", "type")) //choose those not nullable columns
        .withColumn("geo_id", when(col("geo_id").isNull or col("geo_id") === "", -321).otherwise(col("geo_id")))
        .withColumn("event_type", col("type"))          
        .withColumn("st", col("st") * 1000)
        .withColumn("rn", row_number.over(win))
        .where(col("rn") === 1)
        .drop(col("rn")) 
        .cache
      Some(df)
    }catch{
      case e: Exception => {
        val ct = "now"//DateTime.now.toString("Y-M-d H:m:s")
        println(s"Cannot retrieve the log for url $url, current time: $ct") 
        None
      }
    }
  }
       
  
  def timed[T](label: String, code: => T) = {
    println(s"""
      ######################################################################
        Timeing: $label
      ######################################################################
    """)

    val time = config(
      Key.exec.benchRuns -> 2
    ) measure {
      code
    }
    println(s"Total time: $time")
  }
    
  def bench(label:String, code: => Any, times: Int = 3) {
    println(s"""
      ######################################################################
        Timeing: $label
      ######################################################################
    """)
    for(_ <- 1 to times){
      val start = System.nanoTime
      code      
      val time = (System.nanoTime - start)/1000000000.0
      println("spend time: " + time)  
    }
  }

}


