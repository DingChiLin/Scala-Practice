package timeusage

import java.nio.file.Paths

import org.apache.spark.sql._
import org.apache.spark.sql.types._
import org.scalameter._
import org.apache.spark.rdd.RDD

/** Main class */
object TimeUsage {

  import org.apache.spark.sql.SparkSession
  import org.apache.spark.sql.functions._

  val spark: SparkSession =
    SparkSession
      .builder()
      .appName("Time Usage")
      .config("spark.master", "local[2]")
      .getOrCreate()
  
  spark.sparkContext.setLogLevel("ERROR")  

  // For implicit conversions like converting RDDs to DataFrames
  import spark.implicits._

  /** Main function */
  def main(args: Array[String]): Unit = {
    timeUsageByLifePeriod()
  }

  def timeUsageByLifePeriod(): Unit = {
    val (columns, initDf) = read("/timeusage/large_atussum.csv")
    val (primaryNeedsColumns, workColumns, otherColumns) = classifiedColumns(columns)
    val summaryDf = timeUsageSummary(primaryNeedsColumns, workColumns, otherColumns, initDf).cache
    val a = summaryDf.collect // do an action to cache data for benchmark
    val summaryDs = timeUsageSummaryTyped(summaryDf)
    val summaryRdd = summaryDs.rdd

    import org.apache.spark.sql.expressions.scalalang.typed
    
    timed("RDD Group Count", 
        summaryRdd.groupBy(x => (x.age))
                  .mapValues(x => x.map(z => (z.primaryNeeds, 1.0)).reduce((sum, x) => (sum._1 + x._1, sum._2 + x._2))) // 2316
                  .mapValues{ case (need, count) => (need/count) }.collect)
    timed("DataFrame Group Count", summaryDf.groupBy($"age").agg(avg("primaryNeeds").as("primaryNeeds")).collect) // 757 
    timed("DataSet Group Count", summaryDs.groupByKey(_.age).agg(typed.avg(_.primaryNeeds)).collect) // 1021
    timed("DataSet Group (write as DataFrame) Count", summaryDs.groupBy($"age").agg(avg("primaryNeeds").as("primaryNeeds")).collect) // 685
    
    val result = summaryDs.groupByKey(_.age).agg(avg("primaryNeeds").as("primaryNeeds").as[Double]).collect
    
    
//    val rddResult = timeUsageGroupedRDD(summaryRDD)
//    println(rddResult.collect.toList)
//    timed("RDD Group Aggregate Multiple Column", timeUsageGroupedRDD(summaryRDD).collect())  // 796  =>  4149

//    val dfResult = timeUsageGrouped(summaryDf)
//    dfResult.show()
//    timed("DataFrame Group Aggregate Multiple Column", timeUsageGrouped(summaryDf).collect())  // 1075  =>  1297
//    timed("DataSet Group Aggregate Multiple Column", timeUsageGroupedTyped(summaryDS).collect())  // 1613  =>  2516
//    timed("DataSet to DataFrame and Group Aggregate Multiple Column", timeUsageGrouped(summaryDS.toDF).collect()) // 1052  =>  1194


//    val t0_DF = System.nanoTime()
//    val finalDf = summaryDf.groupBy($"age").count.collect()
//    finalDf.show() 
//    val t1_DF = System.nanoTime()
//    println("Elapsed time DF: " + (t1_DF - t0_DF)/1000000 + "ms")

//    val t0_DF2 = System.nanoTime()
//    val finalDf2 = summaryDf.groupBy($"age").count.collect()
//    finalDf2.show()    
//    val t1_DF2 = System.nanoTime()
//    println("Elapsed time DF2: " + (t1_DF2 - t0_DF2)/1000000 + "ms")
    
//    val t0_SQL = System.nanoTime()
//    val finalDfsql = timeUsageGroupedSql(summaryDf)
//    finalDfsql.show()
//    val t1_SQL = System.nanoTime()
//    println("Elapsed time SQL: " + (t1_SQL - t0_SQL)/1000000 + "ms")
//
//
//    val t0_DS = System.nanoTime()
//    val finalDS = summaryDS.groupByKey(_.age).count.collect()
//    finalDS.show()
//    val t1_DS = System.nanoTime()
//    println("Elapsed time DS: " + (t1_DS - t0_DS)/1000000 + "ms")
//    
//    
//    val t0_DF_2 = System.nanoTime()
//    val finalDf2 = timeUsageGrouped(summaryDf)
//    finalDf2.show()    
//    val t1_DF_2 = System.nanoTime()
//    println("Elapsed time DF 2: " + (t1_DF_2 - t0_DF_2)/1000000 + "ms")
  }
  
