export OCDIR=/git/OCspark
cd $OCDIR/tcpdma
./build.sh
mvn package install
cd $OCDIR/tcpclient
cd $OCDIR/tf
./build.arm.sh
mvn package install
echo "** DONE **"