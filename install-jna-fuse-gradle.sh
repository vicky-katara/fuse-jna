#!/bin/bash
sudo yum -y install java-1.8.0-openjdk-devel.x86_64
wget https://downloads.gradle.org/distributions/gradle-2.12-bin.zip
unzip gradle*
cd gradle*/bin/
export PATH=$PATH:$(pwd)
cd ../..
chmod 777 ramdisk
chmod 777 extra-credit-implemented