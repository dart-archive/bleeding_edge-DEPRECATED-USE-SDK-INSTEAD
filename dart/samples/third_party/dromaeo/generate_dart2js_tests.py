#!/usr/bin/python

import glob
import os
import os.path
import platform
import re
import subprocess
import sys

SAMPLES_PATH = os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__))))
DART_PATH = os.path.dirname(SAMPLES_PATH)
TOOLS_PATH = os.path.join(DART_PATH, 'tools')

sys.path.append(TOOLS_PATH)
import utils

EXECUTABLE_MAP = {
    'frog': 'frogc',
    'dart2js': 'dart2js'
}

def Compile(source, target, compiler):
  executable = EXECUTABLE_MAP[compiler]
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
      script = '<script type="text/javascript" src="%s">' % jsname
      outfile.write(re.sub(pattern, script, line))
    else:
      outfile.write(line)

  if head == 'tests':
    os.chdir('..')

# Frog compile individual dom and html tests into tests/frog.
tests = glob.glob('tests/dom-*-*.html')

for test in tests:
  HtmlConvert(test, 'frog')
  HtmlConvert(test, 'dart2js')

# Compile driver to index-js.html.
HtmlConvert('index.html', 'dart2js')
