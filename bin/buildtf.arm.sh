#cd $GITDIR/tcpclient
#mvn package install
#if [ $? ]; then echo 'mvn package tcplient failed'; exit 127; fi
cd $GITDIR/tfdma
./build.arm.sh  # .build.sh

if [ "$BUILDOK" != "TRUE" ]; then echo 'tfdma build.arm.sh failed'; exit 127; fi
mvn compile # package  # hhhhhhhhhhinstall
if (( $? != 0 )); then echo 'mvn package tfdma failed' && popd &&  exit 127; fi
cd $GITDIR/tf
mvn compile # package
if (( $? != 0 )); then echo 'mvn package tf failed' && popd &&  exit 127; fi
echo "** DONE **"

