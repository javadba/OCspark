cd tf
for port in 61234 61244 61254; do
	echo "starting servers in batch $port .."
  nohup mvn exec:java -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.openchai.tensorflow.TfServer -Dexec.args="* $port foo.cfg"  -Dopenchai.tfserver.config.file=/shared/conf/apps-config.yml > tfserver.$port.log 2>&1 &
  nohup java -Dlogger.level=3 -Dopenchai.tfserver.config.file=/shared/conf/apps-config.yml -Dtcpserver.wait.cycles=16 -Djava.net.preferIPv4Stack=true -classpath $GITDIR/tf/target/tf-1.0.0.jar:$GITDIR/tf/libs/* org.openchai.tensorflow.TfServer localhost $port > tfserver.$port.log 2>&1 &
done
