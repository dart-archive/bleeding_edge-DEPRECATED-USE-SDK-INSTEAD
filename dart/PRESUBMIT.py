# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

"""Top-level presubmit script for Dart.

See http://dev.chromium.org/developers/how-tos/depottools/presubmit-scripts
for more details about the presubmit API built into gcl.
"""

MOVED_TO_GITHUB = ("The dart svn repo is no more, and is only here for a "
    "grace period until dependent projects have moved off. The new repo "
    "is located at https://github.com/dart-lang/sdk, alongside the rest "
    "of the dart-lang repos. Commits to this repo will have no effect "
    "and you should not do it.")

def CheckChangeOnCommit(input_api, output_api):
  return [output_api.PresubmitError(MOVED_TO_GITHUB)]

def CheckChangeOnUpload(input_api, output_api):
  return [output_api.PresubmitError(MOVED_TO_GITHUB)]

