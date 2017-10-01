cd $GITDIR/tcpclient
mvn package install
if (( $? != 0 )); then echo 'mvn package tcpclient failed'  &&  exit 127; fi
cd $GITDIR/tfdma
./build.arm.sh  # .build.sh

# if [ "$BUILDOK" != "TRUE" ]; then echo 'tfdma build.arm.sh failed'; exit 127; fi
cd $GITDIR/tf
mvn package install
if (( $? != 0 )); then echo 'mvn package tf failed' &&  exit 127; fi
cd $GITDIR/tfspark
mvn compile package
if (( $? != 0 )); then echo 'mvn package tfspark failed' &&  exit 127; fi
echo "** DONE **"