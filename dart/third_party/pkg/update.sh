#!/bin/bash
# Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# bail on error
set -e

update() {
  local package=$1
  local clone_url=$2
  local branch=$3

  echo "*** Deleting old version"
  rm -rf ${package}

  echo "*** Syncing code"
  git clone ${clone_url} ${package}

  pushd $1 > /dev/null
  if [[ $# -eq 3 ]]; then
    git checkout ${branch}
  fi

  echo `git rev-parse HEAD` >> REVISION

  rm -rf .git

  popd > /dev/null
}
