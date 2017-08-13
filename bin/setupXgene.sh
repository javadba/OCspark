
alias aptget='sudo apt-get install -y'
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer
sudo apt-get install oracle-java8-set-default

#sudo apt-get remove scala-library scala
sudo wget www.scala-lang.org/files/archive/scala-2.11.8.deb
sudo dpkg -i scala-2.11.8.deb
aptget screen

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

git config --global user.email javadba@gmail.com
git config --global user.name javadba
git config --global push.default current
git config --global commit.default current

aptget software-properties-common
sudo apt-add-repository universe
sudo apt-get update
aptget maven

sudo mkdir /git
sudo chown steve:steve /git
cd /git
git clone https://github.com/OpenChaiSpark/OCspark
cd OCspark

bin/build-tcp.sh
mvn clean package

sudo mkdir /shared
sudo chown steve:steve /shared
chmod 777 .
wget https://d3kbcqa49mib13.cloudfront.net/spark-2.1.1-bin-hadoop2.7.tgz
ln -s /shared/spark-2.1.1-bin-hadoop2.7 /shared/spark
cd spark

