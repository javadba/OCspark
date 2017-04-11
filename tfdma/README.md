# Tensorflow DMA Module 

First compile the C modules: On mac:

pushd /git/OCSpark/tfdma/src/main/cpp/ && gcc -dynamiclib -odmaserver.dylib -shared -v -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" -fpic /git/OCSpark/tfdma/src/main/cpp/org_openchai_tensorflow_api_PcieDMAServer.c; popd
