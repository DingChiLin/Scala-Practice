package example

import java.io.File
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.rdd.RDD  //can skip if not use
//import scala.util.matching.Regex
import com.github.nscala_time.time.Imports._
import org.scalameter._
import scala.util.parsing.json._
import play.api.libs.json._
//import net.liftweb.json._

import java.sql.DriverManager
import java.sql.Connection

import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.expressions.Aggregator
import org.apache.spark.sql.expressions.scalalang.typed
import org.apache.spark.sql.Encoder
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
  
object AdreqAnalyzer {
  
  val INVALID_GEO_ID = -321
  val PARTITION_SIZE = 100
  val GENERAL_REQUIRED_KEYS = List("type", "time")
  
  //TODO: should read config
  var CONFIG = Map(
    "driver" -> "com.mysql.jdbc.Driver",
    "url" -> "jdbc:mysql://127.0.0.1/ce",
    "username" -> "root",
    "password" -> "1234"
  )
  
  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("Time Usage")
      .config("spark.master", "local[4]")
      .getOrCreate()
    
  spark.sparkContext.setLogLevel("ERROR")     
  import spark.implicits._ 
  
  case class Data(crystal_id:String, device_id:String, event_type:String, st: Long, time:Long, geo_id:Long, simple_geo_id:Long, props:scala.collection.Map[String, String])
  case class AdreqData(crystal_id:String, geo_id:Long, placement:String, request_count:Int, result_count:Int) 
  
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
        StructField("simple_geo_id", LongType, true),
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
  
  def start(){
   
//    val source_bucket = "s3://ce-production-raw-event-logs/ADREQ"
    val source_bucket = "/Users/arthurlin/Desktop/aws_s3_files"
    val current_time = new DateTime("2017-04-20T03:34:56").minute(0).second(0) //TODO:should be today
    val history_start_hour = current_time - 1.hour
    val history_end_hour = current_time
    
    val path = convertTimeToS3PartitionPath(current_time)
    val url = source_bucket + "/" + path + "/*.gz"
    
    val result2 = loadDFByHour(url).get.as[Data].cache
    result2.show
   
    val final_result2 = generateAdreqAnalyticResult(result2)
    final_result2.show()
  }
  
  def process(){            
     val source_bucket = "/Users/arthurlin/Desktop/aws_s3_files" //TODO: should s3 path
     val current_time = new DateTime("2017-04-20T02:34:56").minute(0).second(0) //TODO:should be today
     print (s"Run analyze for $current_time")
        
     // 1. take history rdd  (current time - x_hour to corrent-time - 1_hour)
     val history_start_hour = current_time - 2.hour
     val history_end_hour = current_time - 1.hour 
     val historyDfList = prepareHistoryDataFrame(history_start_hour, history_end_hour, source_bucket)
               
     // 4. get app info and price info from DB
     val app = getAppInfo()
     val price = getPriceInfo()
     
     // 3. ong loop every x minute
     var index = 0
     while(index < 2){ //TODO: should be a long run function
       val time = new DateTime("2017-04-20T02:34:56").minute(0).second(0) //TODO:should be now
       analyze(time, source_bucket, historyDfList)
       index += 1
     }
  }
  
  def analyze(current_time:DateTime, source_bucket:String, historyDfList:List[DataFrame]){
//     // 1. take current_time rdd (current time)
//     var currentDf = loadRddByHour(current_time, source_bucket) match{
//       case None => return
//       case Some(r) => r
//     }
//     
//     // 2. substrack history rdd from current time
//     historyDfList.foreach(ds => 
//       currentDf = currentDf.except(ds)
//     )
//     currentDf.cache
     
     // 3. combine rdd, app info and price info, create an adreq data and insert it to DB  

     // 3.1 filter out type without "ad_request"
     // 3.2 map to key and request/result count pair
     // 3.3 aggregate request/result count by Key
     // 3.4 union with the adreq data we get last time
     // 3.5 reduce request/result count by key again
     // 3.6 preserve the final adreq data that we need to union next time
    
  }
  
  def prepareHistoryDataFrame(start_hour:DateTime, end_hour:DateTime, source_bucket:String):List[DataFrame] = {
//    if(start_hour > end_hour){
//      return List[DataFrame]()
//    }else{
//      val ds = loadRddByHour(start_hour, source_bucket)
//      val next = prepareHistoryDataFrame(start_hour + 1.hour, end_hour, source_bucket)
//      ds match {
//        case Some(d) => d::next
//        case None => next
//      }
//    }
    return null
  }  

  def loadDFByHour(url:String): Option[DataFrame] = {    
    println("load url: " + url)
    
    val win = Window.partitionBy("crystal_id", "device_id", "time", "type", "geo_id").orderBy("st") 
    try{
      val df = spark.read.schema(buildSchema()).json(url)
        .na.drop(List("crystal_id", "device_id", "st", "time", "type")) //choose those not nullable columns
        .withColumn("geo_id", when(col("geo_id").isNull or col("geo_id") === "", INVALID_GEO_ID).otherwise(col("geo_id")))
        .withColumn("event_type", col("type"))          
        .withColumn("st", col("st") * 1000)
        .withColumn("rn", row_number.over(win))
        .where(col("rn") === 1)
        .drop(col("rn"))
        .withColumn("simple_geo_id", when(col("geo_id") === INVALID_GEO_ID, INVALID_GEO_ID).otherwise(col("geo_id") - col("geo_id") % 1000000 ))
        .cache
      Some(df)
    }catch{
      case e: Exception => {
        val ct = DateTime.now.toString("Y-M-d H:m:s")
        println(s"Cannot retrieve the log for url $url, current time: $ct") 
        None
      }
    }
  }
       
