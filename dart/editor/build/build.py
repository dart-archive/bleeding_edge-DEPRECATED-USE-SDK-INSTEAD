#!/usr/bin/env python
#
# Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import glob
import gsutil
import imp
import optparse
import os
import re
import shutil
import stat
import subprocess
import sys
import tempfile
import zipfile
import ziputils

from os.path import join

BUILD_OS = None
DART_PATH = None
TOOLS_PATH = None

CHANNEL = None
PLUGINS_BUILD = None
REVISION = None
SYSTEM = None

NO_UPLOAD = None

DART_DIR = os.path.abspath(os.path.join(__file__, '..', '..', '..'))
utils = imp.load_source('utils', os.path.join(DART_DIR, 'tools', 'utils.py'))
bot_utils = imp.load_source('bot_utils',
    os.path.join(DART_DIR, 'tools', 'bots', 'bot_utils.py'))

def DartArchiveFile(local_path, remote_path, create_md5sum=False):
  # Copy it to the new unified gs://dart-archive bucket
  gsutil = bot_utils.GSUtil()
  gsutil.upload(local_path, remote_path, public=True)
  if create_md5sum:
    # 'local_path' may have a different filename than 'remote_path'. So we need
    # to make sure the *.md5sum file contains the correct name.
    assert '/' in remote_path and not remote_path.endswith('/')
    mangled_filename = remote_path[remote_path.rfind('/') + 1:]
    local_md5sum = bot_utils.CreateChecksumFile(local_path, mangled_filename)
    gsutil.upload(local_md5sum, remote_path + '.md5sum', public=True)

def DartArchiveUploadEditorZipFile(zipfile):
  namer = bot_utils.GCSNamer(CHANNEL, bot_utils.ReleaseType.RAW)
  gsutil = bot_utils.GSUtil()

  basename = os.path.basename(zipfile)
  system = None
  arch = None
  if basename.startswith('darteditor-linux'):
    system = 'linux'
  elif basename.startswith('darteditor-mac'):
    system = 'macos'
  elif basename.startswith('darteditor-win'):
    system = 'windows'

  if basename.endswith('-32.zip'):
    arch = 'ia32'
  elif basename.endswith('-64.zip'):
    arch = 'x64'

  assert system and arch

  for revision in [REVISION, 'latest']:
    DartArchiveFile(zipfile, namer.editor_zipfilepath(revision, system, arch),
        create_md5sum=True)

def DartArchiveUploadUpdateSite(local_path):
  namer = bot_utils.GCSNamer(CHANNEL, bot_utils.ReleaseType.RAW)
  gsutil = bot_utils.GSUtil()
  for revision in [REVISION, 'latest']:
    update_site_dir = namer.editor_eclipse_update_directory(revision)
    try:
      gsutil.remove(update_site_dir, recursive=True)
    except:
      # Ignore this, in the general case there is nothing.
      pass
    gsutil.upload(local_path, update_site_dir, recursive=True, public=True)

def DartArchiveUploadSDKs(system, sdk32_zip, sdk64_zip):
  namer = bot_utils.GCSNamer(CHANNEL, bot_utils.ReleaseType.RAW)
  for revision in [REVISION, 'latest']:
    path32 = namer.sdk_zipfilepath(revision, system, 'ia32', 'release')
    path64 = namer.sdk_zipfilepath(revision, system, 'x64', 'release')
    DartArchiveFile(sdk32_zip, path32, create_md5sum=True)
    DartArchiveFile(sdk64_zip, path64, create_md5sum=True)

def DartArchiveUploadAPIDocs(api_zip):
  namer = bot_utils.GCSNamer(CHANNEL, bot_utils.ReleaseType.RAW)
  for revision in [REVISION, 'latest']:
    destination = (namer.apidocs_directory(revision) + '/' +
        namer.apidocs_zipfilename())
    DartArchiveFile(api_zip, destination, create_md5sum=False)

def DartArchiveUploadAndroidZip(android_zip):
  namer = bot_utils.GCSNamer(CHANNEL, bot_utils.ReleaseType.RAW)
  for revision in [REVISION, 'latest']:
    destination = namer.editor_android_zipfilepath(revision)
    DartArchiveFile(android_zip, destination, create_md5sum=False)

def DartArchiveUploadVersionFile(version_file):
  namer = bot_utils.GCSNamer(CHANNEL, bot_utils.ReleaseType.RAW)
  for revision in [REVISION, 'latest']:
    DartArchiveFile(version_file, namer.version_filepath(revision),
        create_md5sum=False)

