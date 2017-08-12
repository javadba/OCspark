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

cd ~/.ssh
scp tx2:~/.ssh/* .

chmod 0700 *
sudo mkdir /git
sudo chown steve:steve /git
cd ~
scp tx2:~/bashrc ~
source ~/.bashrc

echo "export JAVA_HOME=/usr/lib/jvm/java-8-openjdk-arm64" >> ~/.bashrc

sudo mkdir /shared
sudo chmod 777 /shared
ln -s $(pwd) /shared/orajava

scp tx1:/etc/hosts tx13:~
echo "Add hostname to hosts. Then run sudo hostname"
vi hosts
sudo cp hosts /etc/hosts

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
ln -s /git/OCspark/tf/src/main/resources/apps-config.yml /shared/conf/
cd tcpclient
mvn package install
cd ../tfdma
mvn package
./build.arm.sh
cd /git/OCspark
mvn package install

bin/runtf.sh
