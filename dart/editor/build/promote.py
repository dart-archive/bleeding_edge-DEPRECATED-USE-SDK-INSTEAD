#!/usr/bin/env python
# 
# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# Dart Editor promote and google storage cleanup tools.

import gsutil
import optparse
import os
import subprocess
import sys

from os.path import join

CONTINUOUS = 'gs://dart-editor-archive-continuous'
TRUNK = 'gs://dart-editor-archive-trunk'
TESTING = 'gs://dart-editor-archive-testing'
INTEGRATION = 'gs://dart-editor-archive-integration'
RELEASE = 'gs://dart-editor-archive-release'

DART_PATH = os.path.join('..', '..', '..', 'dart')

def _BuildOptions():
  """Setup the argument processing for this program.

  Returns:
    the OptionParser to process the CLI
  """
  usage = """usage: %prog [options] cleanup|promote
  where:
    cleanup will cleanup the Google storage continuous bucket
    promote will promote code between different stages

    If you do not specify the location of GSUtil with --gsutilloc the
    program will look in:
    /b/build/scripts/slave/gsutil (BuildBot location)
    ~/gsutil/gsutil (local disk)
    search your path for gsutil

    Examples:
      cleanup saving the last 150 revisions
        python gsTool.py cleanup --keepcount=150

      promote revision 567 from continuous to integration
        python gsTool.py promote --continuous --revision=567

      promote revision 567 from integration to release
        python gsTool.py promote --integration --revision=567"""


  result = optparse.OptionParser(usage=usage)
  result.set_default('gsbucketuri', 'gs://dart-editor-archive-continuous')
  result.set_default('keepcount', 1000)
  result.set_default('dryrun', False)
  result.set_default('continuous', False)
  result.set_default('trunk', False)
  result.set_default('integration', False)
  result.set_default('testing', False)
  group = optparse.OptionGroup(result, 'Cleanup',
                               'options used to cleanup Google Storage')
  group.add_option('--keepcount',
                   type='int',
                   help='Numer of Builds to keep.',
                   action='store')
  result.add_option_group(group)

  group = optparse.OptionGroup(result, 'Promote',
                               'options used to promote code')
  group.add_option('--revision',
                   help='The svn revision to promote',
                   action='store')
  group.add_option('--continuous',
                   help='Promote from continuous',
                   action='store_true')
  group.add_option('--trunk',
                   help='Promote from trunk',
                   action='store_true')
  group.add_option('--integration',
                   help='Promote from integration',
                   action='store_true')
  result.add_option_group(group)

  result.add_option('--gsbucketuri',
                    help='Dart Continuous Google Storage bucket URI.',
                    action='store')
  result.add_option('--gsutilloc',
                    help='location of gsutil the program',
                    action='store')
  result.add_option('--dryrun', help='don\'t do anything that would change'
                    ' Google Storage',
                    action='store_true')
  result.add_option('--testing', help='user test bucket in '
                    ' Google Storage',
                    action='store_true')

  return result


def main():
  """Main entry point for Google Storage Tools."""

  _PrintSeparator('main')

  parser = _BuildOptions()
  (options, args) = parser.parse_args()

  if not args:
    print 'At least one command must be specified'
    parser.print_help()
    sys.exit(1)

  if args[0] == 'promote':
    command = 'promote'
    if options.revision is None:
      print 'You must specify a --revision to specify which revision to promote'
      parser.print_help()
      sys.exit(3)
    if not (options.continuous or options.integration or 
            options.testing or options.trunk):
      print 'You must specify --continuous, --integration, --testing, or --trunk'
      parser.print_help()
      sys.exit(4)
    if options.continuous and options.integration:
      print 'continuous and integration can not be specified at the same time'
      parser.print_help()
      sys.exit(5)
    if (options.continuous or options.integration) and options.testing:
      print """Warning --continuous or --integration  and --testing are
       specified.  The --testing flag will take precedence and all data will
       go to the testing bucket {0}""".format(TESTING)
  elif args[0] == 'cleanup':
    command = 'cleanup'
    if options.keepcount is None:
      print 'You must specify --keepcount'
      parser.print_help()
      sys.exit(6)
  else:
    print 'Invalid command specified: {0}.  See help below'.format(args[0])
    parser.print_help()
    sys.exit(2)

  gsu = gsutil.GsUtil(options.dryrun, options.gsutilloc)

  if options.testing:
    bucket_from = CONTINUOUS
    bucket_to = TESTING
    print """The --testing attribute is specified.  All data will go to the
    testing bucket {0}.  Press enter to continue""".format(TESTING)
    raw_input('Press Enter to continue')
  elif options.continuous:
    bucket_from = CONTINUOUS
    bucket_to = INTEGRATION
  elif options.trunk:
    bucket_from = TRUNK
    bucket_to = INTEGRATION
  elif options.integration:
    bucket_from = INTEGRATION
    bucket_to = RELEASE

  if command == 'cleanup':
    #if the testing flag is set remove the date from the testing bucket
    if options.testing:
      bucket = TESTING
    #otherwise use the value passed as --gsbucketuri
    else:
      bucket = options.gsbucketuri
    version_dirs = _ReadBucket(gsu, '{0}/{1}'.format(bucket, '[0-9]*'))
    _RemoveElements(gsu, bucket, version_dirs, options.keepcount)
  elif command == 'promote':
    _PromoteBuild(options.revision, bucket_from, bucket_to)


