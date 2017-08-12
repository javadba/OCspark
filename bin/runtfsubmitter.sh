cd /git/OCspark/tf 
mvn exec:java -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.openchai.tensorflow.TfSubmitter -Dexec.args="--rest localhost:8190 local[*] 192.168.0.2 tensorflow-labelimage /data/scenery scenery"
