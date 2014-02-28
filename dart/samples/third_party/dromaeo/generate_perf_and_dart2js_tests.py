#!/usr/bin/python

import glob
import os
import os.path
import platform
import re
import subprocess
import sys

# Compile the Dart dromaeo test down to JavaScript, and also insert two scripts
# that enable us to run this test as a browser test with the Dart browser
# controller.

SAMPLES_PATH = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DART_PATH = os.path.dirname(SAMPLES_PATH)
TOOLS_PATH = os.path.join(DART_PATH, 'tools')

sys.path.append(TOOLS_PATH)
import utils

def Compile(source, target, compiler):
  executable = 'dart2js'
  binary = os.path.abspath(os.path.join(DART_PATH,
                                        utils.GetBuildRoot(utils.GuessOS(),
                                                           'release', 'ia32'),
                                        'dart-sdk', 'bin', executable))

  cmd = [binary, '--out=' + target]
  cmd.append(source)
  print 'Executing: ' + ' '.join(cmd)
  if platform.system() == "Windows":
    subprocess.call(cmd, shell=True)
  else:
    subprocess.call(cmd)

def HtmlConvert(infile, compiler):
  (head, tail) = os.path.split(infile)

  if head == 'tests':
    outdir = compiler
    os.chdir('tests')
    if not os.path.exists(outdir):
      os.makedirs(outdir)
  elif head == '':
    outdir = '.'
  else:
    raise 'Illegal input: ' + infile

  pattern = r'<script type="application/dart" src="([\w-]+).dart">'
  infile = open(tail, 'r')
  outfilename = os.path.join(outdir, tail.replace('.html', '-js.html'))
  outfile = open(outfilename, 'w')

  print 'Converting %s to %s' % (tail, outfilename)
  for line in infile:
    result = re.search(pattern, line)
    if result:
      testname = result.group(1)
      dartname = testname + '.dart'
      jsname = '%s.%s.js' % (testname, compiler)
      outname = os.path.join(outdir, jsname)
      Compile(dartname, outname, compiler)
      script = '<script type="text/javascript" src="%s" defer>' % jsname
      outfile.write(re.sub(pattern, script, line))
    else:
      outfile.write(line)

  if head == 'tests':
    os.chdir('..')

# Compile individual html tests.
tests = glob.glob('tests/dom-*-html.html')

for test in tests:
  HtmlConvert(test, 'dart2js')

# Update index.html to run as a performance test with the dart browser
# controller. Output the result as index-dart.html.
with open('index.html', 'r') as infile:
  with open('index-dart.html', 'w') as outfile:
    for line in infile:
      if re.search('<script src="../../../pkg/browser/lib/dart.js">', line):
        line += ('  <script src="../../../tools/testing/dart/test_controller' +
            '.js"></script>\n  <script src="../../../tools/testing/dart/' +
            'perf_test_controller.js"></script>\n')
      outfile.write(line)

# Compile driver to index-js.html.
HtmlConvert('index-dart.html', 'dart2js')
os.rename('index-dart-js.html', 'index-js.html')
