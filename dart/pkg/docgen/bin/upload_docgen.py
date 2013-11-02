#!/usr/bin/python
#
# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

""" This file will run docgen.dart on the SDK libraries, and upload them to
  Google Cloud Storage for the documentation viewer.
"""

import optparse
import os
from os.path import join, dirname, abspath, exists
import platform
import subprocess
import sys
sys.path.append(abspath(join(dirname(__file__), '../../../tools')))
import utils
from upload_sdk import ExecuteCommand


DART = abspath(join(dirname(__file__), '../../../%s/%s/dart-sdk/bin/dart'
    % (utils.BUILD_ROOT[utils.GuessOS()], utils.GetBuildConf('release',
    utils.GuessArchitecture()))))
PACKAGE_ROOT = abspath(join(dirname(__file__), '../../../%s/%s/packages/'
    % (utils.BUILD_ROOT[utils.GuessOS()], utils.GetBuildConf('release',
    utils.GuessArchitecture()))))
GSUTIL = utils.GetBuildbotGSUtilPath()
GS_SITE = 'gs://dartlang-docgen'
DESCRIPTION='Runs docgen.dart on the SDK libraries, and uploads them to Google \
    Cloud Storage for the dartdoc-viewer. '
# Allow us to override checking SVN's revision number. Useful for development
# so we can upload docs from a branch using an SDK that was built on a
# revision newer than when the branch was forked.
trustSVN = None

def GetOptions():
  parser = optparse.OptionParser(description=DESCRIPTION)
  parser.add_option('--package-root', dest='pkg_root',
    help='The package root for dart. (Default is in the build directory.)',
    action='store', default=PACKAGE_ROOT)
  parser.add_option('--dontTrustSVN', dest='dontTrustSVN', default=False,
    help='Don''t rely on the SVN revision number, check the DART SDK instead',
    action='store_true')
  (options, args) = parser.parse_args()
  SetPackageRoot(options.pkg_root)
  trustSVN = not options.dontTrustSVN

def SetPackageRoot(path):
  global PACKAGE_ROOT
  if exists(path):
    PACKAGE_ROOT = abspath(path)


def SetGsutil():
  """ If not on buildbots, find gsutil relative to docgen. """
  global GSUTIL
  if not exists(GSUTIL):
    GSUTIL = abspath(join(dirname(__file__),
        '../../../third_party/gsutil/gsutil'))


def Upload(source, target):
  """ Upload files to Google Storage. """
  cmd = [GSUTIL, '-m', 'cp', '-q', '-a', 'public-read', '-r', source, target]
  (status, output) = ExecuteCommand(cmd)
  return status


def main():
  GetOptions()

  SetGsutil()

  # Execute Docgen.dart on the SDK.
  ExecuteCommand(['python', 'dartdoc.py', '-d'])

  # Use SVN Revision to get the revision number.
  revision = None
  if trustSVN:
    revision = utils.GetSVNRevision()

  if revision is None:
    # Try to find the version from the dart-sdk folder.
    revision_file_location = abspath(join(dirname(__file__),
        '../../../%s/%s/dart-sdk/revision' % (utils.BUILD_ROOT[utils.GuessOS()],
        utils.GetBuildConf('release', utils.GuessArchitecture()))))
    with open(revision_file_location, 'r') as revision_file:
      revision = revision_file.readline(5)
      revision_file.close()

    if revision is None:
      raise Exception("Unable to find revision. ")

  # Upload the all files in Docs into a folder based off Revision number on
  # Cloud Storage.
  # TODO(alanknight): If we give it ./docs/* it fails to upload files in the
  # subdirectory, probably due to escaping. Work around it by changing dir.
  # Better, generalize this to be less dependent on the working directory.
  os.chdir('docs')
  Upload('.', GS_SITE + '/' + revision + '/')
  os.chdir('..')

  # Update VERSION file in Cloud Storage.
  with open('VERSION', 'w') as version_file:
    version_file.write(revision)
    version_file.close()
  Upload('./VERSION', GS_SITE + '/VERSION')
  Upload('./VERSION', GS_SITE + '/' + revision + '/' + 'VERSION')

  # Clean up the files it creates.
  ExecuteCommand(['rm', '-rf', './docs'])
  ExecuteCommand(['rm', '-f', './VERSION'])


if __name__ == '__main__':
  main()
