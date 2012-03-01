#!/bin/bash
#
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.
#

function frog_build {
  local infile=$1;
  local outfile="$infile.js";
  local backup=".$file.old";
  [ -f $outfile ] && cp $outfile $backup;
  ../../frog/minfrog --out=$outfile --compile-only $infile
  if [ -f $backup ]; then
    if `diff $outfile $backup -q > /dev/null`; then
      echo "  - $outfile remains unchanged ";
    else
      echo "  - $outfile was updated ";
    fi;
    rm $backup;
  fi
}

# Rebuild pond and the compiler isolate using minfrog
frog_build pond.dart
frog_build compiler.dart

# Build the editors module
cat codemirror/lib/codemirror.js \
    codemirror/mode/dart/dart.js \
    codemirror/mode/htmlmixed/htmlmixed.js \
    codemirror/mode/xml/xml.js \
    codemirror/mode/javascript/javascript.js \
    codemirror/mode/css/css.js \
    codemirror/mode/diff/diff.js \
    editors.js > editors_module.js
