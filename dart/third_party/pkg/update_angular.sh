#!/bin/bash
# Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# bail on error
set -e

source update.sh

update "angular" "https://github.com/angular/angular.dart"

echo "*** Generating test files"
./generate_angular_tests.sh
