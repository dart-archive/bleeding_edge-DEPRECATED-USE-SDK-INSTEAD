#!/usr/bin/env python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

"""Command line wrapper to run the frog compiler under the Dart VM"""

# TODO(jimhug): This is a temporary hack to enable running on the VM.  We need
#   the ability to get command-line arguments, to return exit codes, and to
#   communicate with spawned processes in the VM for this to go away.


import optparse
import os
import platform
import tempfile
import shutil
import subprocess
import sys


from os.path import dirname, join, realpath, exists, basename, relpath

HOME = dirname(realpath(__file__))
sys.path.append(join(HOME, os.pardir, 'tools'))
import utils

HTML = '''<html>
  <head><title>Frog</title><head>
  <body>
    <script type="application/javascript" src="out.js"></script>
  </body>
</html>
'''


# Returns the path to the Dart test runner (executes the .dart file).
def GetDartRunner(mode, arch, component):
  build_root = utils.GetBuildRoot(utils.GuessOS(), mode, arch)
  if component == 'frog':
    return os.path.join(build_root, 'frog', 'bin', 'frog')
  else:
    suffix = ''
    if utils.IsWindows():
      suffix = '.exe'
    return os.path.join(build_root, 'dart') + suffix


def GetDart():
  # Get the release version.
  return GetDartRunner('release', 'ia32', 'vm')

def GetD8():
  return join(dirname(GetDart()), 'd8')

D8 = GetD8()

def execute(cmd):
  """Execute a command in a subprocess. """
  try:
    proc = subprocess.Popen(cmd, stdout=sys.stdout, stderr=sys.stderr,
        env=os.environ)
    return proc.wait()
  except Exception as e:
    print 'Exception when executing: ' + ' '.join(cmd)
    print e
    return 1

def parseOptions(args):
  optionParser = optparse.OptionParser()

  optionParser.add_option('--work', dest='workdir',
      default=None,
      metavar='DIR', help='Directory where temporary files are created.')

  # Meta-flag for VM running compiler, probably want more options here.
  optionParser.add_option('--vm_flags',
    # TODO(jimhug): Make it easier to enable and disable this for tests.
    #default='--enable_type_checks --enable_asserts',
    default='',
    help='Flags to pass to the VM that is running frog itself.')

  optionParser.add_option('--vm',
    default=GetDart(),
    help='The location of the VM.')

  optionParser.add_option('--js_cmd',
    default = '%s --crankshaft' % D8,
    metavar='FILE', help='The shell cmd to use to run output JS code.')

  optionParser.add_option('--keep_files',
      action='store_true', help='Do not remove temporary files.')

  # TODO(vsm): Hook in HtmlConverter.
  optionParser.add_option('--html',
      action='store_true', help='Invoke this in the browser instead of d8.')
  optionParser.add_option('--browser',
      default = None,
      metavar='FILE', help='The browser to use to run output HTML.')

  optionParser.add_option('--verbose',
      help='Verbose output', default=False, action='store_true')

  optionParser.set_usage("frog.py <dart-script-file> [<dart-options>]")
  return optionParser.parse_args(args)


def main(args):
  if '--' in args:
    index = args.index('--')
    pythonArgs = args[1:index]
    dartArgs = args[index + 1:]
  else:
    pythonArgs = []
    dartArgs = args[1:]

  options, extraArgs = parseOptions(pythonArgs)
  if options.verbose:
    print ("dartArgs=%s pythonArgs=%s extraArgs=%s" %
           (' '.join(dartArgs), ' '.join(pythonArgs), ' '.join(extraArgs)))

  if len(extraArgs) != 0:
    optionParser.print_help()
    return 1

  dart = options.vm
  if not exists(dart):
    print "Dart VM not built. Please run the following command:"
    build_file = relpath(join(HOME, os.pardir, 'tools', 'build.py'))
    print ' ' + build_file + ' -m release'
    return 1

  return compileAndRun(options, dartArgs, dart)


def ensureJsEngine(options):
  if not exists(D8):
    print "No engine available for running JS code."
    print "See frog/README.txt for instructions."
    return 1
  return 0

def compileAndRun(options, args, dart):
  jsArgs = []
  for i in range(len(args)):
    if args[i].endswith('.dart'):
      jsArgs = args[i+1:]
      args = args[:i+1]
      break

  outfile_given = False
  execute_output = True
  for i in range(len(args)):
    if args[i].startswith('--out'):
      outfile_given = True
      outfile = args[i][6:]
      execute_output = False
      break;
    if args[i] == '--compile-only':
      execute_output = False
      break;

  if options.verbose: print "jsArgs %s" % ' '.join(jsArgs);

  workdir = options.workdir
  cleanup = False
  if not workdir:
    if not options.html:
      workdir = tempfile.mkdtemp()
      if not options.keep_files:
        cleanup = True
    else:
      # A persistent location for the browser to load.
      workdir = 'html'
      if not os.path.exists(workdir):
        os.mkdir(workdir)

  if not outfile_given:
    outfile = join(workdir, 'out.js')
    args = ['--out=%s' % outfile] + args

  outhtml = join(workdir, 'out.html')

  browser = options.browser
  if not browser:
    if platform.system() == 'Darwin':
      # Use the default browser on the Mac
      browser = 'Open'
    else:
      # TODO(vsm): This is only available on Goobuntu.
      browser = 'google-chrome'

  args = ['--libdir=%s/lib' % HOME] + args

  compiler_cmd = [dart]
  if options.vm_flags:
    compiler_cmd.extend(options.vm_flags.split(' '))
  compiler_cmd.append(join(HOME, 'frogc.dart'))
  compiler_cmd.extend(args)
  exit_code = execute(compiler_cmd)
  if exit_code:
    if options.verbose: print ("cmd exited with status %d" % exit_code)
    if cleanup: shutil.rmtree(workdir)
    if exit_code < 0:
      print("VM exited with signal %d" % (-exit_code))
      # TODO(ahe): Using 253 to signal a crash to test.dart.
      return 253
    return exit_code

  result = 0
  if execute_output:
    if not options.html:
      if ensureJsEngine(options) != 0:
        return 1
      js_cmd = options.js_cmd
      result = execute(js_cmd.split(' ') + [outfile] + jsArgs)
    else:
      f = open(outhtml, 'w')
      f.write(HTML)
      f.close()
      result = execute([browser, outhtml])
  elif outfile_given:
    print 'Compilation succeded. Code generated in: %s' % outfile
  else:
    print 'Compilation succeded.'

  if cleanup: shutil.rmtree(workdir)
  if result != 0:
    return 1
  return 0

if __name__ == '__main__':
  sys.exit(main(sys.argv))
