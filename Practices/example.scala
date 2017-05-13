object Example extends App{
	
	println(example1(1, 10)) // 45
	//println(example2(1, 10))
  //println(example3(1, 10))
  //println(example4(1, 10))

  println(genList(1, 10)) // List(1,2,3,4,5,6,7,8,9,10)

  // traditional
	def example1(x:Int, y:Int):Int = {
    var acc = 0
    for(i <- x until y){
      acc += i
    }
    return acc
	}

  // no return
	def example2(x:Int, y:Int):Int = {
    var acc = 0
    for(i <- x until y){
      acc += i
    }
    acc
  }

  // no mutable variable, no loop
  def example3(x:Int, y:Int):Int = {
    if(x >= y) 0
    else x + example3(x+1, y)
  }

  // tail recursive
  def example4(x:Int, y:Int):Int = {
    def tail(z:Int, acc:Int):Int = {
      if(z >= y) acc
      else tail(z+1, acc + z) 
    }
    tail(x, 0)
  }

  // generate a list
  def genList(x:Int, y:Int):List[Int] = {
    if(x > y) List()
    else x::genList(x+1, y)
  }

  val x = 0
  x match {
    //case y if y < 5 => println("x<5")
    case 0 => println("0")
    case y:Int => println("INT")
  }



}
