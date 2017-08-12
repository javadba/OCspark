export OCDIR=/git/OCspark
cd $OCDIR/tf
port="${1-65234}"
mvn exec:java -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.openchai.tensorflow.TfServer -Dexec.args="* $port foo.cfg" -Dopenchai.tfserver.config.file=/shared/conf/apps-config.yml