  def timed[T](label: String, code: => T) = {
    println(s"""
      ######################################################################
        Timeing: $label
      ######################################################################
    """)
    val time = config(
      Key.exec.benchRuns -> 10
    ) measure {
      code
    }
    println(s"Total time: $time")
  }

  /** @return The read DataFrame along with its column names. */
  def read(resource: String): (List[String], DataFrame) = {

    val rdd = spark.sparkContext.textFile(fsPath(resource))
//    writeLargeRdd(rdd)
    val headerColumns = rdd.first().split(",").to[List]
    // Compute the schema based on the first line of the CSV file
    val schema = dfSchema(headerColumns)

    val data =
      rdd
        .mapPartitionsWithIndex((i, it) => if (i == 0) it.drop(1) else it) // skip the header line
        .map(_.split(",").to[List])
        .map(row)
    
    val dataFrame =
      spark.createDataFrame(data, schema)

    (headerColumns, dataFrame)
  }

  def writeLargeRdd(rdd: RDD[String]) = {
    var data = rdd
    
    val noHeadData =
      rdd
        .mapPartitionsWithIndex((i, it) => if (i == 0) it.drop(1) else it) // skip the header line
        
    for(i <- (1 to 10)){
      data = data.union(noHeadData)
    }    
    data.saveAsTextFile("large_atussum.csv")
  }
   
  /** @return The filesystem path of the given resource */
  def fsPath(resource: String): String =
    Paths.get(getClass.getResource(resource).toURI).toString

  /** @return The schema of the DataFrame, assuming that the first given column has type String and all the others
    *         have type Double. None of the fields are nullable.
    * @param columnNames Column names of the DataFrame
    */
  def dfSchema(columnNames: List[String]): StructType = {
    val structFields = columnNames.map(
      {
        case cn if cn == columnNames.head => StructField(cn, StringType, false)
        case cn => StructField(cn, DoubleType, false)
      }
    ) 
    println(structFields.toList)
    return StructType(structFields)
  }
    


  /** @return An RDD Row compatible with the schema produced by `dfSchema`
    * @param line Raw fields
    */
  def row(line: List[String]): Row = {
    val row = line.map(
      {
        case r if r == line.head => r
        case r => r.toDouble
      }
    )
    Row.fromSeq(row)
  }
  /** @return The initial data frame columns partitioned in three groups: primary needs (sleeping, eating, etc.),
    *         work and other (leisure activities)
    *
    * @see https://www.kaggle.com/bls/american-time-use-survey
    *
    * The dataset contains the daily time (in minutes) people spent in various activities. For instance, the column
    * “t010101” contains the time spent sleeping, the column “t110101” contains the time spent eating and drinking, etc.
    *
    * This method groups related columns together:
    * 1. “primary needs” activities (sleeping, eating, etc.). These are the columns starting with “t01”, “t03”, “t11”,
    *    “t1801” and “t1803”.
    * 2. working activities. These are the columns starting with “t05” and “t1805”.
    * 3. other activities (leisure). These are the columns starting with “t02”, “t04”, “t06”, “t07”, “t08”, “t09”,
    *    “t10”, “t12”, “t13”, “t14”, “t15”, “t16” and “t18” (those which are not part of the previous groups only).
    */
  def classifiedColumns(columnNames: List[String]): (List[Column], List[Column], List[Column]) = {
    import scala.collection.mutable.ListBuffer
    
    val need_keys = List("t01", "t03", "t11", "t1801", "t1803")
    val work_keys = List("t05", "t1805")
    val other_keys = List("t02", "t04", "t06", "t07", "t08", "t09", "t10", "t12", "t13", "t14", "t15", "t16", "t18")
    
    val needs = ListBuffer[Column]()
    val works = ListBuffer[Column]()
    val others = ListBuffer[Column]()
    
    columnNames.foreach(
      {
        case x if need_keys.exists(x.contains) => needs += new Column(x)
        case x if work_keys.exists(x.contains) => works += new Column(x)
        case x if other_keys.exists(x.contains) => others += new Column(x)  
        case _ => 
      }
    )
    
    return (needs.toList, works.toList, others.toList)
  }