def DartArchiveUploadInstaller(
      arch, installer_file, extension, release_type=bot_utils.ReleaseType.RAW):
  namer = bot_utils.GCSNamer(CHANNEL, release_type)
  gsu_path = namer.editor_installer_filepath(
      REVISION, SYSTEM, arch, extension)
  DartArchiveFile(installer_file, gsu_path, create_md5sum=False)

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
    args.append('-Dbuild.channel=' + CHANNEL)
    args.append('-Ddart.version.full=' + utils.GetVersion())
    args.append('-Dbuild.version.qualifier=' +
        utils.GetEclipseVersionQualifier())

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
  result.add_option('--build-installer',
                    help='Fetch editor, build installer and archive it.',
                    action='store_true', default=False)
  return result

def main():
  """Main entry point for the build program."""
  global BUILD_OS
  global CHANNEL
  global DART_PATH
  global NO_UPLOAD
  global PLUGINS_BUILD
  global REVISION
  global SYSTEM
  global TOOLS_PATH

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
  antpath = os.path.join(thirdpartypath, 'apache_ant', '1.8.4')
  bzip2libpath = os.path.join(thirdpartypath, 'bzip2')
  buildpath = os.path.join(editorpath, 'tools', 'features',
                           'com.google.dart.tools.deploy.feature_releng')
  buildos = utils.GuessOS()

  BUILD_OS = utils.GuessOS()
  DART_PATH = dartpath
  TOOLS_PATH = toolspath

  if (os.environ.get('DART_NO_UPLOAD') is not None):
    NO_UPLOAD = True

  # TODO(devoncarew): remove this hardcoded e:\ path
  buildroot_parent = {'linux': dartpath, 'macos': dartpath, 'win32': r'e:\tmp'}
  buildroot = os.path.join(buildroot_parent[buildos], 'build_root')

  os.chdir(buildpath)
  ant_property_file = None
  sdk_zip = None

  try:
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
    print 'revision(in)   = |{0}|'.format(options.revision)
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

    username = utils.GetUserName()
    if not username:
      print 'Could not find username'
      return 6

    build_skip_tests = os.environ.get('DART_SKIP_RUNNING_TESTS')
    sdk_environment = os.environ
    if sdk_environment.has_key('JAVA_HOME'):
      print 'JAVA_HOME = {0}'.format(str(sdk_environment['JAVA_HOME']))

    EDITOR_REGEXP = (r'^dart-editor(-(?P<installer>(installer)))?'
                      '(-(?P<system>(win|mac|linux)))?' +
                      '(-(?P<channel>(be|dev|stable)))?$')
    builder_name = str(options.name)
    match = re.match(EDITOR_REGEXP, builder_name)
    if not match:
      raise Exception("Buildername '%s' does not match pattern '%s'."
                      % (builder_name, EDITOR_REGEXP))

    CHANNEL = match.groupdict()['channel'] or 'be'
    SYSTEM = match.groupdict()['system']
    BUILD_INSTALLER = bool(match.groupdict()['installer'])

    PLUGINS_BUILD = SYSTEM is None
    REVISION = revision

    # Make sure the buildername and the options agree
    assert BUILD_INSTALLER == options.build_installer

    running_on_buildbot = username.startswith('chrome')

    homegsutil = join(DART_PATH, 'third_party', 'gsutil', 'gsutil')
    gsu = gsutil.GsUtil(False, homegsutil,
      running_on_buildbot=running_on_buildbot)

    def build_installer():
      release_type = bot_utils.ReleaseType.SIGNED
      if CHANNEL == 'be':
        # We don't have signed bits on bleeding_edge, so we take the unsigned
        # editor.
        release_type = bot_utils.ReleaseType.RAW
      if SYSTEM == 'win':
        # The windows installer will use unsigned *.exe files to avoid going
        # through two rounds of signing.
        release_type = bot_utils.ReleaseType.RAW

      def editor_location(arch):
        namer = bot_utils.GCSNamer(CHANNEL, release_type)
        editor_path = namer.editor_zipfilepath(REVISION, SYSTEM, arch)
        return editor_path

      def create_windows_installer(installer_file, input_dir):
        # We add a README file to the installation.
        # The editor uses this to determine that we are a windows
        # installation.
        readme_file = os.path.join(input_dir, 'README-WIN')
        with open(readme_file, 'w') as fd:
          fd.write("This is the installation directory of the "
               "Dart Editor and the corresponding dart sdk.\n")

        msi_builder = os.path.join(DART_PATH, 'tools',
                                   'create_windows_installer.py')
        wix_bin = os.path.join(DART_PATH, 'third_party',
                               'wix')
        version = utils.GetShortVersion()
        bot_utils.run(['python', msi_builder,
                       '--msi_location=%s' % installer_file,
                       '--input_directory=%s' % input_dir,
                       '--version=%s' % version,
                       '--wix_bin=%s' % wix_bin])

      def create_mac_installer(installer_file):
        dart_folder_icon = os.path.join(DART_PATH,
            'editor/tools/plugins/com.google.dart.tools.ui/' +
            'icons/dart_about_140_160.png')
        dmg_builder = os.path.join(DART_PATH, 'tools',
                                   'mac_build_editor_dmg.sh')
        bot_utils.run([dmg_builder, installer_file, 'dart',
                       dart_folder_icon, "Dart Installer"])


      if SYSTEM == 'mac' or SYSTEM == 'win':
        for arch in ['32', '64']:
          extension = 'dmg' if SYSTEM == 'mac' else 'msi'
          with utils.TempDir('build_editor_installer') as temp_dir:
            with utils.ChangedWorkingDirectory(temp_dir):
              # Fetch the editor zip file from the new location.
              zip_location = os.path.join(temp_dir, 'editor.zip')
              if gsu.Copy(editor_location(arch), zip_location, False):
                raise Exception("gsutil command failed, aborting.")
              # Unzip the editor (which contains a directory named 'dart').
              editor_zip = ziputils.ZipUtil(zip_location, buildos)
              editor_zip.UnZip(temp_dir)
              unzip_dir = os.path.join(temp_dir, 'dart')

              assert os.path.exists('dart') and os.path.isdir('dart')
              installer_name = 'darteditor-installer.%s' % extension
              installer_file = os.path.join(temp_dir, installer_name)
              if SYSTEM == 'mac':
                create_mac_installer(installer_file)
              else:
                create_windows_installer(installer_file, unzip_dir)
              assert os.path.isfile(installer_file)

              # Archive to new bucket
              DartArchiveUploadInstaller(arch, installer_file, extension,
                                         release_type=release_type)

      else:
        raise Exception(
            "We currently cannot build installers for %s" % SYSTEM)
    def build_editor(sdk_zip):
      ant_property_file = tempfile.NamedTemporaryFile(suffix='.property',
                                                      prefix='AntProperties',
                                                      delete=False)
      ant_property_file.close()
      ant = AntWrapper(ant_property_file.name, os.path.join(antpath, 'bin'),
                       bzip2libpath)

      ant.RunAnt(os.getcwd(), '', '', '', '',
                 '', '', buildos, ['-diagnostics'])

      if not os.path.exists(buildout):
        os.makedirs(buildout)

      # clean out old build artifacts
      for f in os.listdir(buildout):
        if ('dartsdk-' in f) or ('darteditor-' in f) or ('dart-editor-' in f):
          os.remove(join(buildout, f))

      InstallDartium(buildroot, buildout, buildos, gsu)

      if not PLUGINS_BUILD:
        StartBuildStep('create_sdk')
        EnsureDirectoryExists(buildout)
        try:
          sdk_zip = CreateSDK(buildout)
        except:
          BuildStepFailure()


      if builder_name.startswith('dart-editor-linux'):
        StartBuildStep('api_docs')
        try:
          CreateApiDocs(buildout)
        except:
          BuildStepFailure()

      StartBuildStep(builder_name)

      if PLUGINS_BUILD:
        status = BuildUpdateSite(ant, revision, builder_name, buildroot,
                                 buildout, editorpath, buildos)
        return status

      with utils.TempDir('ExtraArtifacts') as extra_artifacts:
        # Tell the ant script where to write the sdk zip file so it can
        # be expanded later
        status = ant.RunAnt('.', 'build_rcp.xml', revision, builder_name,
                            buildroot, buildout, editorpath, buildos,
                            sdk_zip=sdk_zip,
                            running_on_bot=running_on_buildbot,
                            extra_artifacts=extra_artifacts)
        # The ant script writes a property file in a known location so
        # we can read it.
        properties = ReadPropertyFile(buildos, ant_property_file.name)

        if not properties:
          raise Exception('no data was found in file {%s}'
                          % ant_property_file.name)
        if status:
          if properties['build.runtime']:
            PrintErrorLog(properties['build.runtime'])
          return status

        # For the dart-editor build, return at this point.
        # We don't need to install the sdk+dartium, run tests, or copy to google
        # storage.
        if not buildos:
          print 'skipping sdk and dartium steps for dart-editor build'
          return 0

        # This is an override for local testing
        force_run_install = os.environ.get('FORCE_RUN_INSTALL')

        if force_run_install or (not PLUGINS_BUILD):
          InstallSdk(buildroot, buildout, buildos, buildout)
          InstallDartium(buildroot, buildout, buildos, gsu)

        if status:
          return status

        if not build_skip_tests:
          RunEditorTests(buildout, buildos)

        if buildos:
          StartBuildStep('upload_artifacts')

          _InstallArtifacts(buildout, buildos, extra_artifacts)

          # dart-editor-linux.gtk.x86.zip --> darteditor-linux-32.zip
          RenameRcpZipFiles(buildout)

          PostProcessEditorBuilds(buildout, buildos)

          if running_on_buildbot:
            version_file = _FindVersionFile(buildout)
            if version_file:
              DartArchiveUploadVersionFile(version_file)

            # Archive the android editor zip file
            if builder_name.startswith('dart-editor-linux'):
              UploadAndroidZip(buildout)

            found_zips = _FindRcpZipFiles(buildout)
            for zipfile in found_zips:
              DartArchiveUploadEditorZipFile(zipfile)

        return 0

    if BUILD_INSTALLER:
      build_installer()
    else:
      build_editor(sdk_zip)
  finally:
    if ant_property_file is not None:
      print 'cleaning up temp file {0}'.format(ant_property_file.name)
      os.remove(ant_property_file.name)
    print 'cleaning up {0}'.format(buildroot)
    shutil.rmtree(buildroot, True)
    print 'Build Done'


