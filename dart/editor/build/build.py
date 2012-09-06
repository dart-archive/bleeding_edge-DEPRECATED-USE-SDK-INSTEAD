#!/usr/bin/env python
#
# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import glob
import optparse
import os
import re
import shutil
import string
import subprocess
import sys
import tempfile
import gsutil
import ziputils
import hashlib

from os.path import join, basename

BUILD_OS = None
DART_PATH = None
TOOLS_PATH = None
GSU_PATH_REV = None
GSU_API_DOCS_PATH = None
GSU_API_DOCS_BUCKET = 'gs://dartlang-api-docs'
GSU_PATH_LATEST = None
REVISION = None

utils = None

class AntWrapper(object):
  """A wrapper for ant build invocations"""

  _antpath = None
  _bzippath = None
  _propertyfile = None

  def __init__(self, propertyfile, antpath='/usr/bin', bzippath=None):
    """Initialize the ant path.

    Args:
      propertyfile: the file to write the build properties to
      antpath: the path to ant
      bzippath: the path to the bzip jar
    """
    self._antpath = antpath
    self._bzippath = bzippath
    self._propertyfile = propertyfile

  def RunAnt(self, build_dir, antfile, revision, name,
             buildroot, buildout, sourcepath, buildos,
             extra_args=None, sdk_zip=None, running_on_bot=False,
             extra_artifacts=None):
    """Run the given Ant script from the given directory.

    Args:
      build_dir: the directory to run the ant script from
      antfile: the ant file to run
      revision: the SVN revision of this build
      name: the name of the builder
      buildroot: root of the build source tree
      buildout: the location to copy output
      sourcepath: the path to the root of the source
      buildos: the operating system this build is running under (may be null)
      extra_args: any extra args to ant
      sdk_zip: the place to write the sdk zip file
      running_on_bot: True if running on buildbot False otherwise
      extra_artifacts: the directory where extra artifacts will be deposited

    Returns:
      returns the status of the ant call

    Raises:
      Exception: if a shell can not be found
    """
    os_shell = '/bin/bash'
    ant_exec = 'ant'
    is_windows = False
    if not os.path.exists(os_shell):
      os_shell = os.environ['COMSPEC']
      if os_shell is None:
        raise Exception('could not find shell')
      else:
        ant_exec = 'ant.bat'
        is_windows = True

    cwd = os.getcwd()
    os.chdir(build_dir)
    print 'cwd = {0}'.format(os.getcwd())
    print 'ant path = {0}'.format(self._antpath)
    # run the ant file given
    local_env = os.environ
    args = []
    if not is_windows:
      args.append(os_shell)
    args.append(os.path.join(self._antpath, ant_exec))
    args.append('-lib')
    args.append(os.path.join(self._bzippath, 'bzip2.jar'))
    args.append('-noinput')
    args.append('-nouserlib')
    if antfile:
      args.append('-f')
      args.append(antfile)
    if revision:
      args.append('-Dbuild.revision=' + revision)
    if name:
      args.append('-Dbuild.builder=' + name)
    if buildroot:
      args.append('-Dbuild.root=' + buildroot)
    if buildout:
      args.append('-Dbuild.out=' + buildout)
    if sourcepath:
      args.append('-Dbuild.source=' + sourcepath)
    if self._propertyfile:
      args.append('-Dbuild.out.property.file=' + self._propertyfile)
    if buildos:
      args.append('-Dbuild.os={0}'.format(buildos))
    if running_on_bot:
      args.append('-Dbuild.running.headless=true')
    if sdk_zip:
      args.append('-Dbuild.dart.sdk.zip={0}'.format(sdk_zip))
    if extra_artifacts:
      args.append('-Dbuild.extra.artifacts={0}'.format(extra_artifacts))
    if is_windows:
      args.append('-autoproxy')
    if extra_args:
      args.extend(extra_args)
    args.append('-Dbuild.local.build=false')

    extra_args = os.environ.get('ANT_EXTRA_ARGS')
    if extra_args is not None:
      parsed_extra = extra_args.split()
      for arg in parsed_extra:
        args.append(arg)

    print ' '.join(args)
    status = subprocess.call(args, shell=is_windows)
    os.chdir(cwd)
    return status


def BuildOptions():
  """Setup the argument processing for this program."""
  result = optparse.OptionParser()
  result.set_default('dest', 'gs://dart-editor-archive-continuous')
  result.add_option('-m', '--mode',
                    help='Build variants (comma-separated).',
                    metavar='[all,debug,release]',
                    default='debug')
  result.add_option('-v', '--verbose',
                    help='Verbose output.',
                    default=False, action='store')
  result.add_option('-r', '--revision',
                    help='SVN Revision.',
                    action='store')
  result.add_option('-n', '--name',
                    help='builder name.',
                    action='store')
  result.add_option('-o', '--out',
                    help='Output Directory.',
                    action='store')
  result.add_option('--dest',
                    help='Output Directory.',
                    action='store')
  return result


