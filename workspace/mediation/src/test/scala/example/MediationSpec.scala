package example

import org.scalatest._
import com.github.nscala_time.time.Imports._

class MediationSpec extends FlatSpec with Matchers {
  
  /**
   * Real Event Format
   * {"st":1492646405,"device_id":"1bb98ec719994e99963c6979f2a0dfa9","crystal_id":"169b7928a122421f851bbb6b166d5230","nt":2,"cat":"ADREQ","time":1492646354190,"geo_id":1076000000,"ug":"6","app_version":"5.16.6","sdk_version":30190200,"type":"ad_request","idfa":"de4f5e7d-28a1-4008-af4f-ba15d2e0b9ad","props":{"results":{"SCREEN_SAVER":{"9":1}},"requests":{"SCREEN_SAVER":{"1":["1492646102614"]}},"elapsed_time":300103},"version":18}
   */ 
  
  /**
   * decodeAdreqType
   */
  
  "decodeAdreqType" should "return List() when some contents cannot be parsed by JSON" in {
    val props = Map(
      "requests" -> """{"foo":{"1":100}}""",
      "results" -> """[{"foo":{"foo": !!!! CAN NOT PARSE !!!!}}]"""
    )
    val data = AdreqAnalyzer.Data("111", "222", "333", 1, 2, 3, 4, props)
    AdreqAnalyzer.decodeAdreqType(data) shouldBe List()
  }
    
  "decodeAdreqType" should "parse props in Data into List[AdreqData] when requests have list and string" in {
    val props = Map(
      "requests" -> """{"place1":{"1":["1492653598864","111","222"], "2":"5"}}""",  // List size: 3 + string to Int: 5
      "results" -> """{"foo":"bar"}"""
    )
    val data = AdreqAnalyzer.Data("111", "222", "333", 1, 2, 3, 4, props)
    AdreqAnalyzer.decodeAdreqType(data) shouldBe List(AdreqAnalyzer.AdreqData("111", 4, "place1", 8, 0))
  }
  
  "decodeAdreqType" should "parse props in Data into List[AdreqData] when requests have integer and double" in {
    val props = Map(
      "requests" -> """{"place1":{"1":2, "2":3.8}}""",  // Int: 2 + Double to Int: 3
      "results" -> """{"foo":"bar"}"""
    )
    val data = AdreqAnalyzer.Data("111", "222", "333", 1, 2, 3, 4, props)
    AdreqAnalyzer.decodeAdreqType(data) shouldBe List(AdreqAnalyzer.AdreqData("111", 4, "place1", 5, 0))
  }
  
  "decodeAdreqType" should "only parse those results with place equal to 1" in {
    val props = Map(
      "requests" -> """{"foo":"bar"}""",  
      "results" -> """{"place1":{"1":2, "2":3}}""" // count only "1":2
    )
    val data = AdreqAnalyzer.Data("111", "222", "333", 1, 2, 3, 4, props)
    AdreqAnalyzer.decodeAdreqType(data) shouldBe List(AdreqAnalyzer.AdreqData("111", 4, "place1", 0, 2))
  }
  
  "decodeAdreqType" should "parse props in Data into List[AdreqData] when results have string and integer" in {
    val props = Map(
      "requests" -> """{"foo":"bar"}""",  
      "results" -> """{"place1":{"1":"3"}, "place2":{"1":5}}""" // String: 3 & Int: 5
    )
    val data = AdreqAnalyzer.Data("111", "222", "333", 1, 2, 3, 4, props)
    AdreqAnalyzer.decodeAdreqType(data) shouldBe List(AdreqAnalyzer.AdreqData("111", 4, "place1", 0, 3), AdreqAnalyzer.AdreqData("111", 4, "place2", 0, 5))
  }
  
  "decodeAdreqType" should "parse props in Data into List[AdreqData] when there are multiple requests and results" in {
   val props = Map(
      "requests" -> """{"place1":{"1":["a","b","c"]}, "place2":{"2":5}, "place3":{"3":"7"}}""",  // List size: 3, Int: 5, String: 7
      "results" -> """{"place1":{"1":"3"}, "place2":{"1":5}}""" // String: 3 & Int: 5
    )
    val data = AdreqAnalyzer.Data("111", "222", "333", 1, 2, 3, 4, props)
    AdreqAnalyzer.decodeAdreqType(data) shouldBe 
      List(AdreqAnalyzer.AdreqData("111", 4, "place1", 0, 3), 
           AdreqAnalyzer.AdreqData("111", 4, "place2", 0, 5),
           AdreqAnalyzer.AdreqData("111", 4, "place1", 3, 0), 
           AdreqAnalyzer.AdreqData("111", 4, "place2", 5, 0),
           AdreqAnalyzer.AdreqData("111", 4, "place3", 7, 0)      
      )
  }
  

  
  
  
//  "prepareDeltaRdd" should """1. filter out row without GENERAL_REQUIRED_KEYS 
//                              2. generate key value pair by combination of ids 
//                              3. reduce by keep the one with smaller server_time""" in {
//   
//     import org.apache.spark.sql.SparkSession
//     import org.apache.spark.sql.functions._
//     val spark: SparkSession =
//       SparkSession
//         .builder()
//         .appName("Time Usage")
//         .config("spark.master", "local[2]")
//         .getOrCreate()
//     val sc = spark.sparkContext
//     sc.setLogLevel("ERROR")     
//
//     // delete this event because it has no crystal_id
//     val event1 = """{"type":"ad_request","geo_id":111,"crystal_id":"","st":789,"time":789000,"device_id":"456"}"""
//      
//     // delete this event because its server_time and client_time have difference more than 4 hour
//     val event2 = """{"type":"ad_request","geo_id":111,"crystal_id":"123","st":30000,"time":10000000,"device_id":"456"}"""
//    
//     // add null_geo_id to this event
//     val event3 = """{"type":"ad_request","crystal_id":"111","st":789,"time":789000,"device_id":"456"}"""
//     
//     // reduce this two by delete event4 because it has larger server_time
//     val event4 = """{"type":"ad_request","geo_id":111,"crystal_id":"222","st":790, "time":789000,"device_id":"456"}"""
//     val event5 = """{"type":"ad_request","geo_id":111,"crystal_id":"222","st":789, "time":789000,"device_id":"456"}"""
//
//     val null_geo_id = Mediation.INVALID_GEO_ID
//
//     var rdd = sc.parallelize(List(event1, event2, event3, event4, event5))
//     val result = List((s"111|!$$@#||456|!$$@#||789000|!$$@#||$null_geo_id|!$$@#||ad_request", "111|456|789|" + event3.dropRight(1) + s"""',"geo_id":$null_geo_id}"""),
//                       ("222|!$@#||456|!$@#||789000|!$@#||111|!$@#||ad_request", "222|456|789|" + event5))
//     Mediation.prepareDeltaRdd(rdd).collect.toList shouldBe result
//  }
  
//  "convertTimeToS3PartitionPath" should "convert datetime object to string with s3 format" in {
//    val dt = new DateTime("2017-06-05T02:34:56");
//    println(dt.toString("Y|MM|dd|HH").split('|').toList)
//    Mediation.convertTimeToS3PartitionPath(dt) shouldBe "year=2017/month=06/day=05/hour=02"
//  }
}