def ReadPropertyFile(buildos, property_file):
  """Read a property file and return a dictionary of key/value pairs.

  Args:
    buildos: the os the build is running under
    property_file: the file to read

  Returns:
    the dictionary of Ant properties
  """
  properties = {}
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

  version_file = os.path.join(out_dir, 'VERSION')
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

  sdk_zip = ziputils.ZipUtil(join(sdk_dir, "dartsdk-%s-32.zip" % buildos),
                             buildos)
  sdk_zip.UnZip(unzip_dir_32)
  sdk_zip = ziputils.ZipUtil(join(sdk_dir, "dartsdk-%s-64.zip" % buildos),
                             buildos)
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


def InstallDartiumFromDartArchive(buildroot, buildout, buildos, gsu):
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
  revision = 'latest' if CHANNEL == 'be' else REVISION

  rcpZipFiles = _FindRcpZipFiles(buildout)
  for rcpZipFile in rcpZipFiles:
    print '  found rcp: %s' % rcpZipFile

    arch = 'ia32'
    system  = None
    local_name = None

    if '-linux.gtk.x86.zip' in rcpZipFile:
      local_name = 'dartium-lucid32'
      system = 'linux'
    if '-linux.gtk.x86_64.zip' in rcpZipFile:
      local_name = 'dartium-lucid64'
      arch = 'x64'
      system = 'linux'
    if 'macosx' in rcpZipFile:
      local_name = 'dartium-mac'
      system = 'macos'
    if 'win32' in rcpZipFile:
      local_name = 'dartium-win'
      system = 'windows'

    namer = bot_utils.GCSNamer(CHANNEL, bot_utils.ReleaseType.RAW)
    dartiumFile = namer.dartium_variant_zipfilepath(
        revision, 'dartium', system, arch, 'release')

    # Download and unzip dartium
    unzip_dir = os.path.join(tmp_dir,
      os.path.splitext(os.path.basename(dartiumFile))[0])
    if not os.path.exists(unzip_dir):
      os.makedirs(unzip_dir)

    # Always download as local_name.zip
    tmp_zip_file = os.path.join(tmp_dir, "%s.zip" % local_name)
    if not os.path.exists(tmp_zip_file):
      if gsu.Copy(dartiumFile, tmp_zip_file, False):
        raise Exception("gsutil command failed, aborting.")
      # Dartium is unzipped into unzip_dir/dartium-*/
      dartium_zip = ziputils.ZipUtil(tmp_zip_file, buildos)
      dartium_zip.UnZip(unzip_dir)

    dart_zip_path = join(buildout, rcpZipFile)
    dart_zip = ziputils.ZipUtil(dart_zip_path, buildos)

    add_path = glob.glob(join(unzip_dir, 'dartium-*'))[0]
    if system == 'windows':
      # TODO(ricow/kustermann): This is hackisch. We should make a generic
      # black/white-listing mechanism.
      FileDelete(join(add_path, 'mini_installer.exe'))
      FileDelete(join(add_path, 'sync_unit_tests.exe'))
      FileDelete(join(add_path, 'chrome.packed.7z'))

    # Add dartium to the rcp zip
    dart_zip.AddDirectoryTree(add_path, 'dart/chromium')
  shutil.rmtree(tmp_dir, True)


