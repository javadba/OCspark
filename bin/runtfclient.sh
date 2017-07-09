pushd $GITDIR/tf && mvn exec:java -Djava.net.preferIPv4Stack=true -Dexec.mainClass=org.openchai.tensorflow.TFSubmitter -Dexec.args="local 192.168.1.125 tensorflow-labelimage abc 2"; popd
