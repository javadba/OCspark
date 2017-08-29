cd $GITDIR/tcpclient
mvn package install
if (( $? != 0 )); then echo 'mvn package tcpclient failed' && popd &&  exit 127; fi

cd $GITDIR/tfdma
./build.osx.sh
# if [ "$BUILDOK" != "TRUE" ]; then echo 'tfdma build.osx.sh failed'; exit 1; fi
cd $GITDIR/tf
mvn package install
if (( $? != 0 )); then echo 'mvn package tf failed' && popd &&  exit 127; fi
cd $GITDIR/tfspark
mvn compile # package
if (( $? != 0 )); then echo 'mvn package tfspark failed' && popd &&  exit 127; fi
echo "** DONE **"
