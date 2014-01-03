#!/bin/bash
# Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# bail on error
set -e

source update.sh

update "js" "https://github.com/dart-lang/js-interop"
