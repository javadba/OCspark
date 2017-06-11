export OCDIR=/git/OCspark
cd $OCDIR/tf
mvn exec:java -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.openchai.tensorflow.TfServer -Dexec.args="" -Dopenchai.tfserver.config.file=/shared/conf/apps-config.yml
