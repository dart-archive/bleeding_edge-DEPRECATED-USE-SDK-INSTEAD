"""A class to wrapper zip files."""

import os
import platform
import shutil
import stat
import subprocess
import sys
import zipfile


class DuplicateFile(Exception):
  """Exception thrown when a duplicate file is found when adding a file."""
  def __init__(self, zip_path, zip_name):
    Exception.__init__(self)
    self.zip_path = zip_path
    self.zip_name = zip_name

  def __str__(self):
    return 'file {0} is already in {1}'.format(repr(self.zip_path),
                                               repr(self.zip_name))


class ZipUtil(object):
  """A class to use for altering zip files."""
  _zipfile_name = None
  _is_windows = False

  def __init__(self, zipfile_in, buildos, create_new=False):
    """initialize the zip utilities class.

    Args:
      zipfile_in: the zip file to alter
      buildos: the os the build is running under
    Raises:
      Exception: if the files does not exist or it is not a zip file
    """
    if create_new:
      if not os.path.exists(zipfile_in):
        localzip = zipfile.ZipFile(zipfile_in, 'w')
        localzip.writestr("readme.txt", "empty zip file")
        localzip.close()
      else:
        raise Exception('zip file {0} exists'.format(zipfile_in))
    else:
      if not os.path.exists(zipfile_in):
        raise Exception('zip file {0} does not exist'.format(zipfile_in))
    if not zipfile.is_zipfile(zipfile_in):
      raise Exception('{0} is not a zip file'.format(zipfile_in))
    self._zipfile_name = os.path.abspath(zipfile_in)
    self._is_windows = buildos.find('win') >= 0

  def AddFile(self, new_file, zip_name, zip_in=None, mode_in='a',
              fail_on_duplicate=True):
    """Add a file to a zip.

    new file is the path to the file to add and the zip_name is the name
    of the file in the zip.
    Args:
      new_file: The new file to add to the zip.
      zip_name: the path to write this file at in the zip file.
      zip_in: a ZipFile that is already open
      mode_in: the mode to open the zip file with
      fail_on_duplicate: raise Exception if the file already exists in
                         the zip file.
    Raises:
      DuplicateFile: if a duplicate file exists in the zip file.
    """

    localzip = None
    try:
      #if no open ZIpFile is passed in open the zip file
      if zip_in is None:
        localzip = zipfile.ZipFile(self._zipfile_name, mode=mode_in)
      else:
        localzip = zip_in
      zip_name = zip_name.replace('\\\\', '/')
      zip_name = zip_name.replace('\\', '/')
      if fail_on_duplicate:
        try:
          localzip.getinfo(zip_name)
          raise DuplicateFile(zip_name, self._zipfile_name)
        except KeyError:
          pass  # an exception should be thrown if a new entry does not exist
      localzip.write(new_file, zip_name, zipfile.ZIP_DEFLATED)
    finally:
      #only close the file if it was opened in this method
      if localzip is not None and zip_in is None:
        localzip.close()

  def AddDirectoryTree(self, base_directory, write_path=None, mode_in='a',
                       fail_on_duplicate=True):
    """Add a Directory Tree to a Zip file.

    Args:
      base_directory: the directory to start the copy
      write_path: the path to replace in the zip entry
      mode_in: the mode to open the zip with
      fail_on_duplicate: fail the insertion if there the file
                          already exists.
    """
    localzip = None
    abs_dir = os.path.abspath(base_directory)

    try:
      count = 0
      print 'adding {0} to {1}'.format(base_directory, self._zipfile_name)
      localzip = zipfile.ZipFile(self._zipfile_name, mode=mode_in)
      for root, dirs, files in os.walk(abs_dir):
        for f in files:
          full_file = os.path.join(root, f)
          rel_file = full_file[len(abs_dir) + 1:]
          if write_path is not None:
            rel_file = os.path.join(write_path, rel_file)
          count += 1
          if not count % 100:
            print '{0} - '.format(count),
          self.AddFile(full_file, rel_file, localzip, mode_in,
                       fail_on_duplicate)
    finally:
      if localzip is not None:
        localzip.close()
    print 'done'

  def UnZip(self, destination, show_output=False):
    """Unzip into the destination.

    this command will use the os level zip commend to the work
    Args:
      destination: the destination to unzip a file into
      show_output: True display the output of the unzip command False
                    do not display it
    Returns:
      True if the unzip command succeeds false if not
    Raises:
      Exception: if destination is not a directory and does not exist
    """
    status = True
    if os.path.exists(destination) and os.path.isdir(destination):
      cwd = os.getcwd()
      try:
        os.chdir(destination)
        #on windows there are no file permissions so the python unzip code
        #will work
        if self._is_windows:
          self._PythonUnzip()
        else:
          #on Linux and Mac use the system unzip to save the permissions
          cmd = ['unzip', self._zipfile_name]
          print ' '.join(cmd)
          p = subprocess.Popen(cmd, stdout=subprocess.PIPE,
                               stderr=subprocess.PIPE)
          (stdout, stderr) = p.communicate()
          if p.returncode:
            print 'unzip {0} failed with {1}'.format(self._zipfile_name,
                                                     p.returncode)
            print str(stderr)
            show_output = True
            status = False
          if show_output:
            print str(stdout)

      finally:
        os.chdir(cwd)
    else:
      if os.path.exists(destination):
        msg = '{0} does not exist'.format(destination)
        print msg
      else:
        msg = '{0} is not a directory'.format(destination)
        print msg
      raise Exception(msg)
    return status

  def _PythonUnzip(self):
    """Unzip a file using the python zipfile module."""
    ziplocal = None
    try:
      ziplocal = zipfile.ZipFile(self._zipfile_name)
      ziplocal.extractall()
    finally:
      if ziplocal is not None:
        ziplocal.close()