  /** @return a projection of the initial DataFrame such that all columns containing hours spent on primary needs
    *         are summed together in a single column (and same for work and leisure). The “teage” column is also
    *         projected to three values: "young", "active", "elder".
    *
    * @param primaryNeedsColumns List of columns containing time spent on “primary needs”
    * @param workColumns List of columns containing time spent working
    * @param otherColumns List of columns containing time spent doing other activities
    * @param df DataFrame whose schema matches the given column lists
    *
    * This methods builds an intermediate DataFrame that sums up all the columns of each group of activity into
    * a single column.
    *
    * The resulting DataFrame should have the following columns:
    * - working: value computed from the “telfs” column of the given DataFrame:
    *   - "working" if 1 <= telfs < 3
    *   - "not working" otherwise
    * - sex: value computed from the “tesex” column of the given DataFrame:
    *   - "male" if tesex = 1, "female" otherwise
    * - age: value computed from the “teage” column of the given DataFrame:
    *   - "young" if 15 <= teage <= 22,
    *   - "active" if 23 <= teage <= 55,
    *   - "elder" otherwise
    * - primaryNeeds: sum of all the `primaryNeedsColumns`, in hours
    * - work: sum of all the `workColumns`, in hours
    * - other: sum of all the `otherColumns`, in hours
    *
    * Finally, the resulting DataFrame should exclude people that are not employable (ie telfs = 5).
    *
    * Note that the initial DataFrame contains time in ''minutes''. You have to convert it into ''hours''.
    */
  def timeUsageSummary(
    primaryNeedsColumns: List[Column],
    workColumns: List[Column],
    otherColumns: List[Column],
    df: DataFrame
  ): DataFrame = {
    val workingStatusProjection: Column = when($"telfs" >= 1 && $"telfs" < 3, "working").otherwise("not working").as("working")
    val sexProjection: Column = when($"tesex" === 1, "male").otherwise("female").as("sex")
    val ageProjection: Column = when($"teage" >= 15 && $"teage" <= 22, "young").when($"teage" >= 23 && $"teage" <= 55, "active").otherwise("elder").as("age")

    val primaryNeedsProjection: Column = primaryNeedsColumns.reduce(_ + _).divide(60).as("primaryNeeds")
    val workProjection: Column = workColumns.reduce(_ + _).divide(60).as("work")
    val otherProjection: Column = otherColumns.reduce(_ + _).divide(60).as("other")
    df
      .select(workingStatusProjection, sexProjection, ageProjection, primaryNeedsProjection, workProjection, otherProjection)
      .where($"telfs" <= 4) // Discard people who are not in labor force
  }

  /** @return the average daily time (in hours) spent in primary needs, working or leisure, grouped by the different
    *         ages of life (young, active or elder), sex and working status.
    * @param summed DataFrame returned by `timeUsageSumByClass`
    *
    * The resulting DataFrame should have the following columns:
    * - working: the “working” column of the `summed` DataFrame,
    * - sex: the “sex” column of the `summed` DataFrame,
    * - age: the “age” column of the `summed` DataFrame,
    * - primaryNeeds: the average value of the “primaryNeeds” columns of all the people that have the same working
    *   status, sex and age, rounded with a scale of 1 (using the `round` function),
    * - work: the average value of the “work” columns of all the people that have the same working status, sex
    *   and age, rounded with a scale of 1 (using the `round` function),
    * - other: the average value of the “other” columns all the people that have the same working status, sex and
    *   age, rounded with a scale of 1 (using the `round` function).
    *
    * Finally, the resulting DataFrame should be sorted by working status, sex and age.
    */
  def timeUsageGrouped(summed: DataFrame): DataFrame = {
//    summed.groupBy("working", "sex", "age")
//          .agg(round(avg("primaryNeeds"), 1).as("primaryNeeds"), 
//               round(avg("work"), 1).as("work"), 
//               round(avg("other"), 1).as("other"))
//          .orderBy("working", "sex", "age")
          
    summed.groupBy("working", "sex", "age")
          .agg(avg("primaryNeeds").as("primaryNeeds"), 
               avg("work").as("work"), 
               avg("other").as("other"))
          .orderBy("working", "sex", "age")
          }

