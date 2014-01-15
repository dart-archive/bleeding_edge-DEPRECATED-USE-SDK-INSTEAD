#!/usr/bin/env python
#
# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# Dart Editor promote tools.

import imp
import optparse
import os
import subprocess
import sys
import urllib

from os.path import join

DART_PATH = os.path.abspath(os.path.join(__file__, '..', '..', '..'))
BOT_UTILS = os.path.abspath(os.path.join(
    DART_PATH,  'tools', 'bots', 'bot_utils.py'))

bot_utils = imp.load_source('bot_utils', BOT_UTILS)

def BuildOptions():
  usage = """usage: %prog promote [options]
  where:
    promote - Will promote builds from raw/signed locations to release
              locations.

    Example: Promote revision r29962 on dev channel:
        python editor/build/promote.py promote --channel=dev --revision=29962
  """

  result = optparse.OptionParser(usage=usage)

  group = optparse.OptionGroup(
      result, 'Promote', 'options used to promote code')
  group.add_option(
      '--revision', help='The svn revision to promote', action='store')
  group.add_option(
      '--channel', type='string', help='Channel to promote.', default=None)
  result.add_option_group(group)

  return result


def main():
  parser = BuildOptions()
  (options, args) = parser.parse_args()

  def die(msg):
    print msg
    parser.print_help()
    sys.exit(1)

  if not args:
    die('At least one command must be specified')

  if args[0] == 'promote':
    command = 'promote'
    if options.revision is None:
      die('You must specify a --revision to specify which revision to promote')

    # Make sure revision is a valid integer
    try:
      _ = int(options.revision)
    except:
      die('You must supply a valid integer argument to --revision to promote')

    # Make sure options.channel is a valid
    if not options.channel:
      die('Specify --channel=be/dev/stable')
    if options.channel not in bot_utils.Channel.ALL_CHANNELS:
      die('You must supply a valid channel to --channel to promote')
  else:
    die('Invalid command specified: {0}.  See help below'.format(args[0]))

  if command == 'promote':
    _PromoteDartArchiveBuild(options.channel, options.revision)


def UpdateDocs():
  try:
    print 'Updating docs'
    url = "http://api.dartlang.org/docs/releases/latest/?force_reload=true"
    f = urllib.urlopen(url)
    f.read()
    print 'Successfully updated api docs'
  except Exception as e:
    print 'Could not update api docs, please manually update them'
    print 'Failed with: %s' % e


def _PromoteDartArchiveBuild(channel, revision):
  # These namer objects will be used to create GCS object URIs. For the
  # structure we use, please see tools/bots/bot_utils.py:GCSNamer
  raw_namer = bot_utils.GCSNamer(channel, bot_utils.ReleaseType.RAW)
  signed_namer = bot_utils.GCSNamer(channel, bot_utils.ReleaseType.SIGNED)
  release_namer = bot_utils.GCSNamer(channel, bot_utils.ReleaseType.RELEASE)

  def promote(to_revision):
    def safety_check_on_gs_path(gs_path, revision, channel):
      if not ((revision == 'latest' or int(revision) > 0)
              and len(channel) > 0
              and ('%s' % revision) in gs_path
              and channel in gs_path):
        raise Exception(
            "InternalError: Sanity check failed on GS URI: %s" % gs_path)

    def remove_gs_directory(gs_path):
      safety_check_on_gs_path(gs_path, to_revision, channel)
      Gsutil(['-m', 'rm', '-R', '-f', gs_path])

    # Copy sdk directory.
    from_loc = raw_namer.sdk_directory(revision)
    to_loc = release_namer.sdk_directory(to_revision)
    remove_gs_directory(to_loc)
    Gsutil(['-m', 'cp', '-a', 'public-read', '-R', from_loc, to_loc])

    # Copy eclipse update directory.
    from_loc = raw_namer.editor_eclipse_update_directory(revision)
    to_loc = release_namer.editor_eclipse_update_directory(to_revision)
    remove_gs_directory(to_loc)
    Gsutil(['-m', 'cp', '-a', 'public-read', '-R', from_loc, to_loc])

    # Copy api-docs zipfile.
    from_loc = raw_namer.apidocs_zipfilepath(revision)
    to_loc = release_namer.apidocs_zipfilepath(to_revision)
    Gsutil(['-m', 'cp', '-a', 'public-read', from_loc, to_loc])

    # Copy dartium directory.
    from_loc = raw_namer.dartium_directory(revision)
    to_loc = release_namer.dartium_directory(to_revision)
    remove_gs_directory(to_loc)
    Gsutil(['-m', 'cp', '-a', 'public-read', '-R', from_loc, to_loc])

    # Copy editor zip files.
    target_editor_dir = release_namer.editor_directory(to_revision)
    remove_gs_directory(target_editor_dir)
    for system in ['windows', 'macos', 'linux']:
      for arch in ['ia32', 'x64']:
        from_namer = raw_namer
        # We have signed versions of the editor for windows and macos.
        if system == 'windows' or system == 'macos':
          from_namer = signed_namer
        from_loc = from_namer.editor_zipfilepath(revision, system, arch)
        to_loc = release_namer.editor_zipfilepath(to_revision, system, arch)
        Gsutil(['cp', '-a', 'public-read', from_loc, to_loc])
        Gsutil(['cp', '-a', 'public-read', from_loc + '.md5sum',
                 to_loc + '.md5sum'])

    # Copy signed editor installers for macos/windows.
    for system, extension in [('windows', 'msi'), ('macos', 'dmg')]:
      for arch in ['ia32', 'x64']:
        from_loc = signed_namer.editor_installer_filepath(
            revision, system, arch, extension)
        to_loc = release_namer.editor_installer_filepath(
            to_revision, system, arch, extension)
        Gsutil(['cp', '-a', 'public-read', from_loc, to_loc])

    # Copy VERSION file.
    from_loc = raw_namer.version_filepath(revision)
    to_loc = release_namer.version_filepath(to_revision)
    Gsutil(['cp', '-a', 'public-read', from_loc, to_loc])


  promote(revision)
  promote('latest')

def Gsutil(cmd):
  gsutilTool = join(DART_PATH, 'third_party', 'gsutil', 'gsutil')
  bot_utils.run([sys.executable, gsutilTool] + cmd)


if __name__ == '__main__':
  sys.exit(main())
