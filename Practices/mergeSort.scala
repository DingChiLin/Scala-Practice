object MergeSort {
  def main(args: Array[String]): Unit = {
    val A = Array(1,7,2,5,4,3,8,6)
    val result = mergeSort(A)
    result.foreach(print)
  }

  def mergeSort(arr: Array[Int]): Array[Int] = {
    val l = arr.length
    val m = l / 2
    if(l == 1){
      return arr
    }else{
      val leftArr = mergeSort(arr.slice(0,m))
      val rightArr = mergeSort(arr.slice(m,l))

      var leftIndex = 0
      var rightIndex = 0

      var result = scala.collection.mutable.ArrayBuffer.empty[Int]
      while(leftIndex < leftArr.length && rightIndex < rightArr.length){
        if(leftArr(leftIndex) < rightArr(rightIndex)){
          result += leftArr(leftIndex)
          leftIndex += 1
        }else{
          result += rightArr(rightIndex)
          rightIndex += 1
        }
      }

      result ++= leftArr.slice(leftIndex, leftArr.length)
      result ++= rightArr.slice(rightIndex, rightArr.length)

      return result.toArray
    }
  }
}