def InstallDartium(buildroot, buildout, buildos, gsu):
  # On our be/dev/stable channels, we fetch dartium from the new
  # gs://dart-archive/ location.
  InstallDartiumFromDartArchive(buildroot, buildout, buildos, gsu)


def _InstallArtifacts(buildout, buildos, extra_artifacts):
  """Install extra build artifacts into the RCP zip files.

  Args:
    buildout: the location of the ant build output
    buildos: the OS the build is running under
    extra_artifacts: the directory containing the extra artifacts
  """
  print '_InstallArtifacts({%s}, {%s}, {%s})' % (buildout, buildos,
                                                 extra_artifacts)
  files = _FindRcpZipFiles(buildout)
  for f in files:
    dart_zip_path = os.path.join(buildout, f)
    dart_zip = ziputils.ZipUtil(dart_zip_path, buildos)
    dart_zip.AddDirectoryTree(extra_artifacts, 'dart')


def RenameRcpZipFiles(out_dir):
  """Rename the RCP files to be more consistent with the Dart SDK names"""
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


def PostProcessEditorBuilds(out_dir, buildos):
  """Post-process the created RCP builds"""
  with utils.TempDir('editor_scratch') as scratch_dir:

    def instantiate_download_script_template(destination, replacements):
      """Helper function for replacing variables in the
      tools/dartium/download_shellscript_templates.{sh,bat} scripts. It will
      write the final download script to [destination] after doing all
      replacements given in the [replacements] dictionary."""
      template_location = {
       'win' : join(
          DART_DIR, 'tools', 'dartium', 'download_shellscript_template.bat'),
       'linux' : join(
          DART_DIR, 'tools', 'dartium', 'download_shellscript_template.sh'),
       'mac' : join(
          DART_DIR, 'tools', 'dartium', 'download_shellscript_template.sh'),
      }[SYSTEM]

      with open(template_location) as fd:
        content = fd.read()
      for key in replacements:
        content = content.replace(key, replacements[key])
      with open(destination, 'w') as fd:
        fd.write(content)

      # Make it executable if we are not on windows
      if SYSTEM != 'win':
        os.chmod(destination, os.stat(destination).st_mode | stat.S_IEXEC)

    def add_download_scripts(zipFile, arch):
      shell_ending = {
        'win' : '.bat',
        'linux' : '.sh',
        'mac' : '.sh',
      }[SYSTEM]

      # We don't have 64-bit versions of dartium/content_shell for
      # macos/windows.
      if SYSTEM in ['mac', 'win']:
        arch = '32'

      namer = bot_utils.GCSNamer(CHANNEL, bot_utils.ReleaseType.RELEASE)

      # We're adding download scripts to the chromium directory.
      # The directory tree will look like this after that:
      #   dart/dart-sdk/bin/dart{,.exe}
      #       /chromium/download_contentshell.{sh,bat}
      #       /chromium/download_dartium_debug.{sh,bat}
      #       /chromium/download_file.dart

      # Add download_file.dart helper utility to the zip file.
      f = ziputils.ZipUtil(zipFile, buildos)
      f.AddFile(join(DART_DIR, 'tools', 'dartium', 'download_file.dart'),
                'dart/chromium/download_file.dart')

      # Add content shell download script
      contentshell_name = namer.dartium_variant_zipfilename(
          'content_shell', SYSTEM, arch, 'release')
      contentshell_download_script = join(scratch_dir, 'download_contentshell')
      instantiate_download_script_template(contentshell_download_script, {
        'VAR_DESTINATION' : contentshell_name,
        'VAR_DOWNLOAD_URL' :
            ("http://dartlang.org/editor/update/channels/%s/%s/dartium/%s"
             % (CHANNEL, REVISION, contentshell_name)),
      })
      f.AddFile(contentshell_download_script,
          'dart/chromium/download_contentshell%s' % shell_ending)

      # Add dartium debug download script
      dartium_debug_name = namer.dartium_variant_zipfilename(
          'dartium', SYSTEM, arch, 'debug')
      dartium_download_script = join(scratch_dir, 'download_dartium_debug')
      instantiate_download_script_template(dartium_download_script, {
        'VAR_DESTINATION' : dartium_debug_name,
        'VAR_DOWNLOAD_URL' :
            ("http://dartlang.org/editor/update/channels/%s/%s/dartium/%s"
             % (CHANNEL, REVISION, dartium_debug_name)),
      })
      f.AddFile(dartium_download_script,
          'dart/chromium/download_dartium_debug%s' % shell_ending)

    def add_android_content_shell(zipFile):
      # On bleeding edge we take the latest bits, we don't want to wait
      # for the dartium builders to finish.
      revision = 'latest' if CHANNEL == 'be' else REVISION
      with utils.TempDir('apk') as tempDir:
        namer = bot_utils.GCSNamer(CHANNEL, bot_utils.ReleaseType.RELEASE)
        content_shell_name = 'content_shell-android'
        apkName = namer.dartium_android_apk_filename(content_shell_name,
                                                     'arm',
                                                     'release')
        remoteApk = namer.dartium_android_apk_filepath(revision,
                                                       content_shell_name,
                                                       'arm',
                                                       'release')
        local_path = os.path.join(tempDir, apkName)
        if gsu.Copy(remoteApk, local_path, False):
          raise Exception("gsutil command failed, aborting.")
        f = ziputils.ZipUtil(zipFile, buildos)
        f.AddFile(local_path, 'dart/android/%s' % apkName)

    # Create a editor.properties
    editor_properties = os.path.join(scratch_dir, 'editor.properties')
    with open(editor_properties, 'w') as fd:
      fd.write("com.dart.tools.update.core.url=http://dartlang.org"
               "/editor/update/channels/%s/\n" % CHANNEL)

    for zipFile in _FindRcpZipFiles(out_dir):
      basename = os.path.basename(zipFile)
      is_64bit = basename.endswith('-64.zip')

      print('  processing %s' % basename)

      readme_file = join('dart', 'README')
      if (basename.startswith('darteditor-win32-')):
        seven_zip = os.path.join(DART_DIR, 'third_party', '7zip', '7za.exe')
        bot_utils.run([seven_zip, 'd', zipFile, readme_file], env=os.environ)
      else:
        bot_utils.run(['zip', '-d', zipFile, readme_file], env=os.environ)

      # If we're on -dev/-stable build: add an editor.properties file
      # pointing to the correct update location of the editor for the channel
      # we're building for.
      if CHANNEL != 'be':
        f = ziputils.ZipUtil(zipFile, buildos)
        f.AddFile(editor_properties, 'dart/editor.properties')

      # Add a shell/bat script to download contentshell and dartium debug.
      # (including the necessary tools/dartium/download_file.dart helper).
      add_download_scripts(zipFile, '64' if is_64bit else '32')

      add_android_content_shell(zipFile)

      # adjust memory params for 64 bit versions
      if is_64bit:
        if (basename.startswith('darteditor-macos-')):
          inifile = join('dart', 'DartEditor.app', 'Contents', 'MacOS',
                         'DartEditor.ini')
        else:
          inifile = join('dart', 'DartEditor.ini')

        if (basename.startswith('darteditor-win32-')):
          f = zipfile.ZipFile(zipFile)
          f.extract(inifile.replace('\\','/'))
          f.close()
        else:
          bot_utils.run(['unzip', zipFile, inifile], env=os.environ)

        Modify64BitDartEditorIni(inifile)

        if (basename.startswith('darteditor-win32-')):
          seven_zip = os.path.join(DART_DIR, 'third_party', '7zip', '7za.exe')
          bot_utils.run([seven_zip, 'd', zipFile, inifile], env=os.environ)
          bot_utils.run([seven_zip, 'a', zipFile, inifile], env=os.environ)
        else:
          bot_utils.run(['zip', '-d', zipFile, inifile], env=os.environ)
          bot_utils.run(['zip', '-q', zipFile, inifile], env=os.environ)
        os.remove(inifile)

      # post-process the info.plist file
      if (basename.startswith('darteditor-macos-')):
        infofile = join('dart', 'DartEditor.app', 'Contents', 'Info.plist')
        bot_utils.run(['unzip', zipFile, infofile], env=os.environ)
        ReplaceInFiles(
            [infofile],
            [('<dict>',
              '<dict>\n\t<key>NSHighResolutionCapable</key>\n\t\t<true/>')])
        bot_utils.run(['zip', '-q', zipFile, infofile], env=os.environ)
        os.remove(infofile)


