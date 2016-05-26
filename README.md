# OCspark 
Spark related contributions from OpenChai

Presently there are two primary contributions:  

<b>Locality Sensitive RDD - LsSinkRDD and LsSourceRDD</b>
The LsSourceRDD reads from local filesystem directories - allowing individual slices/partitions of data to run locally. The intended use case is iterative processing in machine learning algorithms.
The LsSinkRDD sets up that capability by writing data from other RDD's into the correct local filesystem structure as expected by LsSourceRDD's.

<b>Point to Point communication RDD - P2pRDD</b>
P2pRDD sets up a TcP channel between the Driver and each of the Workers. This channel can be used to provide instructions and/or data updates from Driver to Workers without incurring the I/O of reading RDD data afresh. 

The use case is allow repeatedly processing the data in a tight loop but potentially with different Machine Learning or Deep Learning parameters. The Workers would send back results - possibly in the form of updated Weights - to the Driver -who in turn sends updated parameters (and possibly weights) to the Worker.  

Two advantages can be derived from this:

(a) The Workers can run many loops on the in-memory RDD data
(b) Individual workers may complete and get updated parameters then continue to do additional processing - while other workers may not yet have completed.


<h3> How to build</h3>
mvn -DskipTests=true package

<h3> How to test </h3>
Testing P2pRDD:

spark-submit --master spark://\<host\>:7077 --jars $(pwd)/libs/spark_p2prdd-1.0.0.jar --class org.openchai.spark.rdd.P2pRDDTest $(pwd)/libs/spark_p2prdd-1.0.0-tests.jar spark://\<host\>:7077

Testing LsSinkRDD and LsSourceRDD:

spark-submit --master spark://\<host\>:7077 --jars $(pwd)/libs/spark_p2prdd-1.0.0.jar --class org.openchai.spark.rdd.P2pRDDTest $(pwd)/libs/spark_p2prdd-1.0.0-tests.jar spark://\<host\>:7077

Additional documentation -including details on the RDD capabilities of LSSink/LsSource and P2p - are  in the <b>doc</b> directory

Credits: conception, design, and development effort for this open source project have been supplied by [OpenChai](http://openchai.org/) and by Futurewei - a US research of [Huawei Inc](http://www.huawei.com/us/).  Funding has been contributed by [Huawei Inc](http://www.huawei.com/us/).
