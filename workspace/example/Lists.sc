import org.apache.spark.SparkConf
import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._

import org.apache.spark.rdd.RDD
import org.scalameter._

object Main {
  println("Welcome to the Scala worksheet")       //> Welcome to the Scala worksheet
  
  val conf: SparkConf = new SparkConf().setMaster("local[4]").setAppName("RddTest")
                                                  //> conf  : org.apache.spark.SparkConf = org.apache.spark.SparkConf@369f73a2
  val sc: SparkContext = new SparkContext(conf)   //> Using Spark's default log4j profile: org/apache/spark/log4j-defaults.propert
                                                  //| ies
                                                  //| 17/04/13 16:01:08 INFO SecurityManager: Changing view acls to: arthurlin
                                                  //| 17/04/13 16:01:08 INFO SecurityManager: Changing modify acls to: arthurlin
                                                  //| 17/04/13 16:01:08 INFO SecurityManager: SecurityManager: authentication disa
                                                  //| bled; ui acls disabled; users with view permissions: Set(arthurlin); users w
                                                  //| ith modify permissions: Set(arthurlin)
                                                  //| 17/04/13 16:01:08 INFO Slf4jLogger: Slf4jLogger started
                                                  //| 17/04/13 16:01:08 INFO Remoting: Starting remoting
                                                  //| 17/04/13 16:01:08 INFO Remoting: Remoting started; listening on addresses :[
                                                  //| akka.tcp://sparkDriver@192.168.1.3:52413]
                                                  //| 17/04/13 16:01:08 INFO Utils: Successfully started service 'sparkDriver' on 
                                                  //| port 52413.
                                                  //| 17/04/13 16:01:08 INFO SparkEnv: Registering MapOutputTracker
                                                  //| 17/04/13 16:01:08 INFO SparkEnv: Registering BlockManagerMaster
                                                  //| 17/04/13 16:01:08 INFO DiskBlockManager: Created local directory at /var/
                                                  //| Output exceeds cutoff limit.
    
  val list1 = Array((1, "a"), (1, "z"), (2, "b"), (3, "c"))
                                                  //> list1  : Array[(Int, String)] = Array((1,a), (1,z), (2,b), (3,c))
  list1.groupBy(x => x._1)                        //> res0: scala.collection.immutable.Map[Int,Array[(Int, String)]] = Map(2 -> Ar
                                                  //| ray((2,b)), 1 -> Array((1,a), (1,z)), 3 -> Array((3,c)))
                                            
  val rdd1 = sc.parallelize(list1)                //> rdd1  : org.apache.spark.rdd.RDD[(Int, String)] = ParallelCollectionRDD[0] a
                                                  //| t parallelize at Main.scala:17
  rdd1.groupByKey().collect.toList                //> 17/04/13 16:01:09 INFO SparkContext: Starting job: collect at Main.scala:18
                                                  //| 17/04/13 16:01:09 INFO DAGScheduler: Registering RDD 0 (parallelize at Main.
                                                  //| scala:17)
                                                  //| 17/04/13 16:01:09 INFO DAGScheduler: Got job 0 (collect at Main.scala:18) wi
                                                  //| th 4 output partitions (allowLocal=false)
                                                  //| 17/04/13 16:01:09 INFO DAGScheduler: Final stage: Stage 1(collect at Main.sc
                                                  //| ala:18)
                                                  //| 17/04/13 16:01:09 INFO DAGScheduler: Parents of final stage: List(Stage 0)
                                                  //| 17/04/13 16:01:09 INFO DAGScheduler: Missing parents: List(Stage 0)
                                                  //| 17/04/13 16:01:09 INFO DAGScheduler: Submitting Stage 0 (ParallelCollectionR
                                                  //| DD[0] at parallelize at Main.scala:17), which has no missing parents
                                                  //| 17/04/13 16:01:10 INFO MemoryStore: ensureFreeSpace(1952) called with curMem
                                                  //| =0, maxMem=2061647216
                                                  //| 17/04/13 16:01:10 INFO MemoryStore: Block broadcast_0 stored as values in me
                                                  //| mory (estimated size 1952.0 B, free 1966.1 MB)
                                                  //| 17/04/13 16:01:10 INFO MemoryStore: ensureFreeSpace(1339)
                                                  //| Output exceeds cutoff limit.
                                                  
  rdd1.count                                      //> 17/04/13 16:01:10 INFO SparkContext: Starting job: count at Main.scala:20
                                                  //| 17/04/13 16:01:10 INFO DAGScheduler: Got job 1 (count at Main.scala:20) with
                                                  //|  4 output partitions (allowLocal=false)
                                                  //| 17/04/13 16:01:10 INFO DAGScheduler: Final stage: Stage 2(count at Main.scal
                                                  //| a:20)
                                                  //| 17/04/13 16:01:10 INFO DAGScheduler: Parents of final stage: List()
                                                  //| 17/04/13 16:01:10 INFO DAGScheduler: Missing parents: List()
                                                  //| 17/04/13 16:01:10 INFO DAGScheduler: Submitting Stage 2 (ParallelCollectionR
                                                  //| DD[0] at parallelize at Main.scala:17), which has no missing parents
                                                  //| 17/04/13 16:01:10 INFO MemoryStore: ensureFreeSpace(1112) called with curMem
                                                  //| =6828, maxMem=2061647216
                                                  //| 17/04/13 16:01:10 INFO MemoryStore: Block broadcast_2 stored as values in me
                                                  //| mory (estimated size 1112.0 B, free 1966.1 MB)
                                                  //| 17/04/13 16:01:10 INFO MemoryStore: ensureFreeSpace(845) called with curMem=
                                                  //| 7940, maxMem=2061647216
                                                  //| 17/04/13 16:01:10 INFO MemoryStore: Block broadcast_2_piece0
                                                  //| Output exceeds cutoff limit.
}