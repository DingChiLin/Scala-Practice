package recfun

object Main {
  def main(args: Array[String]) {
    println("Pascal's Triangle")
    for (row <- 0 to 10) {
      for (col <- 0 to row)
        print(pascal(col, row) + " ")
      println()
    }
    
    val chars = ":)".toList
    println(balance(chars))
    
    val money = 10
    val coins = List(2,3,4)
    println(countChange(money, coins))
  }

  /**
   * Exercise 1 
   */
    def pascal(c: Int, r: Int): Int = {
      if(c > r || c < 0){
        0
      }else if(c == 0){
        1
      }else{
        pascal(c, r-1) + pascal(c-1, r-1)
      }
    }
  
  /**
   * Exercise 2
   */
    def balance(chars: List[Char]): Boolean = {
      if(chars.isEmpty){true}
      
      def balancer(chars:List[Char], bal:Int = 0):Int = {                
        if(bal < 0){
          -1
        }else if(chars.isEmpty){
          if(bal != 0){
            -1
          }else{
            bal
          }
        }else{ 
          if(chars.head == '('){
            balancer(chars.tail, bal + 1)
          }else if(chars.head == ')'){
            balancer(chars.tail, bal - 1)
          }else{
            balancer(chars.tail, bal)
          }
        }
      }
      
      balancer(chars) == 0
    }
  
  /**
   * Exercise 3
   */
    def countChange(money: Int, coins: List[Int]): Int = {
      money match {
        case _ if money < 0 || coins.length <= 0 =>{
          0
        }
        case 0 => {
          1 
        }
        case _ => {
          countChange(money - coins.head, coins) + countChange(money, coins.tail) 
        }
      }
    }
  }
