cd tf
for port in 61234 61244 61254; do
	echo "starting servers in batch $port .."
  nohup mvn exec:java -Dlogger.level=0 -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.openchai.tensorflow.TfServer -Dexec.args="* $port foo.cfg"  -Dopenchai.tfserver.config.file=/shared/conf/apps-config.yml > tfserver.$port.log 2>&1 &
done
