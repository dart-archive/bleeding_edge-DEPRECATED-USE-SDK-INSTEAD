#!/usr/bin/python

"""Copyright 2012 the Dart project authors. All rights reserved.

Python file to test gsutil.py.
"""
import os
import platform
import shutil
import tempfile
import unittest
import gsutil

# access to protected members of gsutil.py
# pylint: disable=W0212

class TestGsutil(unittest.TestCase):
  """Class to test the gsutil.py class."""
  test_prefix = 'gs://'
  test_bucket = 'dart-editor-archive-testing'
  test_folder = 'unit-testing'
  build_count = 3

  def setUp(self):
    """Setup the test."""
    self._iswindows = False
    operating_system = platform.system()
    if operating_system == 'Windows' or operating_system == 'Microsoft':
    # On Windows Vista platform.system() can return "Microsoft" with some
    # versions of Python, see http://bugs.python.org/issue1082 for details.
      self._iswindows = True
    username = os.environ.get('USER')
    if username is None:
      username = os.environ.get('USERNAME')

    if username is None:
      self.fail('could not find the user name tried environment variables'
                ' USER and USERNAME')
    self.test_folder = 'test_folder-{0}-{1}'.format(username, operating_system)
    if username.startswith('chrome'):
      self.running_on_buildbot = True
    else:
      self.running_on_buildbot = False

    self._gsu = gsutil.GsUtil(running_on_buildbot=self.running_on_buildbot)
    self._CleanFolder(self.test_folder)
    self._SetupFolder(self.test_bucket, self.test_folder, self.build_count)

  def tearDown(self):
    """Teardown the test."""
    self._gsu = None

  def test_initialization(self):
    """test the initialization of the GsUtil class."""
    #find out if gsutil is on the path
    path_gs_util = None
    path = os.environ['PATH']
    if path is not None:
      pathelements = path.split(os.pathsep)
      for pathelement in pathelements:
        gs_util_candidate = os.path.join(pathelement, 'gsutil')
        if os.path.exists(gs_util_candidate):
          path_gs_util = gs_util_candidate
          break

    path_gs_util = None
    # ensure that the creation will find an instance of gsutil
    if path_gs_util is not None:
      self.assertFalse(self._gsu is None)
      self.assertEqual(self._gsu._gsutil, path_gs_util)
    else:
      #should be running on the build server
      if self._iswindows:
        gsutilexe = 'e:\\b\\build\\scripts\\slave\\gsutil'
        self.assertTrue(self._gsu._useshell)
      else:
        gsutilexe = '/b/build/scripts/slave/gsutil'
        self.assertFalse(self._gsu._useshell)
      self.assertEqual(gsutilexe, self._gsu._gsutil)
      self.assertFalse(self._gsu._dryrun)

  def test_readBucket(self):
    """Test GsUtil.ReadBucket to make sure it can read the bucket."""
    objects = self._FindInBucket('/' + self.test_folder + '/')
    self.assertEqual(self.build_count, len(objects))

  def test_copyObject(self):
    """Test GsUtil.CopyObject to make sure it is actually doing the copy."""
    # by setUp running the copy of a file from GoogleStorage works so it
    #  will not be tested here
    test_string = '-2-'
    test_string2 = '-22-'
    objects = self._FindInBucket(test_string)
    self.assertEqual(1, len(objects))
    copy_from = objects[0]
    copy_to = objects[0].replace(test_string, test_string2)
    self._gsu.Copy(copy_from, copy_to, False)
    objects = self._FindInBucket(test_string2)
    self.assertEqual(1, len(objects))
    self.assertTrue(test_string2 in objects[0])

  def test_removeObject(self):
    """Test GsUtil.RemoveObject.

    Make sure it removes only the selected object.
    """
    local_folder = '/{0}/'.format(self.test_folder)
    self._CleanFolder(self.test_folder)
    objects = self._FindInBucket(local_folder)
    self.assertEqual(0, len(objects))

