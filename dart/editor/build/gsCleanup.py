#!/usr/bin/python

"""Copyright 2011 the Dart project authors. All rights reserved.

Use of this source code is governed by a BSD-style license that can be
found in the LICENSE file.

Cleanup the Google Storage dart-editor-archive-continuous bucket.
"""

import optparse
import os
import string
import subprocess
import sys


def _BuildOptions():
  """Setup the argument processing for this program.

  Returns:
    the OptionParser to process the CLI
  """
  result = optparse.OptionParser()
  result.set_default('gsbucketuri', 'gs://dart-editor-archive-continuous')
  result.set_default('keepcount', 10)
  result.set_default('dryrun', False)
  result.add_option('--keepcount',
                    type='int',
                    help='Numer of Builds to keep.',
                    action='store')
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

  dryrun = options.dryrun
  gsutil = _FindGsUtil()

  if gsutil:
    version_dirs = _ReadBucket(gsutil, options.gsbucketuri)
    _RemoveElements(gsutil, version_dirs, options.keepcount, dryrun)


def _FindGsUtil():
  """Find the location of the gsutil program.

  Returns:
    the location of gsutil or None if not found
  """
  _PrintSeparator('_FindGsUtil')
  bot_gs_util = '/b/build/scripts/slave/gsutil'
  home_gs_util = os.path.join(os.path.expanduser('~'), 'gsutil', 'gsutil')
  gsutil = None
  if os.path.exists(bot_gs_util):
    gsutil = bot_gs_util
  elif os.path.exists(home_gs_util):
    gsutil = home_gs_util
  else:
    _PrintFailure('could not find gsutil.'
                  '  Tried {0} and {1}'.format(bot_gs_util, home_gs_util))
  return gsutil


def _ReadBucket(gsutil, bucket):
  """Read the contents of a Google Storage Bucket.

  Args:
    gsutil: the location of the gsutil program
    bucket: the bucket to read the contents of

  Returns:
    a list of bucket entries
  """
  _PrintSeparator('_ReadBucket({0}, {1})'.format(gsutil, bucket))
  elements = {}
  args = []
  args.append(gsutil)
  args.append('ls')
  args.append(bucket)

  print ' '.join(args)

  p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
  (out, err) = p.communicate()
  if p.returncode:
    failure_message = ('failed to read the contents'
                       ' of the bucket {0}\n').format(bucket)
    for ch in err:
      failure_message += ch
    _PrintFailure(failure_message)
  else:
    line = ''
    search_string = string.digits + string.letters + string.punctuation
    for ch in out:
      if search_string.find(ch) >= 0:
        line += ch
      elif not line:
        continue
      else:
        dirname = os.path.basename(os.path.dirname(line))

        if dirname != 'latest':
          dirnum = int(dirname)
          if dirnum in elements:
            elements[dirnum].append(line)
          else:
            elements[dirnum] = [line]
        line = ''
  return elements


def _RemoveElements(gsutil, version_dirs, keepcount, dryrun):
  """Remove the delected elements from Google Storage.

  Args:
    gsutil: the gsutil program to run
    version_dirs: the dictionary of elements to remove keyed by
                  svn version number
    keepcount: the number of elements to keep
    dryrun: in a dry run just print what it is going to do
  """
  _PrintSeparator('_RemoveElements({0}, version_dirs,'
                  ' {1}, {2}'.format(gsutil, keepcount, dryrun))
  version_dirs_size = len(version_dirs)
  delete_count = version_dirs_size - keepcount
  if delete_count > 0:
    count = 0
    keys = version_dirs.keys()
    keys.sort()
    for key in keys:
      if count < delete_count:
        for dirname in version_dirs[key]:
          args = []
          args.append(gsutil)
          args.append('rm')
          args.append(dirname)
          #echo the command to the screen
          print ' '.join(args)
          if not dryrun:
            p = subprocess.Popen(args, stdout=subprocess.PIPE, 
                                 stderr=subprocess.PIPE)
            (out, err) = p.communicate()
            if p.returncode:
              failure_message = ('failed to remove {0}\n').format(dirname)
              for ch in err:
                failure_message += ch
              _PrintFailure(failure_message)
            else:
              for ch in out:
                print ch
      else:
        print 'version {0} will be saved'.format(key)
      count += 1


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
