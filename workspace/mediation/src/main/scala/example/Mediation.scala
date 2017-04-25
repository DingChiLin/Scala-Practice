package example

import java.io.File
import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.rdd.RDD
import scala.util.matching.Regex
import com.github.nscala_time.time.Imports._

object Mediation {
 
  import org.apache.spark.sql.SparkSession
  import org.apache.spark.sql.functions._
  
  val INVALID_GEO_ID = "-321"
  val PARTITION_SIZE = 100
  val GENERAL_REQUIRED_KEYS = List("type", "time")

  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("Time Usage")
      .config("spark.master", "local[2]")
      .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")     
    
  def main(args: Array[String]) {
    
//         val path = "file:/Users/arthurlin/Desktop/aws_s3_files/hour=00/20170420-000001-ce-global-api-prod-4108-Br03CH-OS.log.gz"

//     val dir = "/Users/arthurlin/Desktop/aws_s3_files/hour=00"
//     val d = new File(dir)
//     println(d.exists)
//     println(d.listFiles.toList)
    
     val refTime = "test"
     val source_bucket = "/Users/arthurlin/Desktop/aws_s3_files"
     
//     val result = loadRddByHour(refTime, source_bucket)
//     println(result.take(3).toList)

         
  }
  

  
  def prepareHistoryRdds(start_time:DateTime, end_time:DateTime, source_bucket:String):List[RDD[(String, String)]] = {
    
    val start_hour = start_time.hour(0).minute(0).second(0)
    val end_hour = end_time.hour(0).minute(0).second(0)
    
    if(start_hour > end_hour) return List[RDD[(String, String)]]()
        
      
    val refTime = ""
    val rdd = loadRddByHour(start_time, source_bucket)
  
    return List(rdd)
  
  }
  
  def loadRddByHour(refTime:DateTime, source_bucket:String):RDD[(String, String)] = {
    val p = convertTimeToS3PartitionPath(refTime)
    val allFiles = getFileList(source_bucket, p) 
    val rdd = spark.sparkContext.textFile(allFiles.mkString(","))
   
    return prepareDeltaRdd(rdd)     
  }
  
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
      geoId = INVALID_GEO_ID
      event = event.dropRight(1) + s"""',"geo_id":$geoId}"""
    } 
    
    val type_ = jsonField(event, jsonStringKeyMatcher("type"))
    if(type_.isEmpty()) return nullTuple
    
    val header = crystalId + "|" + deviceId + "|" + serverTime + "|"
    event = header + event
    
    val st = serverTime.toLong * 1000
    if((st - time.toLong).abs > 4 * 60 * 60 * 1000) return nullTuple
    
    val delim = "|!$@#||"
    val key = combine(List(crystalId, deviceId, time, geoId, type_), delim)
     
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
  
  def getSt(line:String):Int = {
    val pos = line.indexOf("|")
    val pos2 = line.indexOf("|", pos + 1)
    val pos3 = line.indexOf("|", pos2 + 1)
    line.slice(pos2 + 1, pos3).toInt
  }
}

