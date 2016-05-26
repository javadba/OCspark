/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.spark.ml

import org.apache.spark.SparkContext
import org.apache.spark.ml.classification._
import org.apache.spark.ml.feature.StringIndexer
import org.apache.spark.ml.util.MLWritable
import org.apache.spark.mllib.linalg.{BLAS, Vector, DenseVector}
import org.apache.spark.sql.{SQLContext, DataFrame}

object MLProxy {
  def axpy(a: Double, x: Vector, y: Vector) = BLAS.axpy(a, x, y)
    case class P(id: Int, name: String,category: String, features: Vector)

  def main(args: Array[String]) = {

    val A = Array
    val v: Vector = new DenseVector(A(1.0,2.0,3.0)).asInstanceOf[Vector]
    val sc = new SparkContext("local[*]", "app")
    val ssc = new SQLContext(sc)
    val df = ssc.createDataFrame(Seq(P(1, "a","c1",new DenseVector(A(1.0,2.0,3.0))),
      P(2, "ab","c1",new DenseVector(A(2.0,3.0,4.0)))))
    //    val df = ssc.sql("create table x")
    //
    ////    val rfc = new RandomForestClassifier("uid")
    //    val rfc = new DecisionTreeClassifier("uid")
    //    val m = rfc.train(df)
    ////    val m = new RandomForestClassificationModel(trees, 1, 1)
    //    val trees = Array(new DecisionTreeClassifier().asInstanceOf[DecisionTreeClassificationModel])
    //
//
//    val indexer = new StringIndexer()
//      .setInputCol("name")
//      .setOutputCol("label")
//
//    val rf = new RandomForestClassifier()
//      .setNumTrees(3)
//      .setFeatureSubsetStrategy("auto")
//      .setImpurity("gini")
//      .setMaxDepth(4)
//      .setMaxBins(32)
//
//    val pipeline = new Pipeline()
//      .setStages(Array(indexer, rf))
//
//    val model = pipeline.fit(df)
//
//    val df2 = model.transform(df)
//
//    df2.show
//    model.asInstanceOf[MLWritable].write.save("/tmp/model")

//    val ds = ssc.createDataset(Seq("1,2,3","4,5,6"))
////    val dat = ssc.read.(sc.parallelize(factors))
//    val cc = ds.map(_.split(","))
//    cc.select(Dataset.expr("sum(*)").as[Double])

//    val aa = sc.parallelize(Seq((1,2),(3,4)))
//      val a = a.toDF("a", "b")
//    a.show
//
//    a.collect.map(_.toSeq)
  }
}
