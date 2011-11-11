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


from os.path import dirname, join, realpath, exists

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

def GetDart():
  # Try a release version
  dart = utils.GetDartRunner('release', 'ia32', 'vm')
  if exists(dart): return dart
  # Try at the top level
  dart = join(os.pardir, dart)
  if exists(dart): return dart

  # Try a debug version
  dart = utils.GetDartRunner('debug', 'ia32', 'vm')
  if exists(dart): return dart
  # Try at the top level
  dart = join(os.pardir, dart)
  return dart

def GetD8():
  system = utils.GuessOS()
  d8 = join(utils.GetBuildRoot(system, 'release', 'ia32'), 'd8')
  if exists(d8): return d8
  # Try at the top level
  d8 = join(os.pardir, d8)
  if exists(d8): return d8

  # Try a debug version
  d8 = join(utils.GetBuildRoot(system, 'debug', 'ia32'), 'd8')
  if exists(d8): return d8
  # Try at the top level
  d8 = join(os.pardir, d8)
  return d8

D8 = GetD8()

def execute(cmd):
  """Execute a command in a subprocess. """
  try:
    pipe = subprocess.Popen(cmd, stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    out, err = pipe.communicate()
    if out is not None: sys.stdout.write(out)
    if err is not None: sys.stderr.write(err)
    return pipe.returncode
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

  optionParser.add_option('--js_cmd',
    default = 'node --crankshaft', # node is really slow without this.
    metavar='FILE', help='The shell cmd to use to run output JS code.')

  optionParser.add_option('--keep_files',
      action='store_true', help='Do not remove temporary files.')

  # TODO(vsm): Hook in HtmlConverter.
  optionParser.add_option('--html',
      action='store_true', help='Invoke this in the browser instead of node/d8.')
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

  dart = GetDart()
  if not exists(dart):
    print("Dart VM not configured in %s," % dart),
    print "run the following command from the dart directory"
    print " tools/build.py -m release dart_bin"
    return 1

  if subprocess.call("node --help >/dev/null 2>&1", shell=True):
    if not exists(D8):
      print "No engine available for running JS code."
      print "See frog/README.txt for instructions."
      return 1
    elif 'node' in options.js_cmd:
      options.js_cmd = D8

  return compileAndRun(options, dartArgs, dart)


def compileAndRun(options, args, dart):
  nodeArgs = []
  for i in range(len(args)):
    if args[i].endswith('.dart'):
      nodeArgs = args[i+1:]
      args = args[:i+1]
      break

  outfile_given = False
  for i in range(len(args)):
    if args[i].startswith('--out'):
      outfile_given = True
      break;

  if options.verbose: print "nodeArgs %s" % ' '.join(nodeArgs);

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
    return exit_code

  result = 0
  if not outfile_given:
    if not options.html:
      js_cmd = options.js_cmd
      result = execute(js_cmd.split(' ') + [outfile] + nodeArgs)
    else:
      f = open(outhtml, 'w')
      f.write(HTML)
      f.close()
      result = execute([browser, outhtml])

  if cleanup: shutil.rmtree(workdir)
  if result != 0:
    return 1
  return 0

if __name__ == '__main__':
  sys.exit(main(sys.argv))
