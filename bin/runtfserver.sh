cd tf
mvn exec:java -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.openchai.tensorflow.TfServer -Dexec.args="localhost $1 foo.cfg"  -Dopenchai.tfserver.config.file=/shared/conf/apps-config.yml
