#!/bin/bash
#
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.
#

# Rebuild pond using minfrog
../../frog/minfrog --out=pond.dart.js --compile-only pond.dart
