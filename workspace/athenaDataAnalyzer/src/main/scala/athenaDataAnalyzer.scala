import java.io.File
import com.github.tototoshi.csv._

object athenaDataAnalyzer{
  def main(args: Array[String]) = {  
    println("hi")
    idfa_analysis
  }  
  
  def idfa_analysis {
 
    val ios_cleansed_app_open = CSVReader.open(new File("/Users/arthurlin/Downloads/IOS_cleansed_app.csv")).allWithHeaders().map(x => Map("idfa" -> x("idfa").toLowerCase, "count" -> x("_col1")))
    val android_cleansed_app_open = CSVReader.open(new File("/Users/arthurlin/Downloads/Android_cleansed_app.csv")).allWithHeaders().map(x => Map("idfa" -> x("idfa").toLowerCase, "count" -> x("_col1")))
    val ios_cleansed_adreq = CSVReader.open(new File("/Users/arthurlin/Downloads/IOS_cleansed_adreq.csv")).allWithHeaders().map(x => Map("idfa" -> x("idfa").toLowerCase, "count" -> x("_col1")))
    val android_cleansed_adreq = CSVReader.open(new File("/Users/arthurlin/Downloads/Android_cleansed_adreq.csv")).allWithHeaders().map(x => Map("idfa" -> x("idfa").toLowerCase, "count" -> x("_col1")))
    
    val all_cleansed_app = 0
    val all_cleansed_adreq = 0
   
    println(ios_cleansed_app_open)
    
    // Size of all data  
    println(ios_cleansed_app_open.map(_("idfa")).size)
    println(android_cleansed_app_open.map(_("idfa")).size)
    println(ios_cleansed_adreq.map(_("idfa")).size)
    println(android_cleansed_adreq.map(_("idfa")).size)
    
    
    
    // check if all idfa in ios_cleansed_adreq is also in ios_cleansed_app
    val ios_adreq_not_app = ios_cleansed_adreq.map(_("idfa")).filterNot(ios_cleansed_app_open.map(_("idfa")).toSet)
    println(ios_adreq_not_app)
    println(ios_adreq_not_app.size)
    val android_adreq_not_app = android_cleansed_adreq.map(_("idfa")).filterNot(android_cleansed_app_open.map(_("idfa")).toSet)
    println(android_adreq_not_app)
    println(android_adreq_not_app.size)
    
    // check if all those idfa have count > 20 in app but not in adreq
    println(ios_cleansed_app_open
      .filter(x => x("count").toInt > 20)
      .map(_("idfa"))
      .size
    )
    
    println(
      ios_cleansed_app_open
        .filter(x => x("count").toInt > 20).map(_("idfa"))
        .filterNot(
           ios_cleansed_adreq.map(_("idfa")).toSet
        )
        .size
    )
    
    println(ios_cleansed_app_open.filter(x => x("idfa") == "e4bd46c9-4c94-4b1b-b94b-d7928d32c8d3"))
    println(ios_cleansed_adreq.filter(x => x("idfa") == "e4bd46c9-4c94-4b1b-b94b-d7928d32c8d3"))
 
    
    
    println(android_cleansed_app_open
      .filter(x => x("count").toInt > 20)
      .map(_("idfa"))
      .size
    )
    
    println(
      android_cleansed_app_open
        .filter(x => x("count").toInt > 20).map(_("idfa"))
        .filterNot(
           android_cleansed_adreq.map(_("idfa")).toSet
        )
        .size
    )
    
    println(android_cleansed_app_open.filter(x => x("idfa") == "c56734c6-61d7-4b36-907c-1debbfe92480"))
    println(android_cleansed_adreq.filter(x => x("idfa") == "c56734c6-61d7-4b36-907c-1debbfe92480"))
 
  }
  
  
}