def _ReadBucket(gsu, bucket):
  """Read the contents of a Google Storage Bucket.

  Args:
    gsu: the location of the gsutil program
    bucket: the bucket to read the contents of

  Returns:
    a list of bucket entries excluding all entries starting with "latest"
  """
  _PrintSeparator('_ReadBucket({0}, {1})'.format(gsu, bucket))
  elements = []
  items = gsu.ReadBucket(bucket)
  for item in items:
    dirpaths = item.split('/')
    if len(dirpaths) >= 3:
      dirname = dirpaths[3]
      if dirname != 'latest':
        try:
          dirnum = int(dirname)
          if not dirnum in elements:
            elements.append(dirnum)
        except ValueError:
          pass

  return elements


def _RemoveElements(gsu, bucket, version_dirs, keepcount):
  """Remove the selected elements from Google Storage.

  Args:
    gsu: the gsutil program to run
    bucket: the bucket to remove the dirs from
    version_dirs: the dictionary of elements to remove keyed by
                  svn version number
    keepcount: the number of elements to keep
  """
  _PrintSeparator('_RemoveElements({0}, version_dirs,'
                  ' {1}'.format(gsu, keepcount))
  version_dirs_size = len(version_dirs)
  delete_count = version_dirs_size - keepcount
  if delete_count > 0:
    count = 0
    version_dirs.sort()
    for gs_dir in version_dirs:
      if count < delete_count:
        gsu.RemoveAll('{0}/{1}/*'.format(bucket, gs_dir))
      else:
        print 'version {0}/{1} will be saved'.format(bucket, gs_dir)
      count += 1
  else:
    print ('nothing to delete because that are only {0} elemens in the list'
           ' and the keep count is set to {0}').format(len(version_dirs),
                                                       keepcount)


def _PromoteBuild(revision, from_bucket, to_bucket):
  """Promote a build from one bucket to another.

  Args:
    revision: the revision to promote
    from_bucket: the bucket to promote from
    to_bucket: the bucket to promote to
  """
  
  src = '{0}/{1}/*'.format(from_bucket, revision)
  integ_rev = '{0}/{1}/'.format(to_bucket, revision)
  integ_latest = '{0}/{1}/'.format(to_bucket, 'latest')

  # copy from continuous/REVISION to integration/REVISION
  print 'copying: {0} -> {1}'.format(src, integ_rev)
  _Gsutil(['cp', '-R', '-a', 'public-read', src, integ_rev])
  _Gsutil(['rm', '-R', '{0}tests'.format(integ_rev)])

  # copy from continuous/REVISION to integration/latest
  print 'copying: {0} -> {1}'.format(src, integ_latest)
  _Gsutil(['cp', '-R', '-a', 'public-read', src, integ_latest])
  _Gsutil(['rm', '-R', '{0}tests'.format(integ_latest)])


def _PrintSeparator(text):
  print '================================'
  print '== %s' % text


def _PrintFailure(text):
  print '*****************************'
  print '** %s' % text
  print '*****************************'


def _Gsutil(cmd):
  gsutilTool = join(DART_PATH, 'third_party', 'gsutil', 'gsutil')
  return _ExecuteCommand([sys.executable, gsutilTool] + cmd)


def _ExecuteCommand(cmd, directory=None):
  """Execute the given command."""
  if directory is not None:
    cwd = os.getcwd()
    os.chdir(directory)
  subprocess.call(cmd, env=os.environ)
  if directory is not None:
    os.chdir(cwd)


if __name__ == '__main__':
  sys.exit(main())
