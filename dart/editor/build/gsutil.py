#!/usr/bin/python

"""Copyright 2011 the Dart project authors. All rights reserved.

Use of this source code is governed by a BSD-style license that can be
found in the LICENSE file.

Cleanup the Google Storage dart-editor-archive-continuous bucket.
"""
import os
import string
import subprocess
import tempfile
import xml.etree.ElementTree as ET


class GsUtil(object):
  """Class to abstract the gsutil calls from the program."""

  _gsutil = None
  _dryrun = False
  _group_ids = {}

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
    for line in open('groupIds.txt', 'r'):
      (first, _, rest) = line.strip().partition(',')
      self._group_ids[first] = rest
    for key in self._group_ids.iterkeys():
      print '{0} = |{1}|'.format(key, self._group_ids[key])

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

  def GetAcl(self, item_uri):
    """Get the ACL on an object in GoogleStorage.

    Args:
      item_uri: the uri of the item to get the acl for

    Returns:
      the ACL for the object or None if it could nto be found
    """
    args = []
    args.append(self._gsutil)
    args.append('getacl')
    args.append(item_uri)
    #echo the command to the screen
    print ' '.join(args)
    p = subprocess.Popen(args, stdout=subprocess.PIPE,
                         stderr=subprocess.PIPE)
    (out, err) = p.communicate()
    if p.returncode:
      failure_message = 'failed to getacl {0}\n'.format(item_uri)
      self._LogStream(err, failure_message, True)
      message = None
    else:
      message = ''
      for ch in out:
        message += ch
    return message

  def CreateAcl(self, acl, who='editors'):
    """Create the new ACL for the Object.

    Args:
      acl: the xml document representing the ACL
      who: who should be added onto the team list

    Returns:
      xml document with updated ACL
    """
    root = ET.fromstring(acl)
  #  root = dom.getroot()
    entries = root.find('Entries')
    foundentries = entries.findall('Entry')
    foundallusers = False
    for entry in foundentries:
      scope = entry.find('Scope')
      scopetype = scope.get('type')
      if scopetype is not None and scopetype == 'AllUsers':
        foundallusers = True

    teamentry = ET.SubElement(entries, 'Entry')
    teamscope = ET.SubElement(teamentry, 'Scope', type='GroupById')
    teamid = ET.SubElement(teamscope, 'ID')
    teamid.text = self._group_ids[who]
    teampremission = ET.SubElement(teamentry, 'Permission')
    teampremission.text = 'FULL_CONTROL'

    if not foundallusers:
      allentry = ET.SubElement(entries, 'Entry')
      ET.SubElement(allentry, 'Scope', type='AllUsers')
      allpremission = ET.SubElement(allentry, 'Permission')
      allpremission.text = 'READ'

    return ET.tostring(root)

  def SetCannedAcl(self, object_uri, canned_acl):
    """Set a canned ACL on an object in GoogleStorage.

    for canned ACL's see
    http://code.google.com/apis/storage/docs/accesscontrol.html

    Args:
      object_uri: the uri of the item to set the acl on
      canned_acl: predefined ACL defined at the URI above
    """
    self._GsutilSetAcl(object_uri, canned_acl)

  def SetAcl(self, object_uri, acl_content):
    """Set the ACL on an object in GoogleStorage.

    Args:
      object_uri: the uri of the item to set the acl on
      acl_content:an XML document setting the ACL
    """
    xmlfile = None
    try:
      if not self._dryrun:
        #the ACL is an XML document.  Write to to a temp file and
        #  then pass the temp file to the gsutil command
        xmlfile = tempfile.NamedTemporaryFile(suffix='.xml', prefix='GsACL',
                                              delete=True)
        try:
          f = open(xmlfile.name, 'w')
          f.write(acl_content)
        finally:
          f.close()

        self._GsutilSetAcl(object_uri, xmlfile.name)
    finally:
      if xmlfile is not None:
        xmlfile.close()

  def _GsutilSetAcl(self, object_uri, acl):
    """Call gsutil to set the given ACL on a Google Storeage object.

    Failing to set an ACL is not considered a fatal error so a message is
    printed but the program continues

    Args:
      object_uri: the object to set the ACL on
      acl: The ACL to set for object_uri.  This can be a canned acl or a file
            containing the xml document for the ACL
    """
    args = []
    args.append(self._gsutil)
    args.append('setacl')
    args.append(acl)
    args.append(object_uri)
    #echo the command to the screen
    print ' '.join(args)
    if not self._dryrun:
      p = subprocess.Popen(args, stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE)
      (out, err) = p.communicate()
      if p.returncode:
        failure_message = 'failed to setacl {0}\n'.format(object_uri)
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

