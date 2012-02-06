import os
import sys
import zipfile
import stat
import subprocess
import shutil

class ZipUtil(object):
  """A class to use for altering zip files.
  """
  _zipfile_name = None

  def __init__(self, zipfile_in):
    """initialize the zip utilities class.
    Args:
      zipfile: the zip file to alter
    """
    if not os.path.exists(zipfile_in):
      raise Exception('zip file {0} does not exist'.format(zipfile_in))
    if not zipfile.is_zipfile(zipfile_in):
      raise Exception('{0} is not a zip file'.format(zipfile_in))
    self._zipfile_name = os.path.abspath(zipfile_in)

  def AddFile(self, new_file, zip_name, zip_in=None, mode_in='a'):
    """Add a file to a zip.
    new file is the path to the file to add and the zip_name is the name 
    of the file in the zip.
    Args:
      new_file: The new file to add to the zip.
      zip_name: the path to write this file at in the zip file.
      zip_in: a ZipFile that is already open
      mode_in: the mode to open the zip file with
    """

    localzip = None
    try:
      #if no open ZIpFile is passed in open the zip file
      if zip_in is None:
        localzip = zipfile.ZipFile(self._zipfile_name, mode=mode_in)
      else:
        localzip = zip_in
      localzip.write(new_file, zip_name)
      zip_info = localzip.getinfo(zip_name)
      print 'file {0} added to zip {1} '.format(zip_info.filename, self._zipfile_name)
    finally:
      #only close the file if it was opened in this method
      if localzip is not None and zip_in is None:
        localzip.close()

  def AddDirectoryTree(self, base_directory, write_path=None, mode_in='a'):
    """Add a Directory Tree to a Zip file.
    Args:
      base_directory: the directory to start the copy
      write_path: the path to replace in the zip entry
      mode_in: the mode to open the zip with
    """
    localzip = None
    abs_dir = os.path.abspath(base_directory)
    
    try:
      localzip = zipfile.ZipFile(self._zipfile_name, mode=mode_in)
      for root, dirs, files in os.walk(abs_dir):
        for f in files:
          full_file = os.path.join(root, f)
          print "full_file = {0}".format(full_file)
          rel_file = full_file[len(abs_dir) + 1:]
          print "rel_file  = {0}".format(rel_file)
          if write_path is not None:
            rel_file = os.path.join(write_path, rel_file)
          self.AddFile(full_file, rel_file, localzip, mode_in)
    finally:
      if localzip is not None:
        localzip.close()

  def UnZip(self, destination, show_output=False):
    """Unzip into the destination.
    this command will use the os level zip commend to the work
    Args:
      destination: the destination to unzip a file into
    Returns:
      True if the unzip command succeedsfalse if not
    """
    status = True
    if os.path.exists(destination) and os.path.isdir(destination):
      cwd = os.getcwd()
      try:
        os.chdir(destination)
        cmd = ['unzip', self._zipfile_name]
        print ' '.join(cmd)
        p = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
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

def main():
  """Main method for testing.  This code will be moved to a test class soon.
  """
  user_rwx = stat.S_IXUSR | stat.S_IRUSR | stat.S_IWUSR
  group_rx = stat.S_IRGRP | stat.S_IXGRP
  other_rx = stat.S_IXOTH | stat.S_IROTH
  test_file = 'test.txt'
  test_file_2 = 'test2.txt'
  zip_file_name = 'test.zip'

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
  
  myzip = ZipUtil(zip_file_name);
  myzip.AddFile(test_file_2, 'me/you/' + test_file_2)
  
  print '*' * 40
  myzip.AddDriectoryTree('../docs', 'temp/docs')
  ztest = zipfile.ZipFile(zip_file_name)
  ztest.printdir()
  ztest.close()
  
  print '*' * 40
  
  tmp_unzip_dir = '/tmp/testZip'
  if os.path.exists(tmp_unzip_dir):
    shutil.rmtree(tmp_unzip_dir)
  os.makedirs('/tmp/testZip')
  myzip.UnZip('/tmp/testZip', True)
  
#  os.remove(zip_file_name)
  os.remove(test_file)
  os.remove(test_file_2)
  
  
if __name__ == '__main__':
  sys.exit(main())
  
