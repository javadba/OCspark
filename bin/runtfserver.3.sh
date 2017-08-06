cd tf
for port in 61230 61240 61250; do
	echo "starting servers in batch $port .."
  nohup mvn exec:java -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.openchai.tensorflow.TfServer -Dexec.args="localhost $port foo.cfg"  -Dopenchai.tfserver.config.file=/shared/conf/apps-config.yml > tfserver.$port.log 2>&1 &
done
