#!/usr/bin/env python
#
# Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.
#

import optparse
import os
import re
import shutil
import subprocess
import sys
import utils

HOST_OS = utils.GuessOS()
HOST_CPUS = utils.GuessCpus()
SCRIPT_DIR = os.path.dirname(sys.argv[0])
DART_ROOT = os.path.realpath(os.path.join(SCRIPT_DIR, '..'))
THIRD_PARTY_ROOT = os.path.join(DART_ROOT, 'third_party')

arm_cc_error = """
Couldn't find the arm cross compiler.
To make sure that you have the arm cross compilation tools installed, run:

$ wget http://src.chromium.org/chrome/trunk/src/build/install-build-deps.sh
OR
$ svn co http://src.chromium.org/chrome/trunk/src/build; cd build
Then,
$ chmod u+x install-build-deps.sh
$ ./install-build-deps.sh --arm --no-chromeos-fonts
"""
DEFAULT_ARM_CROSS_COMPILER_PATH = '/usr/bin'


def BuildOptions():
  result = optparse.OptionParser()
  result.add_option("-m", "--mode",
      help='Build variants (comma-separated).',
      metavar='[all,debug,release]',
      default='debug')
  result.add_option("-v", "--verbose",
      help='Verbose output.',
      default=False, action="store_true")
  result.add_option("-a", "--arch",
      help='Target architectures (comma-separated).',
      metavar='[all,ia32,x64,simarm,arm,simmips,mips,simarm64]',
      default=utils.GuessArchitecture())
  result.add_option("--os",
    help='Target OSs (comma-separated).',
    metavar='[all,host,android]',
    default='host')
  result.add_option("-t", "--toolchain",
    help='Cross-compiler toolchain path',
    default=None)
  result.add_option("-j",
      help='The number of parallel jobs to run.',
      metavar=HOST_CPUS,
      default=str(HOST_CPUS))
  (vs_directory, vs_executable) = utils.GuessVisualStudioPath()
  result.add_option("--devenv",
      help='Path containing devenv.com on Windows',
      default=vs_directory)
  result.add_option("--executable",
      help='Name of the devenv.com/msbuild executable on Windows (varies for '
           'different versions of Visual Studio)',
      default=vs_executable)
  return result


def ProcessOsOption(os):
  if os == 'host':
    return HOST_OS
  return os


def ProcessOptions(options, args):
  if options.arch == 'all':
    options.arch = 'ia32,x64,simarm,simmips,simarm64'
  if options.mode == 'all':
    options.mode = 'release,debug'
  if options.os == 'all':
    options.os = 'host,android'
  options.mode = options.mode.split(',')
  options.arch = options.arch.split(',')
  options.os = options.os.split(',')
  for mode in options.mode:
    if not mode in ['debug', 'release']:
      print "Unknown mode %s" % mode
      return False
  for arch in options.arch:
    archs = ['ia32', 'x64', 'simarm', 'arm', 'simmips', 'mips', 'simarm64']
    if not arch in archs:
      print "Unknown arch %s" % arch
      return False
  options.os = [ProcessOsOption(os) for os in options.os]
  for os in options.os:
    if not os in ['android', 'freebsd', 'linux', 'macos', 'win32']:
      print "Unknown os %s" % os
      return False
    if os != HOST_OS:
      if os != 'android':
        print "Unsupported target os %s" % os
        return False
      if not HOST_OS in ['linux']:
        print ("Cross-compilation to %s is not supported on host os %s."
               % (os, HOST_OS))
        return False
      if not arch in ['ia32', 'arm', 'mips']:
        print ("Cross-compilation to %s is not supported for architecture %s."
               % (os, arch))
        return False
      # We have not yet tweaked the v8 dart build to work with the Android
      # NDK/SDK, so don't try to build it.
      if args == []:
        print "For android builds you must specify a target, such as 'runtime'."
        return False
  return True


def SetTools(arch, target_os, toolchainprefix):
  toolsOverride = None

  # For Android, by default use the toolchain from third_party/android_tools.
  if target_os == 'android' and toolchainprefix == None:
    android_toolchain = GetAndroidToolchainDir(HOST_OS, arch)
    if arch == 'arm':
      toolchainprefix = os.path.join(
          android_toolchain, 'arm-linux-androideabi')
    if arch == 'ia32':
      toolchainprefix = os.path.join(
          android_toolchain, 'i686-linux-android')

  # For ARM Linux, by default use the Linux distribution's cross-compiler.
  if arch == 'arm' and toolchainprefix == None:
    # We specify the hf compiler. If this changes, we must also remove
    # the ARM_FLOAT_ABI_HARD define in configurations_make.gypi.
    toolchainprefix = (DEFAULT_ARM_CROSS_COMPILER_PATH +
                       "/arm-linux-gnueabihf")

  # TODO(zra): Find a default MIPS Linux cross-compiler?

  # Override the Android toolchain's linker to handle some complexity in the
  # linker arguments that gyp has trouble with.
  linker = ""
  if target_os == 'android':
    linker = os.path.join(DART_ROOT, 'tools', 'android_link.py')
  elif toolchainprefix:
    linker = toolchainprefix + "-g++"

  if toolchainprefix:
    toolsOverride = {
      "CC.target"  :  toolchainprefix + "-gcc",
      "CXX.target" :  toolchainprefix + "-g++",
      "AR.target"  :  toolchainprefix + "-ar",
      "LINK.target":  linker,
      "NM.target"  :  toolchainprefix + "-nm",
    }
  return toolsOverride


