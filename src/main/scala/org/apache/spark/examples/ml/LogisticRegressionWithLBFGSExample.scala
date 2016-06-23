package org.apache.spark.examples.ml

import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.feature.LabeledPoint
import org.apache.spark.ml.linalg.{Vectors, SparseVector}
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.sql.{SQLContext, SparkSession}
import org.apache.spark.{SparkConf, SparkContext}

object LogisticRegressionWithLBFGSExample {

  def main(args: Array[String]): Unit = {
    val master = args(1)
    val conf = new SparkConf().setAppName("LogisticRegressionWithLBFGSExample")
      .setMaster(master)
    val sc = new SparkContext(conf)

    // $example on$
    // Load training data in LIBSVM format.
    val mllibData = MLUtils.loadLibSVMFile(sc, "data/mnist/train/mnist.train")
    val data = mllibData.map { p => p.asML }

    val splits = data.randomSplit(Array(0.8, 0.2), seed = 11L)

    val sqlContext = new SQLContext(sc)
    import sqlContext.implicits._
    val training = splits(0).cache()
    val test = splits(1)
    val trainDs = training.toDS()

    //    val pointsTrainDf =  sqlc.createDataFrame(training)
    //    val  pointsTrainDs = pointsTrainDf.as[LabeledPoint]

    val sparkSession = SparkSession.builder().master(master).getOrCreate()
    val pointsTrainDs = sparkSession.createDataset(training)

    // Run training algorithm to build the model
    //    val model = new LogisticRegressionWithLBFGS()
    val model = new LogisticRegression()
      //      .setNumClasses(10)
      .train(pointsTrainDs.as[LabeledPoint])

    val pointsTestDs = sparkSession.createDataset(test)
    // Compute raw scores on the test set.
    val prediction = model.evaluate(pointsTestDs)

    // Compute raw scores on the test set.
    //    val predictionAndLabels = test.map { case LabeledPoint(label, features) =>
    //      val prediction = model.evaluate(features)
    //      (prediction, label)
    //    }

    val preds = prediction.predictions

    val view = preds.createOrReplaceTempView("predictions")
    val predsDs = sqlContext.sql(s"select ${prediction.probabilityCol},${prediction.featuresCol}, ${prediction.labelCol} from predictions")

    val out = predsDs.collect
    println(s"nrows = ${out.length}")
    //    val predictionAndLabels = preds.select(prediction.labelCol, prediction.probabilityCol, prediction.featuresCol) { case LabeledPoint(label, features) =>
    //    val prediction = model.evaluate(predsDs)
    //      (prediction, label)
    //    }

    //    preds.
    //
    //    // Get evaluation metrics.
    //    val metrics = new MulticlassMetrics(predictionAndLabels)
    //    val precision = metrics.precision
    //    println("Precision = " + precision)

    // Save and load model
    //    val modelDir = "target/tmp/scalaLogisticRegressionWithLBFGSModel"
    //    FileUtils.rmdirs(modelDir)
    //    model.save(sc, modelDir)
    //    val sameModel = LogisticRegressionModel.load(sc, modelDir)
    //    val testData = MLUtils.loadLibSVMFile(sc, "data/mnist/test")
    //    val predsAndLabels = model.predict(testData.map{ p => p.features }).zip(testData.map(_.label))
    //    val nerrs = predsAndLabels.filter{ case (p,lab) => math.abs(p -lab) > 0.01 }.count
    // $example off$
    //    println(s"Reloaded model weight= ${sameModel.weights}")
    //    println(s"Number of correct: ${predsAndLabels.count-nerrs} errors=$nerrs")

    sc.stop()
  }
}
