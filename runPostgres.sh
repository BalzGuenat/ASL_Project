#!/bin/bash
echo Installing Postgres
cd ~/postgresql-9.4.5
make install

LD_LIBRARY_PATH=/mnt/local/guenatb/postgres/lib
export LD_LIBRARY_PATH

cd /mnt/local/guenatb/postgres
pkill postgres
rm -rf asl
rm ../guenatb
cp ~/ASL_Project/dbpw .
bin/initdb -D /mnt/local/guenatb/postgres/asl -U postgres --pwfile=dbpw
cp ~/ASL_Project/pg_hba.conf asl
bin/postgres -D /mnt/local/guenatb/postgres/asl -p 39800 -i -k /mnt/local/guenatb > log 2>&1 &
sleep 2
bin/createdb -h /mnt/local/guenatb -p 39800 -U postgres asl
bin/psql -h /mnt/local/guenatb -p 39800 -U postgres -f ~/ASL_Project/dbInitScript.sql -d asl
echo runPostgres.sh has finished. Now tailing log file.
tail -f log