def GetUtils(toolspath):
  """Dynamically get the utils module.

  We use a dynamic import for tools/util.py because we derive its location
  dynamically using sys.argv[0]. This allows us to run this script from
  different directories.

  Args:
    toolspath: the path to the tools directory

  Returns:
    the utils module
  """
  sys.path.append(os.path.abspath(toolspath))
  utils = __import__('utils')
  return utils


def main():
  """Main entry point for the build program."""
  global BUILD_OS
  global DART_PATH
  global TOOLS_PATH
  global GSU_PATH_REV
  global GSU_API_DOCS_PATH
  global GSU_PATH_LATEST
  global REVISION
  global utils
  
  if not sys.argv:
    print 'Script pathname not known, giving up.'
    return 1

  scriptdir = os.path.abspath(os.path.dirname(sys.argv[0]))
  editorpath = os.path.abspath(os.path.join(scriptdir, '..'))
  thirdpartypath = os.path.abspath(os.path.join(scriptdir, '..', '..',
                                                'third_party'))
  toolspath = os.path.abspath(os.path.join(scriptdir, '..', '..',
                                           'tools'))
  dartpath = os.path.abspath(os.path.join(scriptdir, '..', '..'))
  antpath = os.path.join(thirdpartypath, 'apache_ant', 'v1_7_1')
  bzip2libpath = os.path.join(thirdpartypath, 'bzip2')
  buildpath = os.path.join(editorpath, 'tools', 'features',
                           'com.google.dart.tools.deploy.feature_releng')
  utils = GetUtils(toolspath)
  buildos = utils.GuessOS()

  BUILD_OS = utils.GuessOS()
  DART_PATH = dartpath
  TOOLS_PATH = toolspath
  
  # TODO(devoncarew): remove this hardcoded e:\ path
  buildroot_parent = {'linux': dartpath, 'macos': dartpath, 'win32': r'e:\tmp'}
  buildroot = os.path.join(buildroot_parent[buildos], 'build_root')

  os.chdir(buildpath)
  ant_property_file = None
  sdk_zip = None
  
  try:
    ant_property_file = tempfile.NamedTemporaryFile(suffix='.property',
                                                    prefix='AntProperties',
                                                    delete=False)
    ant_property_file.close()
    extra_artifacts = tempfile.mkdtemp(prefix='ExtraArtifacts')
    ant = AntWrapper(ant_property_file.name, os.path.join(antpath, 'bin'),
                     bzip2libpath)

    ant.RunAnt(os.getcwd(), '', '', '', '',
               '', '', buildos, ['-diagnostics'])

    parser = BuildOptions()
    (options, args) = parser.parse_args()
    # Determine which targets to build. By default we build the "all" target.
    if args:
      print 'only options should be passed to this script'
      parser.print_help()
      return 2

    if str(options.revision) == 'None':
      print 'missing revision option'
      parser.print_help()
      return 3

    if str(options.name) == 'None':
      print 'missing builder name'
      parser.print_help()
      return 4

    if str(options.out) == 'None':
      print 'missing output directory'
      parser.print_help()
      return 5

    print 'buildos        = {0}'.format(buildos)
    print 'scriptdir      = {0}'.format(scriptdir)
    print 'editorpath     = {0}'.format(editorpath)
    print 'thirdpartypath = {0}'.format(thirdpartypath)
    print 'toolspath      = {0}'.format(toolspath)
    print 'antpath        = {0}'.format(antpath)
    print 'bzip2libpath   = {0}'.format(bzip2libpath)
    print 'buildpath      = {0}'.format(buildpath)
    print 'buildroot      = {0}'.format(buildroot)
    print 'dartpath       = {0}'.format(dartpath)
    print 'revision(in)   = |{0}|'.format(options.revision)
    #this code handles getting the revision on the developer machine
    #where it can be 123, 123M 123:125M
    print 'revision(in)   = {0}|'.format(options.revision)
    revision = options.revision.rstrip()
    lastc = revision[-1]
    if lastc.isalpha():
      revision = revision[0:-1]
    index = revision.find(':')
    if index > -1:
      revision = revision[0:index]
    print 'revision       = |{0}|'.format(revision)
    buildout = os.path.abspath(options.out)
    print 'buildout       = {0}'.format(buildout)

    if not os.path.exists(buildout):
      os.makedirs(buildout)
    
    # clean out old build artifacts
    for f in os.listdir(buildout):
      if ('dartsdk-' in f) or ('darteditor-' in f) or ('dart-editor-' in f):
        os.remove(join(buildout, f))

    #get user name if it does not start with chrome then deploy
    # to the test bucket otherwise deploy to the continuous bucket
    #I could not find any non-OS specific way to get the user under Python
    # so the environemnt variables 'USER' Linux and Mac and
    # 'USERNAME' Windows were used.
    username = os.environ.get('USER')
    if username is None:
      username = os.environ.get('USERNAME')

    if username is None:
      PrintError('Could not find username')
      return 6
    
    build_skip_tests = os.environ.get('DART_SKIP_RUNNING_TESTS')
    sdk_environment = os.environ
    if username.startswith('chrome'):
      to_bucket = 'gs://dart-editor-archive-continuous'
      running_on_buildbot = True
    else:
      to_bucket = 'gs://dart-editor-archive-testing'
      running_on_buildbot = False
      sdk_environment['DART_LOCAL_BUILD'] = 'dart-editor-archive-testing'

    REVISION = options.revision
    GSU_PATH_REV = '%s/%s' % (to_bucket, options.revision)
    GSU_PATH_LATEST = '%s/%s' % (to_bucket, 'latest')
    GSU_API_DOCS_PATH = '%s/%s' % (GSU_API_DOCS_BUCKET, options.revision)

    homegsutil = join(DART_PATH, 'third_party', 'gsutil', 'gsutil')
    gsu = gsutil.GsUtil(False, homegsutil, running_on_buildbot=running_on_buildbot)

    print '@@@BUILD_STEP dart-ide dart clients: %s@@@' % options.name
    if sdk_environment.has_key('JAVA_HOME'):
      print 'JAVA_HOME = {0}'.format(str(sdk_environment['JAVA_HOME']))
    builder_name = str(options.name)

    if (builder_name != 'dart-editor'):
      PrintSeparator('running the build of the Dart SDK')
      
      EnsureDirectoryExists(buildout)
      sdk_zip = CreateSDK(buildout)

    if (BUILD_OS == 'linux' and builder_name != 'dart-editor'):
      CreateApiDocs(buildout)

    if builder_name == 'dart-editor':
      BuildUpdateSite(gsu, ant, revision, options.name, buildroot, buildout,
              editorpath, buildos)
      return 0

    PrintSeparator('running the build to produce the Zipped RCP''s')
    #tell the ant script where to write the sdk zip file so it can
    #be expanded later
    status = ant.RunAnt('.', 'build_rcp.xml', revision, options.name,
                        buildroot, buildout, editorpath, buildos,
                        sdk_zip=sdk_zip,
                        running_on_bot=running_on_buildbot,
                        extra_artifacts=extra_artifacts)
    #the ant script writes a property file in a known location so
    #we can read it.
    properties = ReadPropertyFile(buildos, ant_property_file.name)

    if not properties:
      raise Exception('no data was found in file {%s}' % ant_property_file.name)
    if status:
      if properties['build.runtime']:
        PrintErrorLog(properties['build.runtime'])
      return status

    #For the dart-editor build, return at this point.
    #We don't need to install the sdk+dartium, run tests, or copy to google
    #storage.
    if not buildos:
      print 'skipping sdk and dartium steps for dart-editor build'
      return 0

    #This is an override for local testing
    force_run_install = os.environ.get('FORCE_RUN_INSTALL')

    if (force_run_install or (builder_name != 'dart-editor')):
      InstallSdk(buildroot, buildout, buildos, buildout)
      InstallDartium(buildroot, buildout, buildos, gsu)

    if status:
      return status

    if not build_skip_tests:
      PrintSeparator('Running the tests')
      junit_status = ant.RunAnt('../com.google.dart.tools.tests.feature_releng',
                              'buildTests.xml',
                              revision, options.name, buildroot, buildout,
                              editorpath, buildos,
                              extra_artifacts=extra_artifacts)
      properties = ReadPropertyFile(buildos, ant_property_file.name)
      if buildos:
        UploadTestHtml(buildout, to_bucket, revision, buildos, gsu)
      if junit_status:
        if properties['build.runtime']:
          #if there is a build.runtime and the status is not
          #zero see if there are any *.log entries
          PrintErrorLog(properties['build.runtime'])

        #
        # Uncomment this section to prevent tests from failing the build
        #        
        # PrintSeparator('JUnit test failures')
        # print('https://gsdview.appspot.com/dart-editor-archive-continuous/%s/tests/%s/index.html'
        #       % (revision, buildos))
        # 
        # junit_status = 0;
    else:
      junit_status = 0

    if buildos:
      _InstallArtifacts(buildout, buildos, extra_artifacts)
      
      # dart-editor-linux.gtk.x86.zip --> darteditor-linux-32.zip
      RenameRcpZipFiles(buildout);
      
      PostProcessEditorBuilds(buildout)
      
      version_file = _FindVersionFile(buildout)
      if version_file:
        UploadFile(version_file, False)

      found_zips = _FindRcpZipFiles(buildout)
      for zipfile in found_zips:
        UploadFile(zipfile)
        
    return junit_status
  finally:
    if ant_property_file is not None:
      print 'cleaning up temp file {0}'.format(ant_property_file.name)
      os.remove(ant_property_file.name)
    if extra_artifacts:
      print 'cleaning up temp dir {0}'.format(extra_artifacts)
      shutil.rmtree(extra_artifacts)
    print 'cleaning up {0}'.format(buildroot)
    shutil.rmtree(buildroot, True)
    print 'Build Done'


def ReadPropertyFile(buildos, property_file):
  """Read a property file and return a dictionary of key/value pares.

  Args:
    buildos: the os the build is running under
    property_file: the file to read

  Returns:
    the dictionary of Ant properties
  """
  properties = {}
  print 'processing file ' + property_file
  for line in open(property_file):
    #ignore comments
    if not line.startswith('#'):
      parts = line.split('=')

      key = str(parts[0]).strip()
      value = str(parts[1]).strip()
      #the property file is written from java so all of the \ are escaped
      #this will clean up the code
      # e.g. build.out = c\:\\Users\\testing\\dart-all/dart will be read into
      #      python as build.out = c\\:\\\\Users\\\\testing\\\\dart-all/dart
      # this code will convert the above to:
      #      c:/Users/testing/dart-all/dart
      # os.path.normpath will convert the path to the appropriate os path
      if buildos is not None and buildos.find('win'):
        value = value.replace(r'\:', ':')
        value = value.replace(r'\\', '/')
      properties[key] = value

  return properties


def PrintErrorLog(rootdir):
  """Print an eclipse error log if one is found.

  Args:
    rootdir: the directory to start from
  """
  print 'search ' + rootdir + ' for error logs'
  found = False
  configdir = os.path.join(rootdir, 'eclipse', 'configuration')
  if os.path.exists(configdir):
    for logfile in glob.glob(os.path.join(configdir, '*.log')):
      print 'Found log file: ' + logfile
      found = True
      for logline in open(logfile):
        print logline
  if not found:
    print 'no log file was found in ' + configdir


