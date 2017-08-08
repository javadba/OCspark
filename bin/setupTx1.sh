alias aptget='sudo apt-get install -y'
sudo add-apt-repository ppa:webupd8team/java
sudo apt-get update
sudo apt-get install oracle-java8-installer
#aptget -f
sudo apt-get install oracle-java8-set-default

#sudo apt-get remove scala-library scala
sudo wget www.scala-lang.org/files/archive/scala-2.11.8.deb
sudo dpkg -i scala-2.11.8.deb

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



