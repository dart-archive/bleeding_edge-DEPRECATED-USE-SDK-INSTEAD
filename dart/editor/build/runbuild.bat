rem @echo off
setlocal
set BUILDBOT_BUILDERNAME=dart-editor-win
python ../../client/tools/buildbot_annotated_steps.py
endlocal