def UploadTestHtml(buildout, bucket, svnid, buildos, gsu):
  """Upload the Test Results HTML to GoogleStorage.

  Args:
    buildout: the location ofthe output of the build
    bucket: the Google Storage bucket the code is staged in
    svnid: the revision id for this build
    buildos: the os the build is running on
    gsu: the gsutil object
  """
  print 'UploadTestHtml({0}, {1}, {2}, {3}, gsu)'.format(buildout,
                                                          bucket,
                                                          svnid,
                                                          buildos)
  gs_dir = '{0}/{1}'.format(bucket, svnid)
  local_dir = '{0}'.format(svnid)
  html_dir = os.path.join(buildout, 'html')
  cwd = os.getcwd()
  gs_test_dir_name = 'tests'
  tmp_dir = None
  try:
    if os.path.exists(html_dir):
      tmp_dir = tempfile.mkdtemp(prefix=gs_test_dir_name)
      local_path = os.path.join(tmp_dir, local_dir)
      os.makedirs(local_path)
      os.chdir(tmp_dir)
      shutil.copytree(html_dir, os.path.join(local_path, gs_test_dir_name,
                                             buildos))
      gsu.Copy(svnid, bucket, recursive_flag=True)
      gs_elements = gsu.ReadBucket('{0}/{1}/{2}/*'.format(gs_dir,
                                                          gs_test_dir_name,
                                                          buildos))
  finally:
    os.chdir(cwd)
    if tmp_dir is not None and os.path.exists(tmp_dir):
      shutil.rmtree(tmp_dir, ignore_errors=True)


def _FindRcpZipFiles(out_dir):
  """Find the Zipped RCP files.

  Args:
    out_dir: the directory the files will be located in

  Returns:
    a collection of rcp zip files
  """
  out_dir = os.path.normpath(os.path.normcase(out_dir))
  rcp_out_dir = os.listdir(out_dir)
  found_zips = []
  for element in rcp_out_dir:
    if (element.startswith('dart-editor')
         or element.startswith('darteditor-')) and element.endswith('.zip'):
      found_zips.append(os.path.join(out_dir, element))
  return found_zips

def _FindVersionFile(out_dir):
  """Find the build version file.

  Args:
    out_dir: the directory to search

  Returns:
    the build version file (or None if none was found)
  """
  out_dir = os.path.normpath(os.path.normcase(out_dir))
  print '_FindVersionFile({0})'.format(out_dir)

  version_file = os.path.join(out_dir, 'VERSION');
  return version_file if os.path.exists(version_file) else None

