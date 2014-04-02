#!/usr/bin/python
# Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

"""
Dartium on Android buildbot steps.

Runs steps after the buildbot builds Dartium on Android,
which should upload the APK to an attached device, and run
Dart and chromium tests on it.
"""

import optparse
import os
import string
import sys

import bot
import bot_utils

SCRIPT_DIR = os.path.dirname(os.path.abspath(__file__))
sys.path.append(os.path.join(SCRIPT_DIR, '..'))
import utils


APK_LOCATION = 'apks/Chrome.apk'
CS_LOCATION = 'apks/ContentShell.apk'

def GetOptionsParser():
  parser = optparse.OptionParser("usage: %prog [options]")
  parser.add_option("--build-products-dir",
                    help="The directory containing the products of the build.")
  return parser


def UploadSetACL(gsutil, local, remote):
  gsutil.upload(local, remote)
  gsutil.setGroupReadACL(remote, 'google.com')
  gsutil.setContentType(remote, 'application/vnd.android.package-archive')
  

def UploadAPKs(options):
  with bot.BuildStep('Upload apk'):
    revision = utils.GetSVNRevision()
    namer = bot_utils.GCSNamer(internal=True)
    # The version of gsutil we have on the bots is not new enough to support
    # the acl set commands.
    bot_utils.GSUtil.USE_DART_REPO_VERSION = True
    gsutil = bot_utils.GSUtil()

    # Archive dartuim
    local = os.path.join(options.build_products_dir, APK_LOCATION)
    # TODO(whesse): pass in arch and mode from reciepe
    remote = namer.dartium_android_apk_filepath(revision,
                                                'dartium-android',
                                                'arm',
                                                'release')
    web_link_prefix = 'https://storage.cloud.google.com/'
    dartium_link = string.replace(remote, 'gs://', web_link_prefix)
    UploadSetACL(gsutil, local, remote)
    print "Uploaded dartium, available from: %s" % dartium_link

    # Archive content shell
    local = os.path.join(options.build_products_dir, CS_LOCATION)
    # TODO(whesse): pass in arch and mode from reciepe
    remote = namer.dartium_android_apk_filepath(revision,
                                                'content_shell-android',
                                                'arm',
                                                'release')
    content_shell_link = string.replace(remote, 'gs://', web_link_prefix)
    UploadSetACL(gsutil, local, remote)
    print "Uploaded content shell, available from: %s" % content_shell_link

def main():
  if sys.platform != 'linux2':
    print "This script was only tested on linux. Please run it on linux!"
    sys.exit(1)

  parser = GetOptionsParser()
  (options, args) = parser.parse_args()

  if not options.build_products_dir:
    print "No build products directory given."
    sys.exit(1)

  UploadAPKs(options)
  sys.exit(0)

if __name__ == '__main__':
  main()
