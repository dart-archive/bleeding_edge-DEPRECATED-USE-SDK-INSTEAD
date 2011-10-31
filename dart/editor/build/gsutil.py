#!/usr/bin/python

"""Copyright 2011 the Dart project authors. All rights reserved.

Use of this source code is governed by a BSD-style license that can be
found in the LICENSE file.

Cleanup the Google Storage dart-editor-archive-continuous bucket.
"""
import os
import string
import subprocess


class GsUtil(object):
  """Class to abstract the gsutil calls from the program."""

  _gsutil = None
  _dryrun = False

  def __init__(self, dryrun=False, gsutil_loc=None):
    """Initialize the class by finding the gsutil programs location.

    Args:
      dryrun: this is a dry run only show the things that would change the
            Google Storage don't actually do it
      gsutil_loc:  the location of the gsutil program if this is None then
                    the program will use ~/gsutil/gsutil
    """
    self._gsutil = self._FindGsUtil(gsutil_loc)
    self._dryrun = dryrun

  def _FindGsUtil(self, gsutil_loc):
    """Find the location of the gsutil program.

    Args:
      gsutil_loc: location of gsutil

    Returns:
      the location of gsutil

    Raises:
      Exception: gsutil is not found
    """
    bot_gs_util = '/b/build/scripts/slave/gsutil'
    if gsutil_loc is None:
      home_gs_util = os.path.join(os.path.expanduser('~'), 'gsutil', 'gsutil')
    else:
      home_gs_util = gsutil_loc
    path = os.environ['PATH']
    if path is not None:
      pathelements = path.split(os.pathsep)
      for pathelement in pathelements:
        gs_util_candidate = os.path.join(pathelement, 'gsutil')
        if os.path.exists(gs_util_candidate):
          path_gs_util = gs_util_candidate
          break

    gsutil = None
    if os.path.exists(bot_gs_util):
      gsutil = bot_gs_util
    elif os.path.exists(home_gs_util):
      gsutil = home_gs_util
    elif path_gs_util is not None:
      gsutil = path_gs_util
    else:
      raise Exception('could not find gsutil.'
                      '  Tried {0} and {1}'.format(bot_gs_util, home_gs_util))
    return gsutil

  def _LogStream(self, stream, header, error_flag=False):
    """Log the contents of a stream to the stdout.

    Args:
      stream: the stream to write to stdout
      header: the header to print before the stream
      error_flag: True this is an error message, false this is informational
    """
    if error_flag:
      self._PrintFailure('{0}\nsee details below'.format(header))
    else:
      print header
    message = ''
    for ch in stream:
      message += ch
    print message

  def ReadBucket(self, bucket):
    """Read the contents of a bucket.

    Args:
      bucket: the bucket to read

    Returns:
      list of the uri's for the elements in the bucket
    """
    args = []
    args.append(self._gsutil)
    args.append('ls')
    args.append(bucket)

    print ' '.join(args)

    p = subprocess.Popen(args, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    (out, err) = p.communicate()
    if p.returncode:
      failure_message = ('failed to read the contents'
                         ' of the bucket {0}\n').format(bucket)
      self._LogStream(err, failure_message, True)
    else:
      line = ''
      items = []
      search_string = string.digits + string.letters + string.punctuation
      for ch in out:
        if search_string.find(ch) >= 0:
          line += ch
        elif not line:
          continue
        else:
          items.append(line)
          line = ''
    return items

  def Copy(self, from_uri, to_uri, public_flag=True):
    """Use GsUtil to copy data.

    Args:
      from_uri: the location to copy from
      to_uri: the location to copy to
      public_flag: flag indicating that the file should be readable from
                    the internet
    """
    cmd = [self._gsutil, 'cp']
    if public_flag:
      cmd.append('-a')
      cmd.append('public-read')
    cmd.append(from_uri)
    cmd.append(to_uri)

    print ' '.join(cmd)
    if not self._dryrun:
      p = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE)
      (out, err) = p.communicate()
      if p.returncode:
        failure_message = 'failed to copy {0} to {1}'.format(from_uri, to_uri)
        self._LogStream(err, failure_message, True)
      else:
        self._LogStream(out, '')

  def Remove(self, item_uri):
    """remove an item form GoogleStorage.

    Args:
      item_uri: the uri fo the item to remove
    """
    args = []
    args.append(self._gsutil)
    args.append('rm')
    args.append(item_uri)
    #echo the command to the screen
    print ' '.join(args)
    if not self._dryrun:
      p = subprocess.Popen(args, stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE)
      (out, err) = p.communicate()
      if p.returncode:
        failure_message = 'failed to remove {0}\n'.format(item_uri)
        self._LogStream(err, failure_message, True)
      else:
        self._LogStream(out, '')

  def _PrintFailure(self, text):
    """Print a failure message."""
    error_line_seperator = '*****************************'
    error_line_text = '{0}'

    print error_line_seperator
    print error_line_seperator
    print error_line_text.format(text)
    print error_line_seperator
    print error_line_seperator

