

Here is the app config script: please update the `pointrflow-a[1..3]` sections


### Env setup
You should copy the `.bashrc` from the tx1/xgene to the new env as well
the setup scripts attempt to do that .. but it probably needs hand holding
and then as shown in the demo on *each* machine you just invoke the  `runtf`   shell function: each machine is customized to then just "do the right thing".  On the `tx1` you need to update
`.bashrc` to set the `OPORT` here:

export OPORT=61234
runtf() { cd $GITDIR; bin/runtf.sh $OPORT ; }

### Apps configuration

(apps-config.yml)[https://github.com/OpenChaiSpark/OCspark/blob/master/tf/src/main/resources/apps-config.yml]

### Xgene master setup
on the `xgene` you need to create a  `/shared/tx1-slaves.txt`.  you can copy the one on the `xgenea` to the new env

#### Images

You can copy from txa1 in /data/scenery and /data/malls on the Xgene


####



