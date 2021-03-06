#!/bin/bash

# if `mvn package install` already done you can comment out next line
# pushd $GITDIR && mvn package install; popd

export BUILDOK="FALSE"
echo "[1] Building C header files for java native methods (PcieDMA[Client|Server]) .."
mvn dependency:build-classpath -Dmdep.outputFile=cp.txt
javah  -v -stubs -d $GITDIR/tfdma/src/main/cpp/includes/ -cp $(cat cp.txt):$GITDIR/tfdma/src/main/java org.openchai.tensorflow.api.PcieDMAClient

echo "[2a] Workaround for -I not working on linux: copy the linux/ include files to its parent dir.."
cp -p $JAVA_HOME/include/linux/*.h $JAVA_HOME/include

echo "[2] Compiling the C files PcieDMA[Client|Server].c .."


pushd $GITDIR/tfdma/src/main/cpp/
CURDIR=$(pwd)
rm -f dmaserver.so
gcc -v -shared  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -odmaserver.so -fpic $GITDIR/tfdma/src/main/cpp/org_openchai_tensorflow_api_PcieDMAServer.c
gcc -v -shared  -I"$JAVA_HOME/include" -I"$JAVA_HOME/include/linux" -odmaclient.so -fpic $GITDIR/tfdma/src/main/cpp/org_openchai_tensorflow_api_PcieDMAClient.c
if (( $? != 0 )); then
    echo " ******  FAILURE ERROR 911 RUN FOR HILLS Did not build so ***********************"
    exit -255
fi

popd

echo "**Entry points for dylibs** "
SOUT=$(find . -name \*.so | xargs nm -g | awk '{print $3}')

echo "[3] Compiling java sources and building release jar.."
mvn package install
if (( $? != 0 )); then echo 'mvn package tfdma failed' && popd &&  exit 127; fi

export BUILDOK="TRUE"
