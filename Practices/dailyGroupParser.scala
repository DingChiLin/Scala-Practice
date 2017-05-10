import collection.immutable.ListMap
object DailyGroupParser extends App{
  val bufferedSource = io.Source.fromFile("/Users/arthurlin/Downloads/ea4f971e-6bd1-477f-9860-212a3856faef.csv")

  var date = 21
  val dateCount = scala.collection.mutable.Map[Int, Int]()
  for (line <- bufferedSource.getLines.drop(1)) {
    val cols = line.split(",").map(_.trim.replace("\"", ""))
    val hour = cols(1).toInt
    val count = cols(2).toInt
    if(hour == 22){ 
      date += 1
    }
    if(dateCount.contains(date)){
      dateCount(date) += count
    }else{ 
      dateCount(date) = count
    }
  }
  bufferedSource.close

  val result = ListMap(dateCount.toSeq.sortBy(_._1):_*)

  var csvString = ""
  println("day,count")
  result.foreach{
    case (key, value) => {
      println(key + "," + value)
    }
  }
}
