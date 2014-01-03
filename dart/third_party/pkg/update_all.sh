#!/bin/bash
# Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# bail on error
set -e

pushd "$( cd $( dirname "${BASH_SOURCE[0]}" ) && pwd )" > /dev/null

for script in $(find . -name "update_*" -not -path "./update_all.sh"); do
  $script
done
