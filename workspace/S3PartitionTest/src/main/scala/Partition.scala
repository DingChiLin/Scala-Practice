import sys.process._

import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.apache.spark.sql.expressions.Window
import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.functions._
import org.apache.spark.SparkConf

//import java.io.File
import java.sql.DriverManager
import java.util.Date
import java.io.FileInputStream

import com.github.nscala_time.time.Imports._
import org.scalameter._
import play.api.libs.json._
import collection.JavaConverters._
//import scopt.OptionParser  

// cloud watch & s3
import com.amazonaws.services.cloudwatch.AmazonCloudWatchClient
import com.amazonaws.services.cloudwatch.model._
import com.amazonaws.auth.BasicAWSCredentials 
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model._

trait ConfigParser{

  case class Argument(
                 isLocal: Boolean = false,
                 bucket: String = "",
                 filePath: String = "/ce-global-prod.json",
                 table: String = "",
                 startTime: String = "",
                 endTime: String = "",
                 activeHour: Int = 100
               )

  val parser = new scopt.OptionParser[Argument]("scopt") {
    head("scopt", "3.x")

    opt[Boolean]('l', "is_local").action( (x, c) =>
      c.copy(isLocal = x) ).text("local mode")

    opt[String]('b', "bucket").action( (x, c) =>
      c.copy(bucket = x) ).text("config s3 path bucket")

    opt[String]('p', "file_path").action( (x, c) =>
      c.copy(filePath = x) ).text("config s3 path")

    opt[String]('t', "table").action( (x, c) =>
      c.copy(table = x) ).text("t")

    opt[String]('s', "startTime").action( (x, c) =>
      c.copy(startTime = x) ).text("process will start at startTime")

    opt[String]('e', "endTime").action( (x, c) =>
      c.copy(endTime = x) ).text("process will terminate after endTime")

    opt[Int]('a', "activeHour").action( (x, c) =>
      c.copy(activeHour = x) ).text("how long will this process active before kill itself")
  }

  def parseConfig(argument: Argument): Map[String, String] = {
    val json:JsValue = if(argument.isLocal){
      val path = getClass.getResource(argument.filePath).getPath
      val configString = new FileInputStream(path)
      Json.parse(configString)
    }else{
      val s3Client = new AmazonS3Client()
      val s3Object = s3Client.getObject(new GetObjectRequest(argument.bucket, argument.filePath))
      val configString = scala.io.Source.fromInputStream(s3Object.getObjectContent).getLines.mkString
      Json.parse(configString)
    }

    val mysql = json \ "mysql"
    val host = (mysql \ "host").as[String]
    val db = (mysql \ "database").as[String]
    val aws = json \ "aws" \ "cn"
    val cloudWatch = json \ "cloudwatch"

    Map(
      // local mode
      "isLocal" -> argument.isLocal.toString,

      // start and end
      "startTime" -> argument.startTime,
      "endTime" -> argument.endTime,
      "activeHour" -> argument.activeHour.toString,

      // sql
      "driver" -> "com.mysql.jdbc.Driver",
      "url" -> s"jdbc:mysql://$host/$db",
      "username" -> (mysql \ "account").as[String],
      "password" -> (mysql \ "password").as[String],
      "adreqResultTable" -> argument.table,

      // s3
      "awsKey" -> (aws \ "accessKeyId").as[String],
      "awsSecret" -> (aws \ "secretAccessKey").as[String],
      "sourceBucket" -> (json \ "s3" \ "veryRawLogBucket").as[String],

      // cloud watch
      "cloudWatchEndPoint" -> (cloudWatch \ "endpoint" \ "realtime-scala").as[String],
      "cloudWatchNameSpace" -> (cloudWatch \ "namespaces" \ "adreq").as[String],
      "cloudWatchMetricName" -> "ReportTaskPreparationToComplete",
      "cloudWatchDimension1Name" -> "ReportTaskTypeName",
      "cloudWatchDimension1Value" -> "Adreq",
      "cloudWatchDimension2Name" -> "ReportTaskTypeVersion",
      "cloudWatchDimension2Value" -> "1.0"
    )
  }

  def parse(args: Array[String]): Map[String, String] = {
    parseConfig(parser.parse(args, Argument()).get)
  }
}

object Partition extends ConfigParser{

  def main(args: Array[String]) {

    println(args.toList)
    val config = parse(args)
    println(config)
    val analyzer = new PartitionCreator(config)
    analyzer.process()
  }

}

case class Data(crystal_id:String, device_id:String, event_type:String, st: Long, time:Long, geo_id:Long, country_geo_id:Long, props:scala.collection.Map[String, String])

class PartitionCreator(CONFIG:Map[String, String]) {

  val INVALID_GEO_ID:Int = -321
  val PARTITION_SIZE:Int = 100
  val GENERAL_REQUIRED_KEYS:List[String] = List("type", "time")
  val MINIMUM_DELAY:Int = 3
  val ACTIVE_HOUR:Int = CONFIG("activeHour").toInt

  val S3BUCKET = s"s3://${CONFIG("sourceBucket")}/AD"

  val ENDTIME:DateTime = getEndTime(CONFIG("endTime"))

  def getEndTime(configEndTime:String):DateTime = {
    if(configEndTime == ""){
      DateTime.now + 100.year
    }else{
      DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(configEndTime)
    }
  }