#  @unittest.skip("Not complete yet")
  def test_getAclOnObject(self):
    """Test GsUtil.GetAcl to make sure it returns the correct ACL XML."""
    search_string = '-2-'
    objects = self._FindInBucket(search_string)
    self.assertEqual(1, len(objects))
    acl_xml = self._gsu.GetAcl(objects[0])
    self.assertTrue(acl_xml)

  def test_copyTree(self):
    """Test GsUtil.Copying a tree of objects.

    Make sure Make sure the objects get coppied to the correct
    structure in Google Storage
    """
    print '*' * 50
    print 'test_copyTree'
    print '*' * 50
    parent_dir = None
    test_uri = '{0}{1}'.format(self.test_prefix, self.test_bucket)
    tag = 'CopyTree'
    try:
      (test_dir, files_created) = self._SetupDirectoryTree(3, 2, tag)
      parent_dir = os.path.dirname(test_dir)
      self._CleanFolder(self.test_folder)
      objects = self._FindInBucket(tag)
      self.assertFalse(len(objects))
      self._gsu.Copy(test_dir, test_uri, recursive_flag=True)
      objects = self._FindInBucket(tag)
      self.assertEqual(len(objects), files_created,
                       msg='using the recursive gsutil'
                       'call{0}, {1} != {2}'.format(test_dir,
                                                    len(objects),
                                                    files_created))
      self._CleanFolder(self.test_folder)
      objects = self._FindInBucket(tag)
      self.assertFalse(len(objects))
      self._gsu._TreeWalkCopy('file://' + test_dir, test_uri)
      objects = self._FindInBucket(tag)
      self.assertEqual(len(objects), files_created,
                       msg='using the tree walker '
                       '{0}, {1} != {2}'.format(test_dir, len(objects),
                                                files_created))
    finally:
      if parent_dir is not None:
        shutil.rmtree(parent_dir, ignore_errors=True)

  def _FindInBucket(self, search_string):
    """Return a list of objects that match a given search string.

    Args:
      search_string: the string to match

    Returns:
      a collection (possibly empty) of the objects that match the search string
    """
    test_uri = '{0}{1}/{2}/*'.format(self.test_prefix, self.test_bucket,
                                     self.test_folder)
    gs_objects = self._gsu.ReadBucket(test_uri)
    objects = []
    for obj in gs_objects:
      if search_string in obj:
        objects.append(obj)
    return objects

  def _SetupDirectoryTree(self, dir_depth, items, tag):
    """setup a directory tree to test CopyTree.

    Args:
      dir_depth: the depth of the directories
      items: the number of files in each directory
      tag: the tag to add to the files

    Returns:
      Root of the created directory structure.
    """
    tmp_dir = tempfile.mkdtemp(prefix=tag)
    sub_dir = os.path.join(tmp_dir, self.test_folder)
    os.makedirs(sub_dir)
    dirs = self._CreateDirectory(sub_dir, dir_depth)
    files_created = 0
    for d in dirs:
      for file_count in range(0, items):
        fname = tag + str(file_count)
        tmp_file = tempfile.NamedTemporaryFile(suffix='.txt',
                                               prefix=fname,
                                               delete=False,
                                               dir=d)
        tmp_file.write(str(file_count))
        files_created += 1
        tmp_file.close()
    return sub_dir, files_created

  def _CreateDirectory(self, base, items):
    dir_list = []
    if items > 0:
      for dir_num in range(items, 0, -1):
        new_dir = tempfile.mkdtemp(prefix='GsutilTest-{0}-'.format(dir_num),
                                   dir=base)
        dir_list.append(new_dir)
        dir_list.extend(self._CreateDirectory(new_dir, items - 1))
    return dir_list

  def _CleanFolder(self, folder):
    """CLean out a given folder.

    Args:
      folder: the name of the folder to clear
    """
    test_uri = '{0}{1}/{2}/*'.format(self.test_prefix, self.test_bucket,
                                     folder)
    print 'cleaning folder {0}'.format(test_uri)
    self._gsu.Remove(test_uri)

  def _SetupFolder(self, bucket, folder, items):
    """Setup a folder for testing.

    Args:
      bucket: the name of the bucket the folder is in
      folder: the folder to setup
      items: the number of itest
    """
    test_uri = '{0}{1}/{2}'.format(self.test_prefix, bucket, folder)
    for count in range(1, items + 1):
      self._PopulateBucket(test_uri, count)

  def _PopulateBucket(self, object_uri, object_id):
    """Populate a bucket with dummy files.

    Args:
      object_uri: URI for the folder
      object_id: the unique id for the new object
    """
    prefix = 'gsutilTest-{0}-'.format(object_id)
    upload_file = tempfile.NamedTemporaryFile(suffix='.txt',
                                              prefix=prefix,
                                              delete=False)
    try:
      upload_file.write('test file {0}'.format(object_id))
      upload_file.close()
      file_uri = upload_file.name
      full_gs_uri = '{0}/{1}'.format(object_uri,
                                     os.path.basename(upload_file.name))
      self._gsu.Copy(file_uri, full_gs_uri, False)
    finally:
      os.remove(upload_file.name)

if __name__ == '__main__':
  unittest.main()

