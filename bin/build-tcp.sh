export OCDIR=/git/OCspark
cd $OCDIR/tcpclient
mvn package install
cd $OCDIR/tf
mvn package
mvn exec:java -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.openchai.tensorflow.TfServer -Dexec.args=""  -Dopenchai.tfserver.config.file=/shared/conf/apps-config.yml