  def getTimeShift(configStartTime:String): Period = {
    if(configStartTime == ""){
      0.second.toPeriod
    }else{
      val startTime = DateTimeFormat.forPattern("yyyy-MM-dd HH:mm:ss").parseDateTime(configStartTime)
      if(startTime < DateTime.now){
        (startTime to DateTime.now).toPeriod
      }else{
        0.second.toPeriod
      }
    }
  }

  val timeShift:Period = getTimeShift(CONFIG("startTime"))
  def effectiveTimeNow():DateTime = {
    DateTime.now - timeShift
  }

  def isExceedEndTime(finishTime:DateTime): Boolean ={
    if(finishTime > ENDTIME){
      println(s"process termintated at time $finishTime because it exceed end time")
      true
    }else{
      false
    }
  }

  // 讓他在 HH:05:00 ~ HH:10:00 的區間內重啟，避免重撈history data浪費時間
  def isExceedActiveHour(finishTime:DateTime, startHour:DateTime): Boolean ={
    if(finishTime > startHour + ACTIVE_HOUR.hours &&
       finishTime > finishTime.minute(0) + 6.minutes &&
       finishTime < finishTime.minute(0) + 10.minutes){
      println(s"process termintated at time $finishTime because it exceed active hour")
      true
    }else {
      false
    }
  }

  val sparkConf:SparkConf =
    if(CONFIG("isLocal").toBoolean){
      new SparkConf()
        .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        .set("spark.master", "local[4]")
        .registerKryoClasses(Array(classOf[Data]))
    }else{
      new SparkConf()
        .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
        .registerKryoClasses(Array(classOf[Data]))
    }

  val spark:SparkSession = SparkSession
    .builder()
    .appName("Time Usage")
    .config(sparkConf)
    .getOrCreate()

  spark.sparkContext.setLogLevel("ERROR")
  import spark.implicits._

  def process(): Unit ={

    val sourceBucket = if(CONFIG("isLocal").toBoolean) "/Users/arthurlin/Desktop/aws_s3_cleansed_ad_files" else "s3://ce-production-cleansed-event-logs/AD"
    println(sourceBucket)
    val startHour = effectiveTimeNow().minute(0).second(0).millis(0)


    // long loop to analyze data each MINIMUM_DELAY minute
    loop(startHour)
    def loop(hour:DateTime):Unit = {
      println(hour)

      val url = sourceBucket + "/" + convertTimeToS3PartitionPath(hour) + "/"
      val df = spark.sqlContext.read.schema(buildSchema()).json(url)

      val dfDummy = df.withColumn("country_geo_id", col("geo_id") - col("geo_id") % 1000000 )

      dfDummy.show()

      val testBucket1 = sourceBucket //"s3://arthur-test/parquet-event-log"
      val testUrl1 = testBucket1 + "/" + convertTimeToS3PartitionPath(hour + 2.hour) + "/"
      dfDummy.write.format("parquet").save(testUrl1)


//      loop(hour + 1.hour)
    }
  }

  def convertTimeToS3PartitionPath(refTime:DateTime): String = {
    val dateInfo = refTime.toString("Y|MM|dd|HH").split('|')
    s"year=${dateInfo(0)}/month=${dateInfo(1)}/day=${dateInfo(2)}/hour=${dateInfo(3)}"
  }

  def convertTimeToS3PartitionPath2(refTime:DateTime): String = {
    val dateInfo = refTime.toString("Y|MM|dd|HH").split('|')
    s"hour=${dateInfo(3)}/day=${dateInfo(2)}/month=${dateInfo(1)}/year=${dateInfo(0)}"
  }

  def buildSchema(): StructType = {
    
    println("build schema")
    /**
    * Event Format Example:
    * {"st":1492646405,"device_id":"1bb98ec719994e99963c6979f2a0dfa9","crystal_id":"169b7928a122421f851bbb6b166d5230","nt":2,"cat":"ADREQ","time":1492646354190,"geo_id":1076000000,"ug":"6","app_version":"5.16.6","sdk_version":30190200,"type":"ad_request","idfa":"de4f5e7d-28a1-4008-af4f-ba15d2e0b9ad","props":{"results":{"SCREEN_SAVER":{"9":1}},"requests":{"SCREEN_SAVER":{"1":["1492646102614"]}},"elapsed_time":300103},"version":18}
    */ 
    StructType(
      Array(
        StructField("crystal_id", StringType, nullable = true),
        StructField("device_id", StringType, nullable = false),
        StructField("geo_id", LongType, nullable = true),
        StructField("country_geo_id", LongType, nullable = true),
        StructField("cat", StringType, nullable = true),
        StructField("version", IntegerType, nullable = true),
        StructField("nt", IntegerType, nullable = true),
        StructField("st", LongType, nullable = true),
        StructField("time", LongType, nullable = true),
        StructField("type", StringType, nullable = true),
        StructField("app_version", StringType, nullable = true),
        StructField("sdk_version", StringType, nullable = true),
        StructField("idfa", StringType, nullable = true),
        StructField("creative_id", LongType, nullable = true),
        StructField("total_file_size", LongType, nullable = true),
        StructField("audience_tags", StringType, nullable = true),
        StructField("props", MapType(StringType,
          StringType
        ), nullable = false)
      )
    )  
  }

}
