#!/bin/bash -e
BUILDER=dart-editor-macos
if [[ "$1" == "full" ]]; 
then
  BUILDER=dart-editor
fi
export BUILDBOT_BUILDERNAME=$BUILDER
echo "BUILDBOT_BUILDERNAME = $BUILDBOT_BUILDERNAME"
python ../../client/tools/buildbot_annotated_steps.py