def InstallSdk(buildroot, buildout, buildos, sdk_dir):
  """Install the SDK into the RCP zip files.

  Args:
    buildroot: the boot of the build output
    buildout: the location of the ant build output
    buildos: the OS the build is running under
    sdk_dir: the directory containing the built SDKs
  """
  print 'InstallSdk(%s, %s, %s, %s)' % (buildroot, buildout, buildos, sdk_dir)
  
  tmp_dir = os.path.join(buildroot, 'tmp')
  
  unzip_dir_32 = os.path.join(tmp_dir, 'unzip_sdk_32')
  if not os.path.exists(unzip_dir_32):
    os.makedirs(unzip_dir_32)

  unzip_dir_64 = os.path.join(tmp_dir, 'unzip_sdk_64')
  if not os.path.exists(unzip_dir_64):
    os.makedirs(unzip_dir_64)
  
  sdk_zip = ziputils.ZipUtil(join(sdk_dir, "dartsdk-%s-32.zip" % buildos), buildos)
  sdk_zip.UnZip(unzip_dir_32)
  sdk_zip = ziputils.ZipUtil(join(sdk_dir, "dartsdk-%s-64.zip" % buildos), buildos)
  sdk_zip.UnZip(unzip_dir_64)
  
  files = _FindRcpZipFiles(buildout)
  for f in files:
    dart_zip_path = os.path.join(buildout, f)
    dart_zip = ziputils.ZipUtil(dart_zip_path, buildos)
    # dart-editor-macosx.cocoa.x86_64.zip
    if '_64.zip' in f:
      dart_zip.AddDirectoryTree(unzip_dir_64, 'dart')
    else:  
      dart_zip.AddDirectoryTree(unzip_dir_32, 'dart')
      

def InstallDartium(buildroot, buildout, buildos, gsu):
  """Install Dartium into the RCP zip files.

  Args:
    buildroot: the boot of the build output
    buildout: the location of the ant build output
    buildos: the OS the build is running under
    gsu: the gsutil wrapper
  Raises:
    Exception: if no dartium files can be found
  """
  print 'InstallDartium(%s, %s, %s)' % (buildroot, buildout, buildos)

  tmp_dir = os.path.join(buildroot, 'tmp')
  
  rcpZipFiles = _FindRcpZipFiles(buildout)
  
  for rcpZipFile in rcpZipFiles:
    print '  found rcp: %s' % rcpZipFile
    
  # dartium-lucid32-full-9420.9420.zip
  # dartium-lucid64-full-9420.9420.zip
  # dartium-mac-full-9420.9420.zip
  # dartium-win-full-9420.9420.zip
  # exclude dartium-lucid64-full-trunk-9571.9571.zip
  dartiumFiles = gsu.ReadBucket('gs://dartium-archive/latest/dartium-*-full-[0-9]*.zip')

  if not dartiumFiles:
    raise Exception("could not find any dartium files")

  tempList = []
  
  for dartiumFile in dartiumFiles:
    print '  found dartium: %s' % dartiumFile
    tempList.append(RemapDartiumUrl(dartiumFile))

  dartiumFiles = tempList

  
  for rcpZipFile in rcpZipFiles:
    searchString = None;
    
    # dart-editor-linux.gtk.x86.zip
    # dart-editor-linux.gtk.x86_64.zip
    # dart-editor-macosx.cocoa.x86.zip
    # dart-editor-macosx.cocoa.x86_64.zip
    # dart-editor-win32.win32.x86.zip
    # dart-editor-win32.win32.x86_64.zip

    if '-linux.gtk.x86.zip' in rcpZipFile:
      searchString = 'dartium-lucid32-full-';
    if '-linux.gtk.x86_64.zip' in rcpZipFile:
      searchString = 'dartium-lucid64-full-';
    if 'macosx' in rcpZipFile:
      searchString = 'dartium-mac-full-';
    if 'win32' in rcpZipFile:
      searchString = 'dartium-win-full-';
    
    for dartiumFile in dartiumFiles:
      if searchString in dartiumFile:
        #download and unzip dartium
        unzip_dir = os.path.join(tmp_dir, os.path.splitext(os.path.basename(dartiumFile))[0])
        if not os.path.exists(unzip_dir):
          os.makedirs(unzip_dir)
        tmp_zip_file = os.path.join(tmp_dir, os.path.basename(dartiumFile))
        
        if not os.path.exists(tmp_zip_file):
          gsu.Copy(dartiumFile, tmp_zip_file, False)
          
          # Dartium is unzipped into something like unzip_dir/dartium-win-inc-7665.7665
          dartium_zip = ziputils.ZipUtil(tmp_zip_file, buildos)
          dartium_zip.UnZip(unzip_dir)
        else:
          dartium_zip = ziputils.ZipUtil(tmp_zip_file, buildos)

        add_path = None
  
        if 'lin' in buildos:
          paths = glob.glob(os.path.join(unzip_dir, 'dartium-*'))
          add_path = paths[0]
          zip_rel_path = 'dart/chromium'
        if 'win' in buildos:
          paths = glob.glob(os.path.join(unzip_dir, 'dartium-*'))
          add_path = paths[0]
          zip_rel_path = 'dart/chromium'
          # remove extra files
          FileDelete(os.path.join(add_path, 'DumpRenderTree.exe'))
          FileDelete(os.path.join(add_path, 'mini_installer.exe'))
          FileDelete(os.path.join(add_path, 'sync_unit_tests.exe'))
        if 'mac' in buildos:
          paths = glob.glob(os.path.join(unzip_dir, 'dartium-*'))
          add_path = os.path.join(paths[0], 'Chromium.app')
          zip_rel_path = 'dart/Chromium.app'
        
        #add to the rcp zip
        dart_zip_path = os.path.join(buildout, rcpZipFile)
        dart_zip = ziputils.ZipUtil(dart_zip_path, buildos)
        dart_zip.AddDirectoryTree(add_path, zip_rel_path)
        
  shutil.rmtree(tmp_dir, True)


