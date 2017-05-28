export OCDIR=/git/OCspark
cd $OCDIR/tcpclient
mvn package install
cd $OCDIR/tf
mvn package
mvn exec:java -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.openchai.tensorflow.TfServer -Dexec.args="" -Dopenchai.tensorflow.cmdline=/home/ubuntu/tensorflow/bazel-bin/tensorflow/examples/label_image/label_image  -Dopenchai.tensorflow.dir=/home/ubuntu/tensorflow