  def convertTimeToS3PartitionPath(refTime:DateTime): String = {
    val dateInfo = refTime.toString("Y|MM|dd|HH").split('|')
    s"year=${dateInfo(0)}/month=${dateInfo(1)}/day=${dateInfo(2)}/hour=${dateInfo(3)}"
  }
 
  def generateAdreqAnalyticResult(data: Dataset[Data]): Dataset[((String, Long, String), (Int, Int))] = {
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
        .flatMap(decodeAdreqType)
        .groupByKey(x => (x.crystal_id, x.geo_id, x.placement))
        .agg(aggregator)
      
      adreq.show()  
        
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
  
  def decodeAdreqType(data:Data):List[AdreqData] = {
    try{
      val props = data.props
      val requests = (Json.parse(props("requests"))).asOpt[Map[String, Map[String, JsValue]]] //.getOrElse( Map("STREAM_C_ROADBLOCK_2" -> Map("1" -> "3"))  )   
      val results = (Json.parse(props("results"))).asOpt[Map[String, Map[String, JsValue]]] //.getOrElse( Map("STREAM_C_ROADBLOCK_2" -> Map("1" -> "3"))  )   
      
      println(requests)
      println(results)
      
      val requestList = requests match{
        case None => List()
        case Some(r) => genRequestList(r, 1, r.keys, data)
      }
      
      val resultList = results match{
        case None => List() 
        case Some(r) => genRequestList(r, 2, r.keys, data)
      }    
      
      resultList ++ requestList   
      
    }catch{
      case e:Exception => {println(s"data $data can not decode with error: $e")}
      List()
    }
  }
  
  // adreqType 1:request, 2:result
  def genRequestList(adreqs:Map[String, Map[String, JsValue]], adreqType:Int, placements:Iterable[String], data: Data):List[AdreqData] = {
    if(placements.size == 0){
      List[AdreqData]()
    }else{
      val placement = placements.head
      val places = adreqs(placement)
      val validPlaces = if(adreqType == 1) places else places.filter{case (k,v) => k == "1"}
      val total_count = validPlaces.values.foldLeft(0){
        (sum, x) => {
          val value = x.asOpt[List[String]].getOrElse(x.asOpt[String].getOrElse(x.asOpt[Int].getOrElse(x.asOpt[Double].getOrElse(None))))
          val count = value match{
            case v:List[Any] => v.size
            case v:String => safeToInt(v)
            case v:Int => v
            case v:Double => v.toInt
            case _ => 0
          }
          sum + count
        }
      }
      val simple_geo_id = data.simple_geo_id
      val adreqData = adreqType match{
        case 1 => AdreqData(data.crystal_id, simple_geo_id, placement, total_count, 0)
        case 2 => AdreqData(data.crystal_id, simple_geo_id, placement, 0, total_count)
      }
      adreqData::genRequestList(adreqs, adreqType, placements.tail, data)
    }
  }
  
  def safeToInt(s: String):Int = {
    try {
      s.toInt
    } catch {
      case e: NumberFormatException => 0
    }
  }
  
  def getAppInfo() = {
    Class.forName(CONFIG("driver"))
    val connection = DriverManager.getConnection(CONFIG("url"), CONFIG("username"), CONFIG("password"))
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery("SELECT app_id, fk_pub_id, ot, crystal_id FROM app")
    
    val app = scala.collection.mutable.Map[String, Map[String, String]]() 
    while(resultSet.next()){
      app(resultSet.getString("crystal_id")) = Map(
        "app_id" -> resultSet.getString("app_id"),
        "pub_id" -> resultSet.getString("fk_pub_id"),
        "ot" -> resultSet.getString("ot")        
      )
    }
    
    connection.close()
    app
  }
  
  def getPriceInfo() = {
    Class.forName(CONFIG("driver"))
    val connection = DriverManager.getConnection(CONFIG("url"), CONFIG("username"), CONFIG("password"))
    val statement = connection.createStatement()
    val resultSet = statement.executeQuery("SELECT price_id, price_key FROM ad_unit_price")
    
    val price = scala.collection.mutable.Map[String, Map[String, String]]() 
    while(resultSet.next()){
      price(resultSet.getString("price_key")) = Map(
        "price_id" -> resultSet.getString("price_id")
      )
    }
    
    connection.close()
    price    
  }
}

object Mediation {
 
  def main(args: Array[String]) {
    AdreqAnalyzer.start()
  }
  
  def timed[T](label: String, code: => T) = {
    println(s"""
      ######################################################################
        Timeing: $label
      ######################################################################
    """)
//    val time = config(
//      Key.exec.benchRuns -> 5
//    ) withWarmer {
//      new Warmer.Default
//    } measure {
//      code
//    }
    val time = config(
      Key.exec.benchRuns -> 5
    ) measure {
      code
    }
    println(s"Total time: $time")
  }
}

