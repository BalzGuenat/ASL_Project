#!/bin/bash

# mark servers
for i in {1..9}; do
	ssh -o ConnectTimeout=2 "guenatb@dryad0${i}.ethz.ch" "pkill postgres; pkill java"
	echo "0${i}" done
done
for i in {10..15}; do
	ssh -o ConnectTimeout=2 "guenatb@dryad${i}.ethz.ch" "pkill postgres; pkill java"
	echo "${i}" done
done