# convert:
#   gs://dartium-archive/latest/dartium-lucid32-full-9420.9420.zip
# to:
#   gs://dartium-archive/dartium-lucid32-full/dartium-lucid32-full-9420.9420.zip  
def RemapDartiumUrl(url):
  name = basename(url)

  reResult = re.search('(\S*-\S*-full)-.*', name)

  directory = reResult.group(1)
  
  remap = "gs://dartium-archive/%s/%s" % (directory, name)
  
  print "    %s ==> %s" % (url, remap)
  
  return remap
  

def _InstallArtifacts(buildout, buildos, extra_artifacts):
  """Install extra build artifacts into the RCP zip files.

  Args:
    buildout: the location of the ant build output
    buildos: the OS the build is running under
    extra_artifacts: the directory containing the extra artifacts
  """
  print '_InstallArtifacts({%s}, {%s}, {%s})' % (buildout, buildos, extra_artifacts)
  files = _FindRcpZipFiles(buildout)
  for f in files:
    dart_zip_path = os.path.join(buildout, f)
    dart_zip = ziputils.ZipUtil(dart_zip_path, buildos)
    dart_zip.AddDirectoryTree(extra_artifacts, 'dart')


def RenameRcpZipFiles(out_dir):
  """Rename the RCP output files to be more consistent with the Dart SDK names"""
  renameMap = {
    "dart-editor-linux.gtk.x86.zip"       : "darteditor-linux-32.zip",
    "dart-editor-linux.gtk.x86_64.zip"    : "darteditor-linux-64.zip",
    "dart-editor-macosx.cocoa.x86.zip"    : "darteditor-macos-32.zip",
    "dart-editor-macosx.cocoa.x86_64.zip" : "darteditor-macos-64.zip",
    "dart-editor-win32.win32.x86.zip"     : "darteditor-win32-32.zip",
    "dart-editor-win32.win32.x86_64.zip"  : "darteditor-win32-64.zip",
  }
  
  for zipFile in _FindRcpZipFiles(out_dir):
    basename = os.path.basename(zipFile)
    if renameMap[basename] != None:
      os.rename(zipFile, join(os.path.dirname(zipFile), renameMap[basename]))


def PostProcessEditorBuilds(out_dir):
  """Post-process the created RCP builds"""
  
  PrintSeparator('Post-process RCP builds')
  
  for zipFile in _FindRcpZipFiles(out_dir):
    basename = os.path.basename(zipFile)
    
    print('  processing %s' % basename)
    
    # post-process the info.plist file
    if (basename.startswith('darteditor-macos-')):
      infofile = join('dart', 'DartEditor.app', 'Contents', 'Info.plist')
      subprocess.call(['unzip', zipFile, infofile], env=os.environ)
      ReplaceInFiles([infofile],
                     [('<dict>',
                       '<dict>\n\t<key>NSHighResolutionCapable</key>\n\t\t<true/>')])
      subprocess.call(['zip', '-q', zipFile, infofile], env=os.environ)
      os.remove(infofile)
      
      
def CalculateChecksum(filename):
  """Calculate the MD5 checksum for filename."""

  md5 = hashlib.md5()
  
  with open(filename, 'rb') as file:
    data = file.read(65536)
    while len(data) > 0:
      md5.update(data)
      data = file.read(65536)
      
  return md5.hexdigest()


def CreateChecksumFile(filename):
  """Create and upload an MD5 checksum file for filename."""

  checksum = CalculateChecksum(filename)
  checksum_filename = '%s.md5sum' % filename
  
  with open(checksum_filename, 'w') as file:
    file.write('%s *%s' % (checksum, os.path.basename(filename)))
    
  return checksum_filename


def ReplaceInFiles(paths, subs):
  '''Reads a series of files, applies a series of substitutions to each, and
     saves them back out. subs should by a list of (pattern, replace) tuples.'''
  for path in paths:
    contents = open(path).read()
    for pattern, replace in subs:
      contents = re.sub(pattern, replace, contents)
    dest = open(path, 'w')
    dest.write(contents)
    dest.close()


def ExecuteCommand(cmd, dir=None):
  """Execute the given command."""
  if dir is not None:
    cwd = os.getcwd()
    os.chdir(dir)
  status = subprocess.call(cmd, env=os.environ)
  if dir is not None:
    os.chdir(cwd)
  return status


def BuildUpdateSite(gsu, ant, revision, name, buildroot, buildout,
              editorpath, buildos):
  ant.RunAnt('../com.google.dart.eclipse.feature_releng',
             'build.xml', revision, name, buildroot, buildout,
              editorpath, buildos, ['-Dbuild.dir=%s' % buildout])
  #TODO(pquitslund): migrate to a bucket copy (rather than serial uploads)
  UploadSite(gsu, buildout, "%s/%s" % (GSU_PATH_REV, 'eclipse-update'))
  UploadSite(gsu, buildout, "%s/%s" % (GSU_PATH_LATEST, 'eclipse-update'))
  
  