def Modify64BitDartEditorIni(iniFilePath):
  f = open(iniFilePath, 'r')
  lines = f.readlines()
  f.close()
  lines[lines.index('-Xms40m\n')] = '-Xms256m\n'
  lines[lines.index('-Xmx1024m\n')] = '-Xmx2000m\n'
  # Add -d64 to give better error messages to user in 64 bit mode.
  lines[lines.index('-vmargs\n')] = '-vmargs\n-d64\n'
  f = open(iniFilePath, 'w')
  f.writelines(lines)
  f.close()


def RunEditorTests(buildout, buildos):
  StartBuildStep('run_tests')

  for editorArchive in _GetTestableRcpArchives(buildout):
    with utils.TempDir('editor_') as tempDir:
      print 'Running tests for %s...' % editorArchive

      zipper = ziputils.ZipUtil(join(buildout, editorArchive), buildos)
      zipper.UnZip(tempDir)

      # before we run the editor, suppress any 'restore windows' dialogs
      if sys.platform == 'darwin':
        args = ['defaults', 'write', 'org.eclipse.eclipse.savedState',
                'NSQuitAlwaysKeepsWindows', '-bool', 'false']
        subprocess.call(args, shell=IsWindows())

      editorExecutable = GetEditorExecutable(join(tempDir, 'dart'))
      args = [editorExecutable, '--test', '--auto-exit',
              '-data', join(tempDir, 'workspace')]

      # Issue 12638. Enable this as soon as we can run editor tests in xvfb
      # again.
      ##if sys.platform == 'linux2':
      ##  args = ['xvfb-run', '-a'] + args

      # this can hang if a 32 bit jvm is not available on windows...
      if subprocess.call(args, shell=IsWindows()):
        BuildStepFailure()