  /**
    * @return Same as `timeUsageGrouped`, but using a plain SQL query instead
    * @param summed DataFrame returned by `timeUsageSumByClass`
    */
  def timeUsageGroupedSql(summed: DataFrame): DataFrame = {
    val viewName = s"summed"
    summed.createOrReplaceTempView(viewName)
    spark.sql(timeUsageGroupedSqlQuery(viewName))
  }

  /** @return SQL query equivalent to the transformation implemented in `timeUsageGrouped`
    * @param viewName Name of the SQL view to use
    */
  def timeUsageGroupedSqlQuery(viewName: String): String = {
    s"SELECT working, sex, age, ROUND(AVG(primaryNeeds), 1) AS primaryNeeds, ROUND(AVG(work), 1) AS work, ROUND(AVG(other), 1) AS other FROM $viewName GROUP BY working, sex, age ORDER BY working, sex, age"
  }
  /**
    * @return A `Dataset[TimeUsageRow]` from the “untyped” `DataFrame`
    * @param timeUsageSummaryDf `DataFrame` returned by the `timeUsageSummary` method
    *
    * Hint: you should use the `getAs` method of `Row` to look up columns and
    * cast them at the same time.
    */
  def timeUsageSummaryTyped(timeUsageSummaryDf: DataFrame): Dataset[TimeUsageRow] =
    timeUsageSummaryDf.as[TimeUsageRow]

  /**
    * @return Same as `timeUsageGrouped`, but using the typed API when possible
    * @param summed Dataset returned by the `timeUsageSummaryTyped` method
    *
    * Note that, though they have the same type (`Dataset[TimeUsageRow]`), the input
    * dataset contains one element per respondent, whereas the resulting dataset
    * contains one element per group (whose time spent on each activity kind has
    * been aggregated).
    *
    * Hint: you should use the `groupByKey` and `typed.avg` methods.
    */
  def timeUsageGroupedTyped(summed: Dataset[TimeUsageRow]): Dataset[TimeUsageRow] = {
    import org.apache.spark.sql.expressions.scalalang.typed
    
//    summed.groupByKey(x => (x.working, x.sex, x.age))
//          .agg(round(typed.avg[TimeUsageRow](_.primaryNeeds), 1).as[Double], 
//               round(typed.avg[TimeUsageRow](_.work), 1).as[Double],
//               round(typed.avg[TimeUsageRow](_.other), 1).as[Double])
//          .map{ case ((working, sex, age), primaryNeeds, work, other) => TimeUsageRow(working, sex, age, primaryNeeds, work, other) }
//          .orderBy("working", "sex", "age")
          
     summed.groupByKey(x => (x.working, x.sex, x.age))
        .agg(typed.avg(_.primaryNeeds), 
             typed.avg(_.work),
             typed.avg(_.other))
        .map{ case ((working, sex, age), primaryNeeds, work, other) => TimeUsageRow(working, sex, age, primaryNeeds.round, work.round, other.round) }
        .orderBy("working", "sex", "age")
  }
  
  def timeUsageGroupedRDD(summed: RDD[TimeUsageRow]): RDD[TimeUsageRow] = {
     summed.groupBy(x => (x.working, x.sex, x.age))
           .mapValues(x => x.map(z => (z.primaryNeeds, z.work, z.other, 1.0)).reduce((sum, x) => (sum._1 + x._1, sum._2 + x._2 , sum._3 + x._3, sum._4 + x._4)))
           .mapValues{ case (need, work, other, count) => (need/count, work/count, other/count) }
           .map{ case ((working, sex, age), (primaryNeeds, work, other)) => TimeUsageRow(working, sex, age, primaryNeeds.round, work.round, other.round) }
           .sortBy(x => (x.working, x.sex, x.age)) 
  }

}

/**
  * Models a row of the summarized data set
  * @param working Working status (either "working" or "not working")
  * @param sex Sex (either "male" or "female")
  * @param age Age (either "young", "active" or "elder")
  * @param primaryNeeds Number of daily hours spent on primary needs
  * @param work Number of daily hours spent on work
  * @param other Number of daily hours spent on other activities
  */
case class TimeUsageRow(
  working: String,
  sex: String,
  age: String,
  primaryNeeds: Double,
  work: Double,
  other: Double
)