def UploadSite(gsu, buildout, gsPath) :
  # remove any old artifacts
  Gsutil(['rm', '-R', join(gsPath, '*')])
  # create eclipse-update/index.html first to ensure eclipse-update prefix
  # exists (needed for recursive copy to follow)
  Gsutil(['cp', '-a', 'public-read', 
          r'file://' + join(buildout, 'buildRepo', 'index.html'),
          join(gsPath,'index.html')])
  # recursively copy update site contents
  UploadDirectory(glob.glob(join(buildout, 'buildRepo', '*')), gsPath)


def CreateApiDocs(buildLocation):
  """Zip up api_docs, upload it, and upload the raw tree of docs"""
  
  CallBuildScript('release', 'ia32', 'api_docs')
  
  apidir = join(DART_PATH, utils.GetBuildRoot(BUILD_OS, 'release', 'ia32'),
                'api_docs')

  UploadApiDocs(apidir)
  
  api_zip = join(buildLocation, 'dart-api-docs.zip')
  
  CreateZip(apidir, api_zip)

  # upload to continuous/svn_rev and to continuous/latest
  UploadFile(api_zip, False)
  
  
def CreateSDK(sdkpath):
  """Create the dart-sdk's for the current OS"""

  if BUILD_OS == 'linux':
    return CreateLinuxSDK(sdkpath)
  if BUILD_OS == 'macos':
    return CreateMacosSDK(sdkpath)
  if BUILD_OS == 'win32':
    return CreateWin32SDK(sdkpath)


def CreateLinuxSDK(sdkpath):
  sdkdir32 = join(DART_PATH, utils.GetBuildRoot('linux', 'release', 'ia32'),
                  'dart-sdk')
  sdkdir64 = join(DART_PATH, utils.GetBuildRoot('linux', 'release', 'x64'),
                  'dart-sdk')

  # Build the SDK. On Linux, building the 64 bit SDK runs into issues w/ trying
  # to build V8 in 64 bit. Sooo, we build the 32 bit SDK, build the 64 bit VM,
  # and synthesize a 64 bit SDK from the two.
  CallBuildScript('release', 'ia32', 'create_sdk')
  CallBuildScript('release', 'x64', 'runtime')
  shutil.rmtree(sdkdir64, True)
  shutil.copytree(sdkdir32, sdkdir64)
  shutil.copy(join(DART_PATH, utils.GetBuildRoot('linux', 'release', 'x64'), 'dart'),
              join(sdkdir64, 'bin'))
  
  sdk32_zip = join(sdkpath, 'dartsdk-linux-32.zip')
  sdk32_tgz = join(sdkpath, 'dartsdk-linux-32.tar.gz')
  sdk64_zip = join(sdkpath, 'dartsdk-linux-64.zip')
  sdk64_tgz = join(sdkpath, 'dartsdk-linux-64.tar.gz')

  CreateZip(sdkdir32, sdk32_zip)
  CreateTgz(sdkdir32, sdk32_tgz)
  CreateZip(sdkdir64, sdk64_zip)
  CreateTgz(sdkdir64, sdk64_tgz)

  UploadFile(sdk32_zip)
  UploadFile(sdk32_tgz)
  UploadFile(sdk64_zip)
  UploadFile(sdk64_tgz)

  return sdk32_zip


def CreateMacosSDK(sdkpath):
  # Build the SDK
  CallBuildScript('release', 'ia32,x64', 'create_sdk')

  sdk32_zip = join(sdkpath, 'dartsdk-macos-32.zip')
  sdk64_zip = join(sdkpath, 'dartsdk-macos-64.zip')
  sdk32_tgz = join(sdkpath, 'dartsdk-macos-32.tar.gz')
  sdk64_tgz = join(sdkpath, 'dartsdk-macos-64.tar.gz')

  CreateZip(join(DART_PATH, utils.GetBuildRoot('macos', 'release', 'ia32'),
                 'dart-sdk'), sdk32_zip)
  CreateZip(join(DART_PATH, utils.GetBuildRoot('macos', 'release', 'x64'),
                 'dart-sdk'), sdk64_zip)
  CreateTgz(join(DART_PATH, utils.GetBuildRoot('macos', 'release', 'ia32'),
                 'dart-sdk'), sdk32_tgz)
  CreateTgz(join(DART_PATH, utils.GetBuildRoot('macos', 'release', 'x64'),
                 'dart-sdk'), sdk64_tgz)

  UploadFile(sdk32_zip)
  UploadFile(sdk64_zip)
  UploadFile(sdk32_tgz)
  UploadFile(sdk64_tgz)

  return sdk32_zip;