# Return x86_64.zip (64 bit) on mac and linux; x86.zip (32 bit) on windows
def _GetTestableRcpArchives(buildout):
  result = []

  for archive in _FindRcpZipFiles(buildout):
    if IsWindows() and archive.endswith('x86.zip'):
      result.append(archive)
    elif not IsWindows() and archive.endswith('x86_64.zip'):
      result.append(archive)

  return result


def GetEditorExecutable(editorDir):
  if sys.platform == 'darwin':
    executable = join('DartEditor.app', 'Contents', 'MacOS', 'DartEditor')
  elif sys.platform == 'win32':
    executable = 'DartEditor.exe'
  else:
    executable = 'DartEditor'
  return join(editorDir, executable)


def IsWindows():
  return sys.platform == 'win32'


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


def ExecuteCommand(cmd, directory=None):
  """Execute the given command."""
  if directory is not None:
    cwd = os.getcwd()
    os.chdir(directory)
  status = subprocess.call(cmd, env=os.environ)
  if directory is not None:
    os.chdir(cwd)
  if status:
    raise Exception('Running %s failed' % cmd)


def BuildUpdateSite(ant, revision, name, buildroot, buildout,
              editorpath, buildos):
  status = ant.RunAnt('../com.google.dart.eclipse.feature_releng',
             'build.xml', revision, name, buildroot, buildout,
              editorpath, buildos, ['-Dbuild.dir=%s' % buildout])
  if status:
    BuildStepFailure()
  else:
    StartBuildStep('upload_artifacts')
    DartArchiveUploadUpdateSite(join(buildout, 'buildRepo'))
  return status


