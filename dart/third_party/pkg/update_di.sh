#!/bin/bash
# Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# bail on error
set -e
source update.sh

update "di" "https://github.com/angular/di.dart"

echo "*** Generating static injector test files"
pushd di > /dev/null

pub install
DART_SDK=../../../out/ReleaseIA32/dart-sdk
$DART_SDK/bin/dart \
    --package-root=$DART_SDK/../packages/ \
    bin/generator.dart \
    $DART_SDK/ \
    test/main.dart \
    di.tests.Injectable \
    test/type_factories_gen.dart \
    packages/

# Use tput to set console to red & go back to black.
echo "$(tput setaf 1)di/test/type_factories_gen.dart regenerated, \
requires manual fixup of imports.$(tput sgr0)"

echo "*** Cleaning up packages"
rm -rf packages
