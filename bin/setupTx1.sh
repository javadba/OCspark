alias aptget='sudo apt-get install -y'
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer
sudo apt-get install oracle-java8-set-default

#sudo apt-get remove scala-library scala
sudo wget www.scala-lang.org/files/archive/scala-2.11.8.deb
sudo dpkg -i scala-2.11.8.deb

export tx1=192.168.1.125
scp $tx1:/etc/hosts ~
echo "Add hostname to hosts. Then run sudo hostname"
vi hosts
sudo cp hosts /etc/hosts

cd ~/.ssh
scp txa2:~/.ssh/* .
chmod 0700 *

cd ~
scp txa2:~/bashrc ~
source ~/.bashrc
echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-arm64" >> ~/.bashrc

sudo mkdir /git
sudo chown steve:steve /git

sudo mkdir /shared
sudo chmod 777 /shared
ln -s $(pwd) /shared/orajava

scp -rp txa1:~ubuntu/tensorflow /shared/
sudo ln -s /shared/tensorflow ~ubuntu/tensorflow

git config --global user.email javadba@gmail.com
git config --global user.name javadba
git config --global push.default current
git config --global commit.default current

aptget software-properties-common
sudo apt-add-repository universe
sudo apt-get update
aptget maven


cd /git
gitc https://OpenChaiSpark:simit-domates-peynir@github.com/OpenChaiSpark/OCspark
mkdir /shared/conf
ln -s /git/OCspark/tf/src/main/resources/apps-config.yml /shared/conf/
cd /git/OCspark
bin/buildtf.sh

bin/runtf.sh 65254



