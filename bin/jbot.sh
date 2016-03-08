#!/usr/bin/env sh

java -jar $1
while [ "$?" = "27" ]
do
	echo Restarting JBot service ...
	java -jar $1
done
exit $?
