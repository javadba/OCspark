export slaves=$(cat /shared/gpu-slaves.txt | awk '{print $2}' | awk -F':' '{print $1}')
publish() {
  for s in $slaves; do echo $s; scp $GITDIR/tf/target/tf-1.0.0.jar $s:/shared ; done
  for s in $slaves; do echo $s; scp $GITDIR/bin/runtfserver.sh $s:/shared ; done
  for s in $slaves; do echo $s; ssh $s chmod +x /shared/runtfserver.sh; done
}

sshall() { for s in $slaves; do echo $s; ssh $s "$@"; done ; }

scpall() { for s in $slaves; do scp $1 $s:$2; done ; }

stoptf() { sshall "kill -9 \$(ps -ef | grep tf-1.0.0.jar | grep -v grep | awk '{print \$2}')" ; }
showtf() { sshall "ps -ef | grep tf-1.0.0.jar | grep -v grep | awk '{print $2}'" ; }
starttf() { ssh $1 "nohup /shared/runtfserver.sh localhost $2 > ~/tf.$2.out 2>&1 &"; }
starttfs() {
  stoptf
  showtf
  starttf txa1 61230
  ssh txa2 'nohup /shared/runtfserver.sh localhost 61240 > ~/tf.out 2>&1 &'
  ssh txa3 'nohup /shared/runtfserver.sh localhost 61250 > ~/tf.out 2>&1 &'
  sleep 3
  showtf

 }
killtf() { ssh $1 "kill -9 \$(ps -ef | grep -v grep | grep $2 | awk '{print \$2}')" ; }

mkTmpDirs() { sshall "echo \$(cat ~/pwd.txt | head -n 1) | sudo -S mkdir -p /data/tmp/tf2 " ; }
