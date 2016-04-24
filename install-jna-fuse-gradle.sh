#!/bin/bash
sudo yum -y install java-1.8.0-openjdk-devel.x86_64
wget https://downloads.gradle.org/distributions/gradle-2.12-bin.zip
unzip gradle*
cd gradle*/bin/
gradlePath = $(pwd)
cd /usr/bin
sudo ln -s $gradlePath gradle
cd ~/fuse-jna/
chmod 777 ramdisk
