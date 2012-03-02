#!/usr/bin/python

import glob
import os
import os.path
import subprocess

def HtmlConvert(infile):
  (head, tail) = os.path.split(infile)
  htmlconverter = os.path.join('..', '..', '..', 'client',
                               'tools', 'htmlconverter.py')
  if head == 'tests':
    htmlconverter = os.path.join('..', htmlconverter)
    outdir = 'frog'
    os.chdir('tests')
  elif head == '':
    outdir = '.'
  else:
    raise 'Illegal input: ' + infile

  cmd = ['python', htmlconverter, tail, '-o', outdir]
  print 'Executing: ' + ' '.join(cmd)
  subprocess.call(cmd)
  if head == 'tests':
    os.chdir('..')

# Frog compile individual dom and html tests into tests/frog.
tests = glob.glob('tests/dom-*-*.html')

for test in tests:
  HtmlConvert(test)

# Frog compile driver to index-js.html.
HtmlConvert('index.html')
