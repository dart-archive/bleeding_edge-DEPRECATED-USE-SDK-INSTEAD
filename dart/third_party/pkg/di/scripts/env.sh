#!/bin/sh
set -e

if [ -n "$DART_SDK" ]; then
    DARTSDK=$DART_SDK
else
    echo "sdk=== $DARTSDK"
    DART=`which dart|cat` # pipe to cat to ignore the exit code
    DARTSDK=`which dart | sed -e 's/\/bin\/dart$/\//'`
    if [ -z "$DARTSDK" ]; then
        DARTSDK="`pwd`/dart-sdk"
    fi
fi

export DART_SDK="$DARTSDK"
export DART=${DART:-"$DARTSDK/bin/dart"}
export PUB=${PUB:-"$DARTSDK/bin/pub"}
export DARTANALYZER=${DARTANALYZER:-"$DARTSDK/bin/dartanalyzer"}
export DARTDOC=${DARTDOC:-"$DARTSDK/bin/dartdoc"}


export DART_FLAGS='--enable_type_checks --enable_asserts'
export PATH=$PATH:$DARTSDK/bin