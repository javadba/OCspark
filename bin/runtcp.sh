export OCDIR=/git/OCspark
cd $OCDIR/tf
mvn exec:java -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.openchai.tensorflow.TfServer -Dexec.args="" -Dopenchai.tensorflow.cmdline=/home/ubuntu/tensorflow/bazel-bin/tensorflow/examples/label_image/label_image  -Dopenchai.tensorflow.dir=/home/ubuntu/tensorflow
