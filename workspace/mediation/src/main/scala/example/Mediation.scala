package example

import java.io.File
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.rdd.RDD
import scala.util.matching.Regex
import com.github.nscala_time.time.Imports._
import org.scalameter._
import scala.util.parsing.json._
import net.liftweb.json._

import org.apache.spark.sql.expressions.Window

import org.apache.spark.sql.expressions.scalalang.typed

object Mediation {
 
  import org.apache.spark.sql.SparkSession
  import org.apache.spark.sql.functions._

    
  val INVALID_GEO_ID = -321
  val PARTITION_SIZE = 100
  val GENERAL_REQUIRED_KEYS = List("type", "time")
  
  case class Data(crystal_id:String, device_id:String, event_type:String, st: Long, time:Long, geo_id:Long, props:scala.collection.Map[String, scala.collection.Map[String, String]])
  def buildSchema(): StructType = {
    StructType(
      Array(
        StructField("app_version", StringType, true),
        StructField("cat", StringType, true),
        StructField("crystal_id", StringType, true),
        StructField("device_id", StringType, true),
        StructField("geo_id", LongType, true),
        StructField("nt", LongType, true),
        StructField("sdk_version", LongType, true),
        StructField("st", LongType, true),
        StructField("time", LongType, true),
        StructField("type", StringType, true),
        StructField("version", LongType, true),
        StructField("props", MapType(StringType,
          MapType(StringType, StringType), true
        ), true)      
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
  
     // 1. take history rdd  (current time - x_hour to corrent-time - 1_hour)
     val source_bucket = "/Users/arthurlin/Desktop/aws_s3_files" //TODO: should s3 path
     val current_time = new DateTime("2017-04-20T02:34:56").minute(0).second(0) //TODO:should be today
       
     val history_start_hour = current_time - 2.hour
     val history_end_hour = current_time 
     
     val dsList = prepareHistoryRdds(history_start_hour, history_end_hour, source_bucket)
     
     // 2. take current_time rdd (current time)
     val ds = loadRddByHour(current_time, source_bucket)
     
     // 3. get app info and price info from DB
     // 4. substrack history rdd from current time
     // 5. combine rdd, app info and price info and insert the result to DB  
  }
  
  def prepareHistoryRdds(start_hour:DateTime, end_hour:DateTime, source_bucket:String):List[Dataset[Data]] = {
    if(start_hour > end_hour){
      return List[Dataset[Data]]()
    }else{
      val ds = loadRddByHour(start_hour, source_bucket)
      ds::prepareHistoryRdds(start_hour + 1.hour, end_hour, source_bucket)
    }
  }
  
  def loadRddByHour(refTime:DateTime, source_bucket:String): Dataset[Data] = {
    val path = convertTimeToS3PartitionPath(refTime)
    val url = source_bucket + "/" + path + "/*.gz"
    
    val win = Window.partitionBy("crystal_id", "device_id", "time", "type", "geo_id").orderBy("st") 
    val ds = spark.read.schema(buildSchema())
        .json(url) //("/Users/arthurlin/Desktop/aws_s3_files/year=2017/month=04/day=20/hour=01/*.gz")
        .withColumn("geo_id", when(col("geo_id").isNull or col("geo_id") === "", INVALID_GEO_ID).otherwise(col("geo_id")))
        .withColumn("event_type", col("type"))          
        .withColumn("st", col("st") * 1000)
        .withColumn("rn", row_number.over(win))
        .where(col("rn") === 1)
        .drop(col("rn"))
        .as[Data]
        .cache
 
    return ds
  }
      

  def row(line: List[String]): Row = {
    val row = line.map(
      {
        case r if r == line.head => r
        case r => r.toDouble
      }
    )
    Row.fromSeq(row)
    
  }
  

  
//  def parseStringWithKey(e:String):(String, Data) = {
//    val data = parseString(e)
//    (data.key, data)
//  }
//  
//  def parseString(e:String):Data = {
//    val nullTuple = Data("","","",0,0,0,"")
//    var event = e.trim
//    if(event.last != '}') return nullTuple
//      
//    val crystalId = jsonField(event, jsonStringKeyMatcher("crystal_id"))
//    if(crystalId.isEmpty()) return nullTuple
//    
//    val deviceId = jsonField(event, jsonStringKeyMatcher("device_id"))
//    if(deviceId.isEmpty()) return nullTuple
//    
//    val serverTime = jsonField(event, jsonNumKeyMatcher("st"))
//    if(serverTime.isEmpty()) return nullTuple 
//    
//    val time = jsonField(event, jsonNumKeyMatcher("time"))
//    if(time.isEmpty()) return nullTuple
//    
//    var geoId = jsonField(event, jsonNumKeyMatcher("geo_id"))
//    if(geoId.isEmpty()){
//      geoId = INVALID_GEO_ID
////      event = event.dropRight(1) + s"""',"geo_id":$geoId}"""
//    } 
//    
//    val event_type = jsonField(event, jsonStringKeyMatcher("type"))
//    if(event_type.isEmpty()) return nullTuple
//    
////    val header = crystalId + "|" + deviceId + "|" + serverTime + "|"
////    event = header + event
//    
//    val st = serverTime.toLong * 1000
//    if((st - time.toLong).abs > 4 * 60 * 60 * 1000) return nullTuple
//    
//    val delim = "|!$@#||"
//    val key = combine(List(crystalId, deviceId, time, geoId, event_type), delim)
//    
//    return Data(crystalId, deviceId, event_type, serverTime.toLong, time.toLong, geoId.toInt, key)
//  }
//  
//  def parseStringAsJsonWithKey(e:String):(String, Data) = {
//    val data = parseStringAsJson(e)
//    (data.key, data)
//  }
//    
//  def parseStringAsJson(e:String):Data = {
//    val nullTuple = Data("","","",0,0,0,"")
//    var event = e.trim
//    val data = JSON.parseFull(event).get.asInstanceOf[Map[String,Any]]
//    
////    if(event.last != '}') return nullTuple
//      
//    val crystalId = data("crystal_id").toString //jsonField(event, jsonStringKeyMatcher("crystal_id"))
//    if(crystalId.isEmpty()) return nullTuple
//    
//    val deviceId = data("device_id").toString //jsonField(event, jsonStringKeyMatcher("device_id"))
//    if(deviceId.isEmpty()) return nullTuple
//    
//    val serverTime = data("st").asInstanceOf[Double].toLong //jsonField(event, jsonNumKeyMatcher("st"))
//    if(serverTime == 0) return nullTuple 
//    
//    val time = data("time").asInstanceOf[Double].toLong //jsonField(event, jsonNumKeyMatcher("time"))
//    if(time == 0) return nullTuple
//    
//    var geoId = 0
//    try{
//      geoId = data("geo_id").asInstanceOf[Double].toInt //jsonField(event, jsonNumKeyMatcher("geo_id"))
//    }catch{
//      case e: java.util.NoSuchElementException => geoId = 0
//    }
////    if(geoId == 0){
////      geoId = -321
//////      event = event.dropRight(1) + s"""',"geo_id":$geoId}"""
////    } 
//    
//    val event_type = data("type").toString //jsonField(event, jsonStringKeyMatcher("type"))
//    if(event_type.isEmpty()) return nullTuple
//    
////    val header = crystalId + "|" + deviceId + "|" + serverTime + "|"
////    event = header + event
//    
//    val st = serverTime.toLong * 1000
//    if((st - time.toLong).abs > 4 * 60 * 60 * 1000) return nullTuple
//    
//    val delim = "|!$@#||"
//    val key = combine(List(crystalId, deviceId, time.toString, geoId.toString, event_type), delim)
//    
//    return Data(crystalId, deviceId, event_type, serverTime, time, geoId, key)
//  
//  }
  
  def convertTimeToS3PartitionPath(refTime:DateTime): String = {
    val dateInfo = refTime.toString("Y|MM|dd|HH").split('|')
    s"year=${dateInfo(0)}/month=${dateInfo(1)}/day=${dateInfo(2)}/hour=${dateInfo(3)}"
  }
    
  def prepareDeltaRdd(rdd:RDD[String]):RDD[(String, String)] = {
    val delta_rdd = 
      rdd.filter(generalValidator(_, GENERAL_REQUIRED_KEYS))
         .map(convertLog2KeyValuePair)
         .filter(!_._1.isEmpty())
         .reduceByKey(dedupeEventByServerTime _, PARTITION_SIZE)
 
    return delta_rdd     
  }
  
  def prepareDeltaDS(ds:Dataset[String]):Dataset[(String, String)] = {
    val delta_ds = 
      ds.filter(generalValidator(_, GENERAL_REQUIRED_KEYS))
        .map(convertLog2KeyValuePair)
        .filter(!_._1.isEmpty())
        .groupByKey(_._1)
        .mapValues{ case (x,y) => y }
        .reduceGroups(dedupeEventByServerTime(_,_))
//         .mapGroups((a,b) => (a, b.map(_._2).reduce(dedupeEventByServerTime(_,_))))
//         .reduceGroups((x,y) => x)
//         .map{ case (k,(a,b)) => (k,b) }
//         .reduceByKey(dedupeEventByServerTime _, PARTITION_SIZE)
 
    return delta_ds    
  }
 
  def getFileList(source_bucket:String, path:String): List[String] = {
    val url = source_bucket + '/' + path
    val dir = new File(url)
    
    dir.listFiles
       .toList
       .map(f => f.getName)
       .filter(name => name.endsWith(".gz"))
       .map(name => "file:" + url + "/" + name)
  }
    
  def convertLog2KeyValuePair(e:String):(String,String) = {
    /*Unpack event message to separate events.*/ 
    val nullTuple = ("","")
    var event = e.trim
    if(event.last != '}') return nullTuple
      
    val crystalId = jsonField(event, jsonStringKeyMatcher("crystal_id"))
    if(crystalId.isEmpty()) return nullTuple
    
    val deviceId = jsonField(event, jsonStringKeyMatcher("device_id"))
    if(deviceId.isEmpty()) return nullTuple
    
    val serverTime = jsonField(event, jsonNumKeyMatcher("st"))
    if(serverTime.isEmpty()) return nullTuple 
    
    val time = jsonField(event, jsonNumKeyMatcher("time"))
    if(time.isEmpty()) return nullTuple
    
    var geoId = jsonField(event, jsonNumKeyMatcher("geo_id"))
    if(geoId.isEmpty()){
      geoId = INVALID_GEO_ID.toString
      event = event.dropRight(1) + s"""',"geo_id":$geoId}"""
    } 
    
    val type_ = jsonField(event, jsonStringKeyMatcher("type"))
    if(type_.isEmpty()) return nullTuple
    
    val header = crystalId + "|" + deviceId + "|" + serverTime + "|"
    event = header + event
    
    val st = serverTime.toLong * 1000
    if((st - time.toLong).abs > 4 * 60 * 60 * 1000) return nullTuple
    
    val delim = "|!$@#||"
    val key = combine(List(crystalId, deviceId, time, geoId.toString, type_), delim)
     
    return (key, event)
  }
  
  def jsonStringKeyMatcher(key:String):Regex = {
    s"""("$key":\\s*")([^"]+)(")""".r
  }
  
  def jsonNumKeyMatcher(key:String):Regex = {
    s"""("$key":\\s*)([\\-0-9]+)""".r
  }
  
  def jsonField(string: String, matcher:Regex):String = {
    matcher.findFirstMatchIn(string) match{ case Some(r) => r.group(2); case None => ""} 
  }
  
  def generalValidator(e:String, keys:List[String]):Boolean = {
    keys.forall(e.contains) 
  }
   
  def combine(strs:List[String], delim:String):String = {
    strs match {
      case x::Nil => x
      case x::xs => x + delim + combine(xs, delim)
      case Nil => ""
    }  
  }
  
  def dedupeEventByServerTime(event1:String, event2:String):String = {
    val st1 = getSt(event1)
    val st2 = getSt(event2)
    if(st1 <= st2) return event1 else return event2
  }
  
  def dedupeDataByServerTime(data1:Data, data2:Data): Data = {
    if(data1.st <= data2.st) return data1 else return data2
  }
  
  def getSt(line:String):Int = {
    val pos = line.indexOf("|")
    val pos2 = line.indexOf("|", pos + 1)
    val pos3 = line.indexOf("|", pos2 + 1)
    line.slice(pos2 + 1, pos3).toInt
  }
  
  
  
  def timed[T](label: String, code: => T) = {
    println(s"""
      ######################################################################
        Timeing: $label
      ######################################################################
    """)
    val time = config(
      Key.exec.benchRuns -> 10
    ) withWarmer {
      new Warmer.Default
    } measure {
      code
    }
    println(s"Total time: $time")
  }
}

