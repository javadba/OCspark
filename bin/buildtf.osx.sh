cd $GITDIR/tcpclient
mvn package install
if [ "$?" != "0" ]; then echo 'mvn package install tcplient failed'; exit 1; fi
cd $GITDIR/tfdma
./build.osx.sh  # .build.sh
if [ "$BUILDOK" != "TRUE" ]; then echo 'tfdma build.osx.sh failed'; exit 1; fi
mvn package install
if [ "$?" != "0" ]; then echo 'mvn package install tfdma failed'; exit 1; fi
cd $GITDIR/tf
mvn package
if [ "$?" != "0" ]; then echo 'mvn package tf failed'; exit 1; fi
echo "** DONE **"
