package example

import java.io.File
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.rdd.RDD  //can skip if not use
//import scala.util.matching.Regex
import com.github.nscala_time.time.Imports._
import org.scalameter._
import scala.util.parsing.json._
//import net.liftweb.json._

import java.sql.DriverManager
import java.sql.Connection

import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.expressions.Aggregator
import org.apache.spark.sql.expressions.scalalang.typed
import org.apache.spark.sql.Encoder

object Mediation {
 
  import org.apache.spark.sql.SparkSession
  import org.apache.spark.sql.functions._
    
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
  
  case class Data(crystal_id:String, device_id:String, event_type:String, st: Long, time:Long, geo_id:Long, props:scala.collection.Map[String, String])
  case class AdreqData(crystal_id:String, geo_id:Long, placement:String, request_count:Int, result_count:Int)
//  case class AdreqResult(key:(String, Long, String), total_count:(Int, Int))
  
  def buildSchema(): StructType = {
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

  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("Time Usage")
      .config("spark.master", "local[4]")
      .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")     
  import spark.implicits._

  def main(args: Array[String]) {
    val source_bucket = "/Users/arthurlin/Desktop/aws_s3_files" //TODO: should s3 path
    val current_time = new DateTime("2017-04-20T01:34:56").minute(0).second(0) //TODO:should be today
    val history_start_hour = current_time - 1.hour
    val history_end_hour = current_time
    var result = prepareHistoryDataFrame(history_start_hour, history_end_hour, source_bucket)(0)
    result.show
    
    val ds = result.as[Data]
//    ds.cache
//    ds.count
//    timed("final result ds using object", generate_adreq_analytic_result(ds).count)
//    timed("final result ds using string", generate_adreq_analytic_result_ds_string(ds).count)

    val rdd = ds.rdd.map(x => convert_to_pure_string(x)).cache // convert to pure string to do benchmark
    rdd.count
    timed("final result rdd", generate_adreq_analytic_result_rdd(rdd).count)
    
    spark.stop
  }
  
  def convert_to_pure_string(data: Data):String = {
    return (data.crystal_id + "|" + data.geo_id + "|" + data.event_type + "|" + scala.util.parsing.json.JSONObject(data.props.toMap)).replace("\\","").replace("\"{", "{").replace("}\"","}").replace("\"[", "[").replace("]\"","]")
  }
  
  // RDD 
  def generate_adreq_analytic_result_rdd(data: RDD[String]):RDD[(String, (Int, Int))] = {
    
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
      .filter(_.contains("ad_request"))
      .flatMap(decode_adreq_type_rdd)
      .aggregateByKey(adreq_initial_state)(_update_adreq_stat, _merge_adreq_stat)
      
    val adreq2 = adreq.union(adreq)
    val result = adreq2.reduceByKey(_merge_adreq_stat)
      
    return result
  }
  
  def decode_adreq_type_rdd(data:String):List[(String, String)] = {
    val values = data.split("""\|""")
    val crystal_id = values(0)
    val geo_id = values(1).toInt - (values(1).toInt % 1000000)
    val requests = JSON.parseFull(values(3)).get.asInstanceOf[Map[String, Map[String, Map[String, Any]]]]("requests")
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
  
  // Dataset
  def generate_adreq_analytic_result(data: Dataset[Data]): Dataset[((String, Long, String), (Int, Int))] = {

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
      .flatMap(x => decode_adreq_type(x))
      .groupByKey(x => (x.crystal_id, x.geo_id, x.placement))
      .agg(aggregator)
      
    val adreq2 = adreq.union(adreq)       
    val finalresult = adreq2.groupByKey((_._1)).reduceGroups((sum, x) => (sum._1, (sum._2._1 + x._2._1, sum._2._2 + x._2._2))).map(_._2)
    finalresult
  }
  
  def decode_adreq_type(data:Data):List[AdreqData] = {
    val props = data.props
    val requests = JSON.parseFull(props("requests")).get.asInstanceOf[Map[String, Map[String, Any]]]
    //TODO: doesn't process requests yet
//    val results = JSON.parseFull(props("result")).get.asInstanceOf[Map[String, Map[String, Any]]]
    //TODO: how to concat two list? time complexity?
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
  
  // Data set as String 
  def generate_adreq_analytic_result_ds_string(data: Dataset[Data]): Dataset[(String, (Int, Int))] = {
    
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
  
  
  // End of Benchmark code
  
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
     // 1. take current_time rdd (current time)
     var currentDf = loadRddByHour(current_time, source_bucket) match{
       case None => return
       case Some(r) => r
     }
     
     // 2. substrack history rdd from current time
     historyDfList.foreach(ds => 
       currentDf = currentDf.except(ds)
     )
     currentDf.cache
     
     // 3. combine rdd, app info and price info, create an adreq data and insert it to DB  

     // 3.1 filter out type without "ad_request"
     // 3.2 map to key and request/result count pair
     // 3.3 aggregate request/result count by Key
     // 3.4 union with the adreq data we get last time
     // 3.5 reduce request/result count by key again
     // 3.6 preserve the final adreq data that we need to union next time
    
  }
  
  def prepareHistoryDataFrame(start_hour:DateTime, end_hour:DateTime, source_bucket:String):List[DataFrame] = {
    if(start_hour > end_hour){
      return List[DataFrame]()
    }else{
      val ds = loadRddByHour(start_hour, source_bucket)
      val next = prepareHistoryDataFrame(start_hour + 1.hour, end_hour, source_bucket)
      ds match {
        case Some(d) => d::next
        case None => next
      }
    }
  }
  
  def loadRddByHour(refTime:DateTime, source_bucket:String): Option[DataFrame] = {
    val path = convertTimeToS3PartitionPath(refTime)
    val url = source_bucket + "/" + path + "/*.gz"
    
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

