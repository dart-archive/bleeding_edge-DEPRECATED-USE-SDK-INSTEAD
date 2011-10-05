# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# Copies files into directory structure needed for appengine.

OUT_DIR="../../out/"
DEPLOY_DIR="${OUT_DIR}isolate_deploy_dir/"
RESOURCES_DIR="${DEPLOY_DIR}resources/"

echo "deploying to ${DEPLOY_DIR}"

set -x
mkdir -p $RESOURCES_DIR
mkdir -p ${RESOURCES_DIR}samples/isolate
cp ${OUT_DIR}/Debug_dartc/obj.target/geni/samples/isolate/isolate_sample.app/samples/isolate/isolate_sample.app.js ${RESOURCES_DIR}/samples/isolate/isolate_sample.js

cp -r ../../base ${RESOURCES_DIR}
cp -r ../../dom ${RESOURCES_DIR}
cp -r ../../observable ${RESOURCES_DIR}
cp -r ../../util ${RESOURCES_DIR}
cp -r ../../samples/isolate ${RESOURCES_DIR}samples/
cp app.yaml ${DEPLOY_DIR}
cp main.py ${DEPLOY_DIR}
