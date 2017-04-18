object Lists {;import org.scalaide.worksheet.runtime.library.WorksheetSupport._; def main(args: Array[String])=$execute{;$skip(58); 
  println("Welcome to the Scala worksheet");$skip(62); 
  
  val list1 = List((1, "a"), (1, "z"), (2, "b"), (3, "c"));System.out.println("""list1  : <error> = """ + $show(list1 ));$skip(12); 
  val x = 1;System.out.println("""x  : Int = """ + $show(x ));$skip(21); 
  val l = Seq(1,2,3);System.out.println("""l  : Seq[Int] = """ + $show(l ));$skip(25); 
  val arr = Array(1,2,3);System.out.println("""arr  : Array[Int] = """ + $show(arr ))}
}
