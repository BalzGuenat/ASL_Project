#!/bin/bash

# Packs the whole ASL folder and pushes to all servers.
# creates a file in /mnt/local/guenatb naming the server.
# starts postgres on dryad0 on port .

# archive the repo
cd ..
tar -chf ASL_Project.tar ASL_Project

# push to home
scp ASL_Project.tar guenatb@dryad01.ethz.ch:~
ssh guenatb@dryad01.ethz.ch "tar -xf ASL_Project.tar
	rm ASL_Project.tar
	ln -s /mnt/local/guenatb local"

# mark servers
for i in {1..9}; do
	ssh -o ConnectTimeout=2 "guenatb@dryad0${i}.ethz.ch" "mkdir /mnt/local/guenatb; touch /mnt/local/guenatb/dryad0${i}"
	echo "0${i}" "done"
done
for i in {10..15}; do
	ssh -o ConnectTimeout=2 "guenatb@dryad${i}.ethz.ch" "mkdir /mnt/local/guenatb; touch /mnt/local/guenatb/dryad${i}"
	echo "${i}" "done"
done

# postgres on dryad01
ssh guenatb@dryad01.ethz.ch "screen -dm -S postgres ASL_Project/runPostgres.sh"
#sleep 15
