java -Dlogger.level=2 -classpath $GITDIR/tfspark/target/classes:$GITDIR/tfspark/libs/* -Djava.net.preferIPv4Stack=true org.openchai.tensorflow.SparkSubmitter /shared/conf/submitter2.yml
