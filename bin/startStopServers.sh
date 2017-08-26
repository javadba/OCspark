export slaves=$(cat /shared/tx1-slaves.txt | awk -F':' '{print $1}')
publish() {
  for s in $slaves; do echo $s; scp $GITDIR/tf/target/tf-1.0.0.jar $s:/shared ; done
  for s in $slaves; do echo $s; scp $GITDIR/bin/runtfserver.sh $s:/shared ; done
  for s in $slaves; do echo $s; ssh $s chmod +x /shared/runtfserver.sh; done
}

sshall() { slaves=$(cat /shared/tx1-slaves.txt | awk -F':' '{print $1}'); for s in $slaves; do echo $s; ssh $s "$@"; done ; }

stoptf() { sshall "kill -9 \$(ps -ef | grep tf-1.0.0.jar | grep -v grep | awk '{print \$2}')" ; }
showtf() { sshall "ps -ef | grep tf-1.0.0.jar | grep -v grep | awk '{print $2}'" ; }
starttf() {
  stoptf
  showtf
   # for s in slaves; do echo $s; ssh $s /shared/runtfserver.sh localhost; done
  ssh txa1 'nohup /shared/runtfserver.sh localhost 61230 > ~/tf.out 2>&1 &'
  ssh txa2 'nohup /shared/runtfserver.sh localhost 61240 > ~/tf.out 2>&1 &'
  ssh txa3 'nohup /shared/runtfserver.sh localhost 61250 > ~/tf.out 2>&1 &'
  sleep 3
  showtf

 }
