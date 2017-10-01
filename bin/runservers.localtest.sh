cd tf
echo "gitdir=$GITDIR"
for port in 61234 61244 61254; do
	echo "starting servers in batch $port .."
  #nohup java -Xcheck:jni -XX:NativeMemoryTracking=summary -Dlogger.level=3 -Dopenchai.tfserver.config.file=/shared/conf/apps-config.yml -Djava.net.preferIPv4Stack=true -classpath $GITDIR/tf/target/tf-1.0.0.jar:$GITDIR/tf/libs/* org.openchai.tensorflow.TfServer localhost $port > tfserver.$port.log 2>&1 &
  nohup java -Dlogger.level=3 -Dopenchai.tfserver.config.file=/git/OCspark/tf/src/main/resources/apps-config.localtest.yml -Djava.net.preferIPv4Stack=true -classpath $GITDIR/tf/target/tf-1.0.0.jar:$GITDIR/tf/libs/* org.openchai.tensorflow.TfServer localhost $port > tfserver.$port.log 2>&1 &
done
