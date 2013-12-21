#!/bin/bash
# Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# bail on error
set -e

DIR=$( cd $( dirname "${BASH_SOURCE[0]}" ) && pwd )
echo $DIR
pushd $DIR > /dev/null

echo "*** Deleting old version"
rm -rf di

echo "*** Syncing code"
git clone https://github.com/angular/di.dart di

pushd di
REVISION=`git rev-parse HEAD`
rm -rf .git

echo "*** Generating static injector test files"
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

echo "*** Cleaning up packages"
rm -rf packages

echo "*** Updated to revision $REVISION"
