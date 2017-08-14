cd $OCDIR/tcpclient
mvn package install
cd $OCDIR/tfdma
./build.arm.sh  # .build.sh
mvn package install
cd $OCDIR/tf
mvn package install
echo "** DONE **"