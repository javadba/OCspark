package org.openchai.spark.rdd

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.openchai.tcp.rpc.{P2pConnectionParams, SolverServerIf}

import scala.reflect.ClassTag

object SolverRDD {
  val weightsMergePolicy: String = "best"
}

class SolverRDD[KVO:ClassTag,T:ClassTag](sc: SparkContext, parent: RDD[KVO], p2pParams: P2pConnectionParams)
  extends P2pRDD[KVO,T](sc, parent, p2pParams, new SolverServerIf(SolverRDD.weightsMergePolicy)) {
}
