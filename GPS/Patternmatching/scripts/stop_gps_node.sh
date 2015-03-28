#!/bin/bash
temp=`ps ax | grep gps_node_runner | awk '{gsub(" ","\n");print $1}'`

# echo "${temp}"
temp=${temp// /$'\n'}
for process in $temp; do
  kill -9 $process
done
