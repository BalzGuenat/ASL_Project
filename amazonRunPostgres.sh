#!/bin/bash

psql -f ~/ASL_Project/dbInitScript.sql -d asl
echo runPostgres.sh has finished. Now tailing log file.
tail -f /var/log/postgresql/postgresql-9.3-main.log