def CreateWin32SDK(sdkpath):
  # Build the SDK
  CallBuildScript('release', 'ia32,x64', 'create_sdk')

  sdk32_zip = join(sdkpath, 'dartsdk-win32-32.zip')
  sdk64_zip = join(sdkpath, 'dartsdk-win32-64.zip')

  CreateZipWindows(join(DART_PATH, utils.GetBuildRoot('win32', 'release', 'ia32'),
                        'dart-sdk'), sdk32_zip)
  CreateZipWindows(join(DART_PATH, utils.GetBuildRoot('win32', 'release', 'x64'),
                        'dart-sdk'), sdk64_zip)

  UploadFile(sdk32_zip)
  UploadFile(sdk64_zip)

  return sdk32_zip;


def CallBuildScript(mode, arch, target):
  """ invoke tools/build.py """
  buildScript = join(TOOLS_PATH, 'build.py')
  cmd = [sys.executable, buildScript, '--mode=%s' % mode, '--arch=%s' % arch, target]
  status = ExecuteCommand(cmd, DART_PATH)
  if status:
    PrintError('SDK build failed: %s' % status)
    raise Exception('SDK build failed')


def CreateZip(directory, file):
  """zip the given directory into the file"""
  EnsureDirectoryExists(file)
  ExecuteCommand(['zip', '-yrq', file, os.path.basename(directory)],
                 os.path.dirname(directory))


def CreateZipWindows(directory, file):
  """zip the given directory into the file - win32 specific"""
  EnsureDirectoryExists(file)
  ExecuteCommand([join(DART_PATH, 'third_party', '7zip', '7za'), 'a', '-tzip',
                  file, os.path.basename(directory)], os.path.dirname(directory))
  
  
def CreateTgz(directory, file):
  """tar gzip the given directory into the file"""
  EnsureDirectoryExists(file)
  ExecuteCommand(['tar', 'czf', file, os.path.basename(directory)],
                 os.path.dirname(directory))


def UploadFile(file, createChecksum=True):
  """Upload the given file to google storage."""
  
  filePathRev = "%s/%s" % (GSU_PATH_REV, os.path.basename(file))
  filePathLatest = "%s/%s" % (GSU_PATH_LATEST, os.path.basename(file))
  
  if (createChecksum):
    checksum = CreateChecksumFile(file)
    
    checksumRev = "%s/%s" % (GSU_PATH_REV, os.path.basename(checksum))
    checksumLatest = "%s/%s" % (GSU_PATH_LATEST, os.path.basename(checksum))
  
  Gsutil(['cp', '-a', 'public-read', r'file://' + file, filePathRev])
  if (createChecksum):
    Gsutil(['cp', '-a', 'public-read', r'file://' + checksum, checksumRev])
    
  Gsutil(['cp', '-a', 'public-read', filePathRev, filePathLatest])
  if (createChecksum):
    Gsutil(['cp', '-a', 'public-read', checksumRev, checksumLatest])


def UploadDirectory(filesToUpload, gs_dir):
  Gsutil(['-m', 'cp', '-a', 'public-read', '-r'] + filesToUpload + [gs_dir])


def UploadApiDocs(dirName):
  # create file in dartlang-api-docs/REVISION/index.html
  # this lets us do the recursive copy in the next step

  localIndexFile = join(dirName, 'index.html')
  destIndexFile = GSU_API_DOCS_PATH + '/index.html'
  
  Gsutil(['cp', '-a', 'public-read', localIndexFile, destIndexFile])

  # copy -R api_docs into dartlang-api-docs/REVISION
  filesToUpload = glob.glob(join(dirName, '*'))
  result = Gsutil(['-m', 'cp', '-a', 'public-read', '-r'] + filesToUpload +
                  [GSU_API_DOCS_PATH])

  if result == 0:
    destLatestRevFile = GSU_API_DOCS_BUCKET + '/latest.txt'
    localLatestRevFilename = join(dirName, 'latest.txt')
    with open(localLatestRevFilename, 'w+') as f:
      f.write(REVISION)

    # overwrite dartlang-api-docs/latest.txt to contain REVISION
    Gsutil(['cp', '-a', 'public-read', localLatestRevFilename, destLatestRevFile])


def Gsutil(cmd):
  gsutilTool = join(DART_PATH, 'third_party', 'gsutil', 'gsutil')
  return ExecuteCommand([sys.executable, gsutilTool] + cmd)


def EnsureDirectoryExists(f):
  d = os.path.dirname(f)
  if not os.path.exists(d):
    os.makedirs(d)


def PrintSeparator(text):
  tag_line_sep = '================================'

  print
  print
  print text
  print tag_line_sep
  print
  sys.stdout.flush()


def PrintError(text):
  error_sep = '*****************************'
  error_text = ' {0}'

  print
  print error_sep
  print error_text.format(text)
  print error_sep
  print
  sys.stderr.flush()


def FileDelete(file):
  """delete the given file - do not re-throw any exceptions that occur"""
  if os.path.exists(file):
    try:
      os.remove(file)
    except:
      print 'error deleting %s' % file


if __name__ == '__main__':
  sys.exit(main())
