# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# Cleanup the Google Storage dart-editor-archive-continuous bucket.

import os
import platform
import StringIO
import subprocess
import tempfile
import xml.etree.ElementTree as ET


class GsUtil(object):
  """Class to abstract the gsutil calls from the program."""

  _gsutil = None
  _dryrun = False
  _useshell = False  # this also implies windows
  _running_on_buildbot = False

  def __init__(self, dryrun=False, gsutil_loc=None, running_on_buildbot=True):
    """Initialize the class by finding the gsutil programs location.

    Args:
      dryrun: this is a dry run only show the things that would change the
            Google Storage don't actually do it
      gsutil_loc:  the location of the gsutil program if this is None then
                    the program will use ~/gsutil/gsutil
      running_on_buildbot: this will be True if the build is runing on
                            buildBot and False if this is a local build
    """
    self._gsutil = self._FindGsUtil(gsutil_loc)
    self._dryrun = dryrun
    self._running_on_buildbot = running_on_buildbot

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
    operating_system = platform.system()
    if operating_system == 'Windows' or operating_system == 'Microsoft':
    # On Windows Vista platform.system() can return "Microsoft" with some
    # versions of Python, see http://bugs.python.org/issue1082 for details.
      # TODO(devoncarew): remove this hardcoded e:\ path
      bot_gs_util = 'e:\\b\\build\\scripts\\slave\\gsutil'
      self._useshell = True
    if gsutil_loc is None:
      home_gs_util = os.path.join(os.path.expanduser('~'), 'gsutil', 'gsutil')
    else:
      home_gs_util = gsutil_loc

    path_gs_util = ''
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
      msg = 'Could not find gsutil.  '
      msg += 'Tried {0}, {1}'.format(bot_gs_util, home_gs_util)
      if path_gs_util is not None:
        msg += ', {0}'.format(path_gs_util)
      raise Exception(msg)
    print 'using gsutil from {0}'.format(gsutil)
    return gsutil

  def _LogStream(self, stream, header, error_flag=False):
    """Log the contents of a stream to the stdout.

    Args:
      stream: the stream to write to stdout
      header: the header to print before the stream
      error_flag: True this is an error message, false this is informational
    """
    if error_flag:
      self._PrintFailure('{0}'.format(header))
    else:
      print header
    print str(stream)

  def ReadBucket(self, bucket):
    """Read the contents of a bucket.

    Args:
      bucket: the bucket to read

    Returns:
      list of the uri's for the elements in the bucket
    """
    args = []
    args.extend(self._CommandGsutil())
    args.append('ls')
    args.append(bucket)

    print ' '.join(args)

    items = []
    p = subprocess.Popen(args, stdout=subprocess.PIPE,
                         stderr=subprocess.PIPE,
                         shell=self._useshell)
    while p.poll() is None:
      line = p.stdout.readline()
      line = line.strip()
      if line.startswith('gs:'):
        items.append(line)

    if p.returncode:
      failure_message = ('failed to read the contents'
                         ' of the bucket {0}\n').format(bucket)
      self._LogStream(p.stderr.read(), failure_message, True)
      return []

    return items

  def Copy(self, from_uri, to_uri, public_flag=True, recursive_flag=False):
    """Use GsUtil to copy data.

    Args:
      from_uri: the location to copy from
      to_uri: the location to copy to
      public_flag: flag indicating that the file should be readable from
                    the Internet
      recursive_flag: copy files recursively to Google Storage

    Returns:
      returns the exit code of gsutil copy
    """
    cmd = []
    cmd.extend(self._CommandGsutil())
    cmd.append('cp')
    if recursive_flag:
      cmd.append('-r')
    if public_flag:
      cmd.append('-a')
      cmd.append('public-read')
    index_col = from_uri.find(':')
    from_url = from_uri
    if index_col < 0:
      from_url = r'file://' + from_uri
    else:
      scheme = from_uri[:index_col]
      if len(scheme) <= 1:
        from_url = r'file://' + from_uri
    to_url = to_uri

    index_col = to_uri.find(':')
    if index_col < 0:
      to_url = r'file://' + to_uri
    else:
      scheme = to_uri[:index_col]
      if len(scheme) <= 1:
        to_url = r'file://' + to_uri
    # On windows gsutil does not convert \ to / on a recursive copy.
    # Therefore the data loaded to GoogleStorage from directory
    # 3333\test\data.txt looks like an object 3333\test\data.txt
    # in the root of the bucket.
    if self._useshell and recursive_flag:
      return self._TreeWalkCopy(from_url, to_url, public_flag)
    else:
      cmd.append(from_url)
      cmd.append(to_url)

      print ' '.join(cmd)
      if not self._dryrun:
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE,
                             shell=self._useshell)
        (out, err) = p.communicate()
        if p.returncode:
          failure_message = 'failed to copy {0} to {1}'.format(from_uri, to_uri)
          self._LogStream(err, failure_message, True)
        else:
          self._LogStream(out, '')
        return p.returncode
    return 0

  def _TreeWalkCopy(self, from_url, to_url, public_flag=True):
    """Do the recursive copy by walking the directory tree.

    Gsutil does not convert \ to / so the data loaded to GoogleStorage
      looks like 3333\test\data.txt in the root of the bucket

    Args:
      from_url: the location to copy from
      to_url: the location to copy to
      public_flag: flag indicating that the file should be readable from
                    the Internet

    Returns:
      returns the exit code of gsutil copy

    Raises:
      Exception: if the schema of the from url is not gs:
    """
    print 'walktree ({0}, {1}, pub = {2})'.format(from_url, to_url, public_flag)
    pos = from_url.find(':')
    if pos >= 0:
      scheme = from_url[:pos]
      path = from_url[pos + 3:]
    else:
      scheme = ''
      path = from_url
    target_path_element = os.path.basename(path)
    elements_to_copy = []
    if scheme is 'gs':
      raise Exception('gs can not be the scheme of the from URL in a '
                      'GsUtil.Copy command')
    print 'schema = ({0}), path = ({1})'.format(scheme, path)
    if 'file' in scheme or not scheme:
      for root, dirs, files in os.walk(path):
        for f in files:
          elements_to_copy.append(os.path.join(root, f))

    cmd = []
    cmd.extend(self._CommandGsutil())
    cmd.append('cp')
    if public_flag:
      cmd.append('-a')
      cmd.append('public-read')

    copy_target = None
    for element in elements_to_copy:
      full_cmd = []
      full_cmd.extend(cmd)
      #if a Windows Drive letter is on the file that is being copied then
      # Gsutil will treat it as a scheme.  So any file with a windows drive
      # letter has to have file:// appended to it
      colon_pos = element.find(':')
      if colon_pos >= 0:
        scheme = element[:colon_pos]
        if len(scheme) <= 1:
          full_cmd.append('file://' + element)
        else:
          full_cmd.append(element)
      else:
        full_cmd.append(element)
      pos = element.find(target_path_element)
      if pos >= 0:
        copy_target = '{0}/{1}'.format(to_url, element[pos:].replace('\\', '/'))
        full_cmd.append(copy_target)
      else:
        print 'could not find {0} in {1}'.format(target_path_element,
                                                 element)
        continue

      print ' '.join(full_cmd)
      if not self._dryrun:
        p = subprocess.Popen(full_cmd, stdout=subprocess.PIPE,
                             stderr=subprocess.PIPE,
                             shell=self._useshell)
        (out, err) = p.communicate()
        if p.returncode:
          failure_message = 'failed to copy {0}\n to {1}'.format(element,
                                                                 copy_target)
          self._LogStream(err, failure_message, True)
          return p.returncode
        else:
          print str(out)
    return 0

  def Move(self, from_uri, to_uri, preserve_acl_flag=True):
    """Use GsUtil to move/rename an element.

    Args:
      from_uri: the location to copy from (mist be gs:)
      to_uri: the location to copy to (must be gs:)
      preserve_acl_flag: causes ACL to be preserved when renaming

    Returns:
      returns the exit code of gsutil copy
    """
    cmd = []
    cmd.extend(self._CommandGsutil())
    cmd.append('mv')
    if preserve_acl_flag:
      cmd.append('-p')
    from_url = from_uri
    if not from_url.startswith('gs://'):
      self._PrintFailure('from URL {0} does not '
                         'start with gs://'.format(from_url))
      return 1

    to_url = to_uri
    if not to_url.startswith('gs://'):
      self._PrintFailure('to URL {0} does not start with gs://'.format(to_url))
      return 1

    cmd.append(from_url)
    cmd.append(to_url)

    print ' '.join(cmd)
    if not self._dryrun:
      p = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE,
                           shell=self._useshell)
      (out, err) = p.communicate()
      if p.returncode:
        failure_message = 'failed to move {0} to {1}'.format(from_uri, to_uri)
        self._LogStream(err, failure_message, True)
      else:
        self._LogStream(out, '')
      return p.returncode
    return 0

  def RemoveAll(self, item_uri):
    """remove an item form GoogleStorage (rm -R).
    """
    args = []
    args.extend(self._CommandGsutil())
    args.append('rm')
    args.append('-R')
    args.append(item_uri)
    #echo the command to the screen
    print ' '.join(args)
    if not self._dryrun:
      p = subprocess.Popen(args, stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE,
                           shell=self._useshell)
      (out, err) = p.communicate()
      if p.returncode:
        failure_message = 'failed to remove {0}\n'.format(item_uri)
        self._LogStream(err, failure_message, True)
      else:
        self._LogStream(out, '')
          
  def Remove(self, item_uri):
    """remove an item form GoogleStorage.

    Args:
      item_uri: the uri of the item to remove
    """
    args = []
    args.extend(self._CommandGsutil())
    args.append('rm')
    args.append(item_uri)
    #echo the command to the screen
    print ' '.join(args)
    if not self._dryrun:
      p = subprocess.Popen(args, stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE,
                           shell=self._useshell)
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
      the ACL for the object or None if it could not be found
    """
    args = []
    args.extend(self._CommandGsutil())
    args.append('getacl')
    args.append(item_uri)
    #echo the command to the screen
    print ' '.join(args)
    p = subprocess.Popen(args, stdout=subprocess.PIPE,
                         stderr=subprocess.PIPE,
                         shell=self._useshell)
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

  def AddPublicAcl(self, acl):
    """Create the new ACL for the Object.

    Args:
      acl: the xml document representing the ACL

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

    if not foundallusers:
      allentry = ET.SubElement(entries, 'Entry')
      ET.SubElement(allentry, 'Scope', type='AllUsers')
      allpremission = ET.SubElement(allentry, 'Permission')
      allpremission.text = 'READ'

    return ET.tostring(root)

  def SetAclFromFile(self, object_uri, acl_file):
    """Set the ACL on an object to the given ACL xml file.

    Args:
      object_uri: the uri of the item to set the ACL on
      acl_file: the file containing the ACL XML for this object
    """
    print 'SetAclFromFile({0}, {1})'.format(object_uri, acl_file)
    self.SetAcl(object_uri, open(acl_file, 'r').read())

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
    print 'SetAcl({0}, aclxml)'.format(object_uri)
    xmlfile = None
    try:
      if not self._dryrun:
        #the ACL is an XML document.  Write to to a temp file and
        #  then pass the temp file to the gsutil command
        xmlfile = tempfile.NamedTemporaryFile(suffix='.xml', prefix='GsACL',
                                              delete=False)
        print 'using temp file {0} to store the xml'.format(xmlfile.name)
        try:
          xmlfile.write(acl_content)
        finally:
          if xmlfile is None:
            print 'never opened the file'
          else:
            print 'closing {0}'.format(xmlfile.name)
            xmlfile.close()

        self._GsutilSetAcl(object_uri, xmlfile.name)
    finally:
      if xmlfile is not None:
        print 'removing {0}'.format(xmlfile.name)
        os.remove(xmlfile.name)

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
    args.extend(self._CommandGsutil())
    args.append('setacl')
    args.append(acl)
    args.append(object_uri)
    #echo the command to the screen
    print ' '.join(args)
    if not self._dryrun:
      p = subprocess.Popen(args, stdout=subprocess.PIPE,
                           stderr=subprocess.PIPE,
                           shell=self._useshell)
      (out, err) = p.communicate()
      if p.returncode:
        failure_message = 'failed to setacl {0}\n'.format(object_uri)
        self._LogStream(err, failure_message, True)
      else:
        self._LogStream(out, '')

  def _PrintFailure(self, text):
    """Print a failure message."""
    error_line_seperator = '*****************************'

    print
    print error_line_seperator
    print text
    print error_line_seperator
    print

  def _CommandGsutil(self):
    """Execute a gsutil command.

    Returns:
      the command to execute gsutil
    """
    args = []
    if not self._running_on_buildbot and self._useshell:
      args.append('python')
    args.append(self._gsutil)
    return args
