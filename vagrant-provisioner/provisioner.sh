#!/bin/bash
jre_tar="jre-7u71-linux-x64.tar.gz"
jdk_tar="jdk-7u71-linux-x64.tar.gz"

# Install jre
cd /opt
if [[ ! -e ${jre_tar} ]]; then
wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
  "http://download.oracle.com/otn-pub/java/jdk/7u71-b14/${jre_tar}"
tar xvf jre-7u71-linux-x64.tar.gz
chown -R root:root jre1.7.0_71
fi

# Install jdk
cd /opt
if [[ ! -e ${jdk_tar} ]]; then
sudo wget --no-cookies --no-check-certificate --header "Cookie: gpw_e24=http%3A%2F%2Fwww.oracle.com%2F; oraclelicense=accept-securebackup-cookie" \
  "http://download.oracle.com/otn-pub/java/jdk/7u71-b14/${jdk_tar}"
tar xvf ${jdk_tar}
chown -R root:root jdk1.7.0_71
fi
alternatives --install /usr/bin/java java /opt/jdk1.7.0_71/bin/java 1
alternatives --install /usr/bin/javac javac /opt/jdk1.7.0_71/bin/javac 1
alternatives --install /usr/bin/jar jar /opt/jdk1.7.0_71/bin/jar 1
