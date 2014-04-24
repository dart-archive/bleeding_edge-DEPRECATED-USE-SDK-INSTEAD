#!/bin/sh
# Runs type factories generator for test files.

if [ -z "$DART_SDK" ]; then
    echo "ERROR: You need to set the DART_SDK environment variable to your dart sdk location"
    exit 1
fi

set -v

dart bin/generator.dart $DART_SDK test/main.dart di.tests.InjectableTest test/type_factories_gen.dart packages/

