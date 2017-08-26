cd $GITDIR/tcpclient
mvn package install
if [ $? ]; then echo 'mvn package tcplient failed'; exit 127; fi
cd $GITDIR/tfdma
./build.arm.sh  # .build.sh

if [ "$BUILDOK" != "TRUE" ]; then echo 'tfdma build.arm.sh failed'; exit 127; fi
mvn package install
if [ "$?" != "0" ]; then echo 'mvn package install tfdma failed'; exit 1; fi
cd $GITDIR/tf
mvn package
if [ "$?" != "0" ]; then echo 'mvn package tf failed'; exit 1; fi
echo "** DONE **"