def CheckDirExists(path, docstring):
  if not os.path.isdir(path):
    raise Exception('Could not find %s directory %s'
          % (docstring, path))


def GetAndroidToolchainDir(host_os, target_arch):
  global THIRD_PARTY_ROOT
  if host_os not in ['linux']:
    raise Exception('Unsupported host os %s' % host_os)
  if target_arch not in ['ia32', 'arm']:
    raise Exception('Unsupported target architecture %s' % target_arch)

  # Set up path to the Android NDK.
  CheckDirExists(THIRD_PARTY_ROOT, 'third party tools');
  android_tools = os.path.join(THIRD_PARTY_ROOT, 'android_tools')
  CheckDirExists(android_tools, 'Android tools')
  android_ndk_root = os.path.join(android_tools, 'ndk')
  CheckDirExists(android_ndk_root, 'Android NDK')

  # Set up the directory of the Android NDK cross-compiler toolchain.
  toolchain_arch = 'arm-linux-androideabi-4.6'
  if target_arch == 'ia32':
    toolchain_arch = 'x86-4.6'
  toolchain_dir = 'linux-x86_64'
  android_toolchain = os.path.join(android_ndk_root,
      'toolchains', toolchain_arch,
      'prebuilt', toolchain_dir, 'bin')
  CheckDirExists(android_toolchain, 'Android toolchain')

  return android_toolchain


def Execute(args):
  process = subprocess.Popen(args)
  process.wait()
  if process.returncode != 0:
    raise Exception(args[0] + " failed")


def CurrentDirectoryBaseName():
  """Returns the name of the current directory"""
  return os.path.relpath(os.curdir, start=os.pardir)


def FilterEmptyXcodebuildSections(process):
  """
  Filter output from xcodebuild so empty sections are less verbose.

  The output from xcodebuild looks like this:

Build settings from command line:
    SYMROOT = .../xcodebuild

=== BUILD TARGET samples OF PROJECT dart WITH CONFIGURATION ...

Check dependencies

=== BUILD AGGREGATE TARGET upload_sdk OF PROJECT dart WITH CONFIGURATION ...

Check dependencies

PhaseScriptExecution "Action \"upload_sdk_py\"" xcodebuild/dart.build/...
    cd ...
    /bin/sh -c .../xcodebuild/dart.build/ReleaseIA32/upload_sdk.build/...


** BUILD SUCCEEDED **

  """

  def is_empty_chunk(chunk):
    empty_chunk = ['', 'Check dependencies', '']
    return not chunk or (len(chunk) == 4 and chunk[1:] == empty_chunk)

  def unbuffered(callable):
    # Use iter to disable buffering in for-in.
    return iter(callable, '')

  section = None
  chunk = []
  # Is stdout a terminal which supports colors?
  is_fancy_tty = False
  clr_eol = None
  if sys.stdout.isatty():
    term = os.getenv('TERM', 'dumb')
    # The capability "clr_eol" means clear the line from cursor to end
    # of line.  See man pages for tput and terminfo.
    try:
      with open('/dev/null', 'a') as dev_null:
        clr_eol = subprocess.check_output(['tput', '-T' + term, 'el'],
                                          stderr=dev_null)
      if clr_eol:
        is_fancy_tty = True
    except subprocess.CalledProcessError:
      is_fancy_tty = False
    except AttributeError:
      is_fancy_tty = False
  pattern = re.compile(r'=== BUILD.* TARGET (.*) OF PROJECT (.*) WITH ' +
                       r'CONFIGURATION (.*) ===')
  has_interesting_info = False
  for line in unbuffered(process.stdout.readline):
    line = line.rstrip()
    if line.startswith('=== BUILD ') or line.startswith('** BUILD '):
      has_interesting_info = False
      section = line
      if is_fancy_tty:
        match = re.match(pattern, section)
        if match:
          section = '%s/%s/%s' % (
            match.group(3), match.group(2), match.group(1))
        # Truncate to avoid extending beyond 80 columns.
        section = section[:80]
        # If stdout is a terminal, emit "progress" information.  The
        # progress information is the first line of the current chunk.
        # After printing the line, move the cursor back to the
        # beginning of the line.  This has two effects: First, if the
        # chunk isn't empty, the first line will be overwritten
        # (avoiding duplication).  Second, the next segment line will
        # overwrite it too avoid long scrollback.  clr_eol ensures
        # that there is no trailing garbage when a shorter line
        # overwrites a longer line.
        print '%s%s\r' % (clr_eol, section),
      chunk = []
    if not section or has_interesting_info:
      print line
    else:
      length = len(chunk)
      if length == 2 and line != 'Check dependencies':
        has_interesting_info = True
      elif (length == 1 or length == 3) and line:
        has_interesting_info = True
      elif length > 3:
        has_interesting_info = True
      if has_interesting_info:
        print '\n'.join(chunk)
        chunk = []
      else:
        chunk.append(line)
  if not is_empty_chunk(chunk):
    print '\n'.join(chunk)


