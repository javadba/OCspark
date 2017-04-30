#!/bin/bash

export GITDIR=/git/OCSpark/

# if `mvn package install` already done you can comment out next line
pushd $GITDIR && mvn package install; popd

echo "[1] Building C header files for java native methods (PcieDMA[Client|Server]) .."
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
javah  -v -stubs -d $GITDIR/tfdma/src/main/cpp/includes/ -cp $(cat cp.txt):$GITDIR/tfdma/src/main/java org.openchai.tensorflow.api.PcieDMAClient

echo "[2] Compiling the C files PcieDMA[Client|Server].c .."
pushd $GITDIR/tfdma/src/main/cpp/ && gcc -dynamiclib -odmaclient.dylib -shared -v -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" -fpic $GITDIR/tfdma/src/main/cpp/org_openchai_tensorflow_api_PcieDMAClient.c; popd
pushd $GITDIR/tfdma/src/main/cpp/ && gcc -dynamiclib -odmaserver.dylib -shared -v -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/darwin" -fpic $GITDIR/tfdma/src/main/cpp/org_openchai_tensorflow_api_PcieDMAServer.c; popd
echo "**Entry points for dylibs** "
find . -name \*.dylib | xargs nm -gU | awk '{print $3}'

echo "[3] Compiling java sources and building release jar.."
mvn package 

