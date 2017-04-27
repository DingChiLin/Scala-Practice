package example

import org.scalatest._
import com.github.nscala_time.time.Imports._

class MediationSpec extends FlatSpec with Matchers {
  
  /**
   * Real Event Format
   * {"st":1492646405,"device_id":"1bb98ec719994e99963c6979f2a0dfa9","crystal_id":"169b7928a122421f851bbb6b166d5230","nt":2,"cat":"ADREQ","time":1492646354190,"geo_id":1076000000,"ug":"6","app_version":"5.16.6","sdk_version":30190200,"type":"ad_request","idfa":"de4f5e7d-28a1-4008-af4f-ba15d2e0b9ad","props":{"results":{"SCREEN_SAVER":{"9":1}},"requests":{"SCREEN_SAVER":{"1":["1492646102614"]}},"elapsed_time":300103},"version":18}
   */ 
  
  "prepareDeltaRdd" should """1. filter out row without GENERAL_REQUIRED_KEYS 
                              2. generate key value pair by combination of ids 
                              3. reduce by keep the one with smaller server_time""" in {
   
     import org.apache.spark.sql.SparkSession
     import org.apache.spark.sql.functions._
     val spark: SparkSession =
       SparkSession
         .builder()
         .appName("Time Usage")
         .config("spark.master", "local[2]")
         .getOrCreate()
     val sc = spark.sparkContext
     sc.setLogLevel("ERROR")     

     // delete this event because it has no crystal_id
     val event1 = """{"type":"ad_request","geo_id":111,"crystal_id":"","st":789,"time":789000,"device_id":"456"}"""
      
     // delete this event because its server_time and client_time have difference more than 4 hour
     val event2 = """{"type":"ad_request","geo_id":111,"crystal_id":"123","st":30000,"time":10000000,"device_id":"456"}"""
    
     // add null_geo_id to this event
     val event3 = """{"type":"ad_request","crystal_id":"111","st":789,"time":789000,"device_id":"456"}"""
     
     // reduce this two by delete event4 because it has larger server_time
     val event4 = """{"type":"ad_request","geo_id":111,"crystal_id":"222","st":790, "time":789000,"device_id":"456"}"""
     val event5 = """{"type":"ad_request","geo_id":111,"crystal_id":"222","st":789, "time":789000,"device_id":"456"}"""

     val null_geo_id = Mediation.INVALID_GEO_ID

     var rdd = sc.parallelize(List(event1, event2, event3, event4, event5))
     val result = List((s"111|!$$@#||456|!$$@#||789000|!$$@#||$null_geo_id|!$$@#||ad_request", "111|456|789|" + event3.dropRight(1) + s"""',"geo_id":$null_geo_id}"""),
                       ("222|!$@#||456|!$@#||789000|!$@#||111|!$@#||ad_request", "222|456|789|" + event5))
     Mediation.prepareDeltaRdd(rdd).collect.toList shouldBe result
  }
  
  "convertLog2KeyValuePair" should "combine crystal_id, device_id, time, geo_id and type_ as key in tuple" in {
    val event = """{"type":"ad_request","geo_id":111,"version":9,"crystal_id":"123","st":789,"time":789000,"device_id":"456"}"""
    val header = "123|456|789|"
    Mediation.convertLog2KeyValuePair(event) shouldBe ("123|!$@#||456|!$@#||789000|!$@#||111|!$@#||ad_request", header+event)

    // need add null geo_id
    val null_geo_id = Mediation.INVALID_GEO_ID
    val event2 = """{"type":"ad_request","version":9,"crystal_id":"123","st":789,"time":789000,"device_id":"456"}"""
    val header2 = "123|456|789|"
    Mediation.convertLog2KeyValuePair(event2) shouldBe (s"""123|!$$@#||456|!$$@#||789000|!$$@#||$null_geo_id|!$$@#||ad_request""", header2+event2.dropRight(1) + s"""',"geo_id":$null_geo_id}""")

    // server_time and time have difference more than 4 hour
    val event3 = """{"type":"ad_request","geo_id":111,"version":9,"crystal_id":"123","st":30000,"time":10000000,"device_id":"456"}"""
    Mediation.convertLog2KeyValuePair(event3) shouldBe ("","")
  }
  
  "getSt" should "find server_time in event" in {
     val event = "123|456|789|{xxxxx}"
     Mediation.getSt(event) shouldBe 789
  }
  
  "generalValidator" should "find if string contains all the keys we need" in {
    val str = "abcde"
    val keys1 = List("a","b","z")
    Mediation.generalValidator(str, keys1) shouldBe false
    val keys2 = List("a","b","c")
    Mediation.generalValidator(str, keys2) shouldBe true
  }
  
  "combine" should "encode str in list and join the list while add delim between each string" in {
     val DELIM = "|!$@#||"
     val list = List("a", "b", "c")
     Mediation.combine(list, DELIM) shouldBe "a|!$@#||b|!$@#||c"
  }
  
  "convertTimeToS3PartitionPath" should "convert datetime object to string with s3 format" in {
    val dt = new DateTime("2017-06-05T02:34:56");
    println(dt.toString("Y|MM|dd|HH").split('|').toList)
    Mediation.convertTimeToS3PartitionPath(dt) shouldBe "year=2017/month=06/day=05/hour=02"
  }
}
