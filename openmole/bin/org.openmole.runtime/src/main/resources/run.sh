#!/bin/sh


#readlink -f does not work on mac, use alternate script
TARGET_FILE=$0

cd `dirname $TARGET_FILE`
TARGET_FILE=`basename $TARGET_FILE`

# Iterate down a (possible) chain of symlinks
while [ -L "$TARGET_FILE" ]
do
    TARGET_FILE=`readlink $TARGET_FILE`
    cd `dirname $TARGET_FILE`
    TARGET_FILE=`basename $TARGET_FILE`
done

REALPATH=$TARGET_FILE
#end of readlink -f

LOCATION=$( cd $(dirname $REALPATH) ; pwd -P )

MEMORY=$1
shift

CONFIGDIR=$1
shift

FLAG=""

JVMVERSION=`java -version 2>&1 | tail -1 -`

case "$JVMVERSION" in
  *64-Bit*) FLAG="-XX:+UseCompressedOops";;
esac


cp -r configuration ${CONFIGDIR}

ulimit -S -v unlimited
ulimit -S -s unlimited

export MALLOC_ARENA_MAX=1

export LC_ALL="en_US.UTF-8"
export LANG="en_US.UTF-8"

java -Dfile.encoding=UTF-8 -Xss1M -Xms64m -Xmx${MEMORY} -Dosgi.locking=none -Dosgi.configuration.area=${CONFIGDIR} $FLAG -XX:ReservedCodeCacheSize=128m -XX:MaxMetaspaceSize=128m -XX:CompressedClassSpaceSize=128m -XX:+UseG1GC -XX:ParallelGCThreads=1 \
  -cp "${LOCATION}/launcher/*" org.openmole.launcher.Launcher --plugins ${LOCATION}/plugins/ --run org.openmole.runtime.SimExplorer --osgi-directory ${CONFIGDIR} -- $@

RETURNCODE=$?

rm -rf ${CONFIGDIR}

exit $RETURNCODE

