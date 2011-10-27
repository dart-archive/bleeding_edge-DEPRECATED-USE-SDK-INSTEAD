#!/usr/bin/python

"""Copyright 2011 the Dart project authors. All rights reserved.

Use of this source code is governed by a BSD-style license that can be
found in the LICENSE file.

Cleanup the Google Storage dart-editor-archive-continuous bucket.
"""
import optparse
import os
import sys
import gsutil

CONTINUOUS = 'gs://dart-editor-archive-continuous'
INTEGRATION = 'gs://dart-editor-archive-integration'
RELEASE = 'gs://dart-editor-archive-release'


def _BuildOptions():
  """Setup the argument processing for this program.

  Returns:
    the OptionParser to process the CLI
  """
  usage = """usage: %prog [options] cleanup|promote
  where:
    cleanup will cleanup the Google storage continupus bucket
    promote will promote code between different stages

    Examples:
      cleanup Google Storage saving the last 100 (default value) revision
        python gsTool.py cleanup

      cleanup saving the last 150 revisions
        python gsTool.py cleanup --keepcount=150

      promote revision 567 from continuous to integration
        python gsTool.py promote --revision=567 --continuous

      promote revision 567 to integration to release
        python gsTool.py promote --revision=567 --integration"""

  result = optparse.OptionParser(usage=usage)
  result.set_default('gsbucketuri', 'gs://dart-editor-archive-continuous')
  result.set_default('keepcount', 100)
  result.set_default('dryrun', False)
  result.set_default('continuous', False)
  result.set_default('integration', False)
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
                   help='the svn revision to promote',
                   action='store')
  group.add_option('--continuous',
                   help='promote from continuous',
                   action='store_true')
  group.add_option('--integration',
                   help='promote from integration',
                   action='store_true')
  result.add_option_group(group)

  result.add_option('--gsbucketuri',
                    help='Google Storage bucket URI.',
                    action='store')
  result.add_option('--dryrun', help='just print what it would do',
                    action='store_true')

  return result


def main():
  """Main entry point for the Google Storage Cleanup."""

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
    if options.continuous is None and options.integration is None:
      print 'You must specify one of --continuous or --integration'
      parser.print_help()
      sys.exit(4)
    if options.continuous and options.integration:
      print 'continuous and integration can not be specified at the same time'
      parser.print_help()
      sys.exit(5)

  elif args[0] == 'cleanup':
    command = 'cleanup'
    if options.keepcount is None:
      print 'You must specify --keepcount'
      parser.print_help()
      sys.exit(6)
  else:
    print 'At least one command must be specified'
    parser.print_help()
    sys.exit(2)

  gsu = gsutil.GsUtil(options.dryrun)

  if options.continuous:
    bucket_from = CONTINUOUS
    bucket_to = INTEGRATION
  elif options.integration:
    bucket_from = INTEGRATION
    bucket_to = RELEASE

  if command == 'cleanup':
    version_dirs = _ReadBucket(gsu, options.gsbucketuri)
    _RemoveElements(gsu, version_dirs, options.keepcount)
  elif command == 'promote':
    _PromoteBuild(gsu, options.revision, bucket_from, bucket_to)


def _ReadBucket(gsu, bucket):
  """Read the contents of a Google Storage Bucket.

  Args:
    gsu: the location of the gsutil program
    bucket: the bucket to read the contents of

  Returns:
    a list of bucket entries excluding all entries starting with "latest"
  """
  _PrintSeparator('_ReadBucket({0}, {1})'.format(gsu, bucket))
  elements = {}
  items = gsu.ReadBucket(bucket)
  for item in items:
    dirname = os.path.basename(os.path.dirname(item))
    if dirname != 'latest':
      dirnum = int(dirname)
      if dirnum in elements:
        elements[dirnum].append(item)
      else:
        elements[dirnum] = [item]
  return elements


def _RemoveElements(gsu, version_dirs, keepcount):
  """Remove the delected elements from Google Storage.

  Args:
    gsu: the gsutil program to run
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
    keys = version_dirs.keys()
    keys.sort()
    for key in keys:
      if count < delete_count:
        for dirname in version_dirs[key]:
          gsu.Remove(dirname)
      else:
        print 'version {0} will be saved'.format(key)
      count += 1
  else:
    print ('nothing to delete because that are only {0} elemens in the list'
           ' and the keep count is set to {0}').format(len(version_dirs),
                                                       keepcount)


def _PromoteBuild(gsu, revision, from_bucket, to_bucket):
  """Promote a build to another environment.

    because Google Storage does not support symbolic links two copies need
    to be made one with the revision number and one with latest
  Args:
    gsu: the gsutil class
    revision: the revision to promote
    from_bucket: the bucket to promote from
    to_bucket: the bucket to promote from
  """
  print '_PromoteBuild({0} , {1}, {2})'.format(gsu, from_bucket, to_bucket)

  elements = _ReadBucket(gsu, from_bucket)
  rev_int = int(revision)
  if rev_int in elements:
    for element in elements[rev_int]:
      from_element = element
      to_element = element.replace(from_bucket, to_bucket)
      print 'promoting {0} to {1}'.format(from_element, to_element)
      gsu.Copy(from_element, to_element)
      to_element = to_element.replace(revision, 'latest')
      gsu.Copy(from_element, to_element)
  else:
    print 'could not find element with {0} as it\'s revision'.format(revision)


def _PrintSeparator(text):
  """Print a separator for the build steps."""
  #used to print separators during the build process
  tag_line_seperator = '================================'
  tag_line_text = '= {0}'

  print tag_line_seperator
  print tag_line_text.format(text)


def _PrintFailure(text):
  """Print a failure message."""
  error_line_seperator = '*****************************'
  error_line_text = '{0}'

  print error_line_seperator
  print error_line_seperator
  print error_line_text.format(text)
  print error_line_seperator
  print error_line_seperator


if __name__ == '__main__':
  sys.exit(main())
