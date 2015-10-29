#!/bin/bash

# Packs the whole ASL folder and pushes to all servers.
# creates a file in /mnt/local/guenatb naming the server.
# starts postgres on dryad0 on port .

key=/cygdrive/c/Users/balzg/Documents/ASL.pem
dbinstance=ubuntu@ec2-52-32-83-143.us-west-2.compute.amazonaws.com
mwinstance=ubuntu@ec2-52-32-70-71.us-west-2.compute.amazonaws.com
clientinst=ubuntu@ec2-52-32-69-222.us-west-2.compute.amazonaws.com

# archive the repo
cd ..
tar -zchf ASL_Project.tar.gz ASL_Project

# push to home
scp -i $key ASL_Project.tar.gz "${dbinstance}:~"
ssh -i $key $dbinstance "tar -zxf ASL_Project.tar.gz
	rm ASL_Project.tar.gz"
scp -i $key ASL_Project.tar.gz "${mwinstance}:~"
ssh -i $key $mwinstance "tar -zxf ASL_Project.tar.gz
	rm ASL_Project.tar.gz"
scp -i $key ASL_Project.tar.gz "${clientinst}:~"
ssh -i $key $clientinst "tar -zxf ASL_Project.tar.gz
	rm ASL_Project.tar.gz"
exit 0