def CreateApiDocs(buildLocation):
  """Zip up api_docs, upload it, and upload the raw tree of docs"""

  apidir = join(DART_PATH,
                utils.GetBuildRoot(BUILD_OS, 'release', 'ia32'),
                'api_docs')

  shutil.rmtree(apidir, ignore_errors = True)

  CallBuildScript('release', 'ia32', 'api_docs')
  CallBuildScript('release', 'ia32', 'docgen')

  UploadApiDocs(apidir)

  api_zip = join(buildLocation, 'dart-api-docs.zip')

  CreateZip(apidir, api_zip)

  DartArchiveUploadAPIDocs(api_zip)

def UploadAndroidZip(out_dir):
  android_zip = join(out_dir, 'android.zip')
  DartArchiveUploadAndroidZip(android_zip)


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

  # Build the SDK
  CallBuildScript('release', 'ia32,x64', 'create_sdk')

  sdk32_zip = join(sdkpath, 'dartsdk-linux-32.zip')
  sdk64_zip = join(sdkpath, 'dartsdk-linux-64.zip')

  CreateZip(sdkdir32, sdk32_zip)
  CreateZip(sdkdir64, sdk64_zip)

  DartArchiveUploadSDKs('linux', sdk32_zip, sdk64_zip)

  return sdk32_zip


def CreateMacosSDK(sdkpath):
  # Build the SDK
  CallBuildScript('release', 'ia32,x64', 'create_sdk')

  sdk32_zip = join(sdkpath, 'dartsdk-macos-32.zip')
  sdk64_zip = join(sdkpath, 'dartsdk-macos-64.zip')

  CreateZip(join(DART_PATH, utils.GetBuildRoot('macos', 'release', 'ia32'),
                 'dart-sdk'), sdk32_zip)
  CreateZip(join(DART_PATH, utils.GetBuildRoot('macos', 'release', 'x64'),
                 'dart-sdk'), sdk64_zip)

  DartArchiveUploadSDKs('macos', sdk32_zip, sdk64_zip)

  return sdk32_zip


