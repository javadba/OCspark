slaves=$(cat /shared/tx1-slaves.txt | awk -F':' '{print $2}')
for s in slaves; do echo $s; scp $GITDIR/tf/target/tf-1.0.0.jar $s; done
for s in slaves; do echo $s; scp $GITDIR/bin/runtfserver.sh $s; done
for s in slaves; do echo $s; ssh $s chmod +x runtfserver.sh; done

# for s in slaves; do echo $s; ssh $s ./runtfserver.sh localhost; done
ssh txa1 ./runtfserver.sh localhost 61230
ssh txa2 ./runtfserver.sh localhost 61240
ssh txa3 ./runtfserver.sh localhost 61250



