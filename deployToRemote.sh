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
	ssh -o ConnectTimeout=2 "guenatb@dryad0${i}.ethz.ch" "mkdir /mnt/local/guenatb; echo ${i} > /mnt/local/guenatb/dryad"
	echo "0${i}" done
done
for i in {10..15}; do
	ssh -o ConnectTimeout=2 "guenatb@dryad${i}.ethz.ch" "mkdir /mnt/local/guenatb; echo ${i} > /mnt/local/guenatb/dryad"
	echo "${i}" done
done
exit 0
# postgres on dryad01
ssh guenatb@dryad15.ethz.ch "screen -dm -S postgres ASL_Project/runPostgres.sh"
	echo Postgres started on "15."
sleep 15 # wait for postgres to install and start

# start middlewares
for i in {10..11}; do
	ssh -o ConnectTimeout=5 "guenatb@dryad${i}.ethz.ch" "screen -dm -S middleware ASL_Project/runMiddleware.sh"
	echo Middleware started on "${i}."
done
sleep 5

# start middlewares
for i in {1..2}; do
	ssh -o ConnectTimeout=5 "guenatb@dryad0${i}.ethz.ch" "screen -dm -S client ASL_Project/runClients.sh"
	echo Client started on "0${i}."
done