def CreateWin32SDK(sdkpath):
  # Build the SDK
  CallBuildScript('release', 'ia32,x64', 'create_sdk')

  sdk32_zip = join(sdkpath, 'dartsdk-win32-32.zip')
  sdk64_zip = join(sdkpath, 'dartsdk-win32-64.zip')

  CreateZipWindows(join(DART_PATH,
                        utils.GetBuildRoot('win32', 'release', 'ia32'),
                        'dart-sdk'), sdk32_zip)
  CreateZipWindows(join(DART_PATH,
                        utils.GetBuildRoot('win32', 'release', 'x64'),
                        'dart-sdk'), sdk64_zip)

  DartArchiveUploadSDKs('win32', sdk32_zip, sdk64_zip)

  return sdk32_zip


def CallBuildScript(mode, arch, target):
  """invoke tools/build.py"""
  buildScript = join(TOOLS_PATH, 'build.py')
  cmd = [sys.executable, buildScript, '--mode=%s' % mode, '--arch=%s' % arch,
         target]
  try:
    ExecuteCommand(cmd, DART_PATH)
  except Exception as error:
    print '%s build failed: %s' % (target, error)
    BuildStepFailure()
    raise Exception('%s build failed' % target)


def CreateZip(directory, targetFile):
  """zip the given directory into the file"""
  EnsureDirectoryExists(targetFile)
  FileDelete(targetFile)
  ExecuteCommand(['zip', '-yrq9', targetFile, os.path.basename(directory)],
                 os.path.dirname(directory))


def CreateZipWindows(directory, targetFile):
  """zip the given directory into the file - win32 specific"""
  EnsureDirectoryExists(targetFile)
  FileDelete(targetFile)
  ExecuteCommand([join(DART_PATH, 'third_party', '7zip', '7za'), 'a', '-tzip',
                  targetFile,
                  os.path.basename(directory)],
                 os.path.dirname(directory))


def UploadApiDocs(dirName):
  apidocs_namer = bot_utils.GCSNamerApiDocs(CHANNEL)
  apidocs_destination_gcsdir = apidocs_namer.docs_dirpath(REVISION)
  apidocs_destination_latestfile = apidocs_namer.docs_latestpath(REVISION)

  # Return early if the documents have already been uploaded.
  # (This can happen if a build was forced, or a commit had no changes in the
  # dart repository (e.g. DEPS file update).)
  if GsutilExists(apidocs_destination_gcsdir):
    print ("Not uploading api docs, since %s is already present."
           % apidocs_destination_gcsdir)
    return

  # Upload everything inside the built apidocs directory.
  Gsutil(['-m', 'cp', '-R', '-a', 'public-read', dirName,
          apidocs_destination_gcsdir])

  # Update latest.txt to contain the newest revision.
  with utils.TempDir('latest_file') as temp_dir:
    latest_file = join(temp_dir, 'latest.txt')
    with open(latest_file, 'w') as f:
      f.write('%s' % REVISION)

    Gsutil(['cp', '-a', 'public-read', latest_file,
            apidocs_destination_latestfile])

def Gsutil(cmd):
  gsutilTool = join(DART_PATH, 'third_party', 'gsutil', 'gsutil')
  ExecuteCommand([sys.executable, gsutilTool] + cmd)

def GsutilExists(gsu_path):
  gsutilTool = join(DART_PATH, 'third_party', 'gsutil', 'gsutil')
  (_, stderr, returncode) = bot_utils.run(
      [gsutilTool, 'ls', gsu_path],
      throw_on_error=False)
  # If the returncode is nonzero and we can find a specific error message,
  # we know there are no objects with a prefix of [gsu_path].
  missing = (returncode and 'CommandException: No such object' in stderr)
  # Either the returncode has to be zero or the object must be missing,
  # otherwise throw an exception.
  if not missing and returncode:
    raise Exception("Failed to determine whether %s exists" % gsu_path)
  return not missing

def EnsureDirectoryExists(f):
  d = os.path.dirname(f)
  if not os.path.exists(d):
    os.makedirs(d)


def StartBuildStep(name):
  print "@@@BUILD_STEP %s@@@" % name
  sys.stdout.flush()


def BuildStepFailure():
  print '@@@STEP_FAILURE@@@'
  sys.stdout.flush()


def FileDelete(f):
  """delete the given file - do not re-throw any exceptions that occur"""
  if os.path.exists(f):
    try:
      os.remove(f)
    except OSError:
      print 'error deleting %s' % f

if __name__ == '__main__':
  sys.exit(main())