def main():
  """Main method for testing.  This code will be moved to a test class soon."""
  user_rwx = stat.S_IXUSR | stat.S_IRUSR | stat.S_IWUSR
  group_rx = stat.S_IRGRP | stat.S_IXGRP
  other_rx = stat.S_IXOTH | stat.S_IROTH
  test_file = 'test.txt'
  test_file_2 = 'test2.txt'
  zip_file_name = 'test.zip'
  pos = platform.system()
  if pos == 'Linux':
    testos = 'linux'
  elif pos == 'Darwin':
    testos = 'macos'
  elif pos == 'Windows' or id == 'Microsoft':
    testos = 'win'

  fout = open(test_file, 'w')
  fout.write('this is a test')
  fout.close()

  fout = open(test_file_2, 'w')
  fout.write('this is a test also')
  fout.close()
  os.chmod(test_file_2, user_rwx | group_rx | other_rx)

  print '*' * 40

  zout = zipfile.ZipFile(zip_file_name, 'w')
  zout.write(test_file)
  zout.close()

  print'*' * 40

  if zipfile.is_zipfile(zip_file_name):
    print 'good zip file'
  else:
    print 'bad zip file'
    return

  ztest = zipfile.ZipFile(zip_file_name)

  print '*' * 40

  ztest.printdir()

  print '*' * 40

  for info in ztest.infolist():
    fname = info.filename
    data = ztest.read(fname)
    imode = info.internal_attr
    emode = info.external_attr
    print '{0} int: {1} ext: {2}'.format(fname, imode, emode)
    if fname.endswith('.txt'):
      print 'the data in {0} is:'.format(fname)
      print data
  ztest.close()
  print '*' * 40

  myzip = ZipUtil(zip_file_name, testos)

  myzip.AddFile(test_file_2, os.path.join('me', 'you', test_file_2))
  try:
    myzip.AddFile(test_file_2, os.path.join('me', 'you', test_file_2),
                  fail_on_duplicate=True)
    raise Exception('should fail to add a duplicate')
  except DuplicateFile:
    pass

  print '*' * 40
  myzip.AddDirectoryTree(os.path.join('..', 'docs'),
                         os.path.join('temp', 'docs'))
  ztest = zipfile.ZipFile(zip_file_name)
  ztest.printdir()
  ztest.close()

  print '*' * 40

  tmp_unzip_dir = '/tmp/testZip'
  if os.path.exists(tmp_unzip_dir):
    shutil.rmtree(tmp_unzip_dir)
  os.makedirs(tmp_unzip_dir)
  myzip.UnZip(tmp_unzip_dir, True)

  if testos.find('win') < 0:
    unzipped_test2 = os.path.join(tmp_unzip_dir, 'me', 'you', test_file_2)
    st = os.stat(unzipped_test2)
    if (st.st_mode & user_rwx == user_rwx and
        st.st_mode & group_rx == group_rx and
        st.st_mode & other_rx == other_rx):
      print 'good'
    else:
      print 'permissions not set correctly'


#  os.remove(zip_file_name)
  os.remove(test_file)
  os.remove(test_file_2)


if __name__ == '__main__':
  sys.exit(main())
