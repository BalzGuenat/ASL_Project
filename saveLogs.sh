#!/bin/bash
key=/cygdrive/c/Users/balzg/Documents/ASL.pem
dbinstance=ubuntu@ec2-52-32-83-143.us-west-2.compute.amazonaws.com
mwinstance=ubuntu@ec2-52-32-70-71.us-west-2.compute.amazonaws.com
clientinst=ubuntu@ec2-52-32-69-222.us-west-2.compute.amazonaws.com

timestamp=$(date +%F_%H-%M-%S)
ssh -i $key $mwinstance "tar -zcf ~/${timestamp}_MW.log.tar.gz -C ~/ASL_Project log4j.log"
scp -i $key "${mwinstance}:~/${timestamp}_MW.log.tar.gz" ~/ASL_Logs/

ssh -i $key $clientinst "tar -zcf ~/${timestamp}_CL.log.tar.gz -C ~/ASL_Project log4j.log"
scp -i $key "${clientinst}:~/${timestamp}_CL.log.tar.gz" ~/ASL_Logs/
exit 0
