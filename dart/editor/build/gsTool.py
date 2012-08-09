#!/usr/bin/env python
# 
# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# Cleanup the Google Storage dart-editor-archive-continuous bucket.

import optparse
import sys
import gsutil

CONTINUOUS = 'gs://dart-editor-archive-continuous'
TESTING = 'gs://dart-editor-archive-testing'
INTEGRATION = 'gs://dart-editor-archive-integration'
RELEASE = 'gs://dart-editor-archive-release'


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
      cleanup Google Storage saving the last 1000 (default value) revision
        python gsTool.py cleanup

      cleanup saving the last 150 revisions
        python gsTool.py cleanup --keepcount=150

      promote revision 567 from continuous to integration
        python gsTool.py promote --revision=567 --continuous

      promote revision 567 to integration to release
        python gsTool.py promote --revision=567 --integration"""

  result = optparse.OptionParser(usage=usage)
  result.set_default('gsbucketuri', 'gs://dart-editor-archive-continuous')
  result.set_default('keepcount', 1000)
  result.set_default('dryrun', False)
  result.set_default('continuous', False)
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
    if not (options.continuous or options.integration or options.testing):
      print 'You must specify one of --continuous or --integration or --testing'
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
  """Remove the delected elements from Google Storage.

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
  print '_PromoteBuild(gsu, {0} , {1}, {2})'.format(revision, from_bucket,
                                                    to_bucket)

  elements = gsu.ReadBucket('{0}/{1}/*'.format(from_bucket, revision))
  for element in elements:
    if not '/tests/' in element:
      from_element = element
      to_element = element.replace(from_bucket, to_bucket)
      print 'promoting {0} to {1}'.format(from_element, to_element)
      gsu.Copy(from_element, to_element)
      acl = gsu.GetAcl(to_element)
      if acl is None:
        _PrintFailure('non-fatal failure, could not get'
                      ' ACL for {0}'.format(to_element))
      else:
        newacl = gsu.AddPublicAcl(acl)
        gsu.SetAcl(to_element, newacl)
      to_element = to_element.replace(revision, 'latest')
      gsu.Copy(from_element, to_element)
      acl = gsu.GetAcl(to_element)
      if acl is None:
        _PrintFailure('non-fatal failure, could not get'
                      ' ACL for {0}'.format(to_element))
      else:
        newacl = gsu.AddPublicAcl(acl)
        gsu.SetAcl(to_element, newacl)


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