def NotifyBuildDone(build_config, success):
  if not success:
    print "BUILD FAILED"

  sys.stdout.flush()

  if success:
    message = 'Build succeeded.'
  else:
    message = 'Build failed.'
  title = build_config

  command = None
  if HOST_OS == 'macos':
    # Use AppleScript to display a UI non-modal notification.
    script = 'display notification  "%s" with title "%s" sound name "Glass"' % (
      message, title)
    command = "osascript -e '%s' &" % script
  elif HOST_OS == 'linux':
    if success:
      icon = 'dialog-information'
    else:
      icon = 'dialog-error'
    command = "notify-send -i '%s' '%s' '%s' &" % (icon, message, title)

  if command:
    # Ignore return code, if this command fails, it doesn't matter.
    os.system(command)


def Main():
  utils.ConfigureJava()
  # Parse the options.
  parser = BuildOptions()
  (options, args) = parser.parse_args()
  if not ProcessOptions(options, args):
    parser.print_help()
    return 1
  # Determine which targets to build. By default we build the "all" target.
  if len(args) == 0:
    if HOST_OS == 'macos':
      targets = ['All']
    else:
      targets = ['all']
  else:
    targets = args

  filter_xcodebuild_output = False
  # Build all targets for each requested configuration.
  for target in targets:
    for target_os in options.os:
      for mode in options.mode:
        for arch in options.arch:
          os.environ['DART_BUILD_MODE'] = mode
          build_config = utils.GetBuildConf(mode, arch, target_os)
          if HOST_OS == 'macos':
            filter_xcodebuild_output = True
            project_file = 'dart.xcodeproj'
            if os.path.exists('dart-%s.gyp' % CurrentDirectoryBaseName()):
              project_file = 'dart-%s.xcodeproj' % CurrentDirectoryBaseName()
            args = ['xcodebuild',
                    '-project',
                    project_file,
                    '-target',
                    target,
                    '-configuration',
                    build_config,
                    'SYMROOT=%s' % os.path.abspath('xcodebuild')
                    ]
          elif HOST_OS == 'win32':
            project_file = 'dart.sln'
            if os.path.exists('dart-%s.gyp' % CurrentDirectoryBaseName()):
              project_file = 'dart-%s.sln' % CurrentDirectoryBaseName()
            if target == 'all':
              args = [options.devenv + os.sep + options.executable,
                      '/build',
                      build_config,
                      project_file
                     ]
            else:
              args = [options.devenv + os.sep + options.executable,
                      '/build',
                      build_config,
                      '/project',
                      target,
                      project_file
                     ]
          else:
            make = 'make'
            if HOST_OS == 'freebsd':
              make = 'gmake'
              # work around lack of flock
              os.environ['LINK'] = '$(CXX)'
            args = [make,
                    '-j',
                    options.j,
                    'BUILDTYPE=' + build_config,
                    ]
            if target_os != HOST_OS:
              args += ['builddir_name=' + utils.GetBuildDir(HOST_OS, target_os)]
            if options.verbose:
              args += ['V=1']

            args += [target]

          toolchainprefix = options.toolchain
          toolsOverride = SetTools(arch, target_os, toolchainprefix)
          if toolsOverride:
            for k, v in toolsOverride.iteritems():
              args.append(  k + "=" + v)
              if options.verbose:
                print k + " = " + v
            if not os.path.isfile(toolsOverride['CC.target']):
              if arch == 'arm':
                print arm_cc_error
              else:
                print "Couldn't find compiler: %s" % toolsOverride['CC.target']
              return 1


          print ' '.join(args)
          process = None
          if filter_xcodebuild_output:
            process = subprocess.Popen(args,
                                       stdin=None,
                                       bufsize=1, # Line buffered.
                                       stdout=subprocess.PIPE,
                                       stderr=subprocess.STDOUT)
            FilterEmptyXcodebuildSections(process)
          else:
            process = subprocess.Popen(args, stdin=None)
          process.wait()
          if process.returncode != 0:
            NotifyBuildDone(build_config, success=False)
            return 1
          else:
            NotifyBuildDone(build_config, success=True)

  return 0


if __name__ == '__main__':
  sys.exit(Main())
