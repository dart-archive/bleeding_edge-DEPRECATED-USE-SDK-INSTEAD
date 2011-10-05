#!/usr/bin/env python
# Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

# This script builds and then uploads the Swarm app to AppEngine,
# where it is accessible by visiting http://dart.googleplex.com.
import os
import subprocess
import sys

from os.path import abspath, basename, dirname, exists, join, split
import base64, re, os, shutil, subprocess, sys, tempfile, optparse

SWARM_PATH = dirname(abspath(__file__))
CLIENT_PATH = dirname(dirname(SWARM_PATH))
CLIENT_TOOLS_PATH = join(CLIENT_PATH, 'tools')

# Add the client tools directory so we can find htmlconverter.py.
sys.path.append(CLIENT_TOOLS_PATH)
import htmlconverter

HTML_FILES = ['swarm.html']

def convertOne(infile, options):
  outfile = join('outcode', basename(infile))
  print 'converting %s to %s' % (infile, outfile)

  # TODO(jmesserly): this is a workaround for an OOM error in DartC
  # See bug 5393264
  if options.optimize:
    os.putenv('DART_JVMARGS', '-Xmx512m')

  if 'dart' in options.target:
    htmlconverter.convertForDartium(infile, outfile.replace('.html', '-dart.html'), False)
  if 'js' in options.target:
    htmlconverter.convertForChromium(infile, options.optimize, outfile.replace('.html', '-js.html'), False)


def Flags():
  """ Consturcts a parser for extracting flags from the command line. """
  result = optparse.OptionParser()
  result.add_option("-i", "--inputs",
      help="The input html versions to generate from",
      metavar="[swarm]",
      default='swarm,')
  result.add_option("-t", "--target",
      help="The target html to generate",
      metavar="[js,dart]",
      default='js,dart')
  result.add_option("--optimize",
      help="Use optimizer in dartc",
      default=False,
      action="store_true")
  #result.set_usage("update.py input.html -o OUTDIR -t chromium,dartium")
  return result

def main():
  os.chdir(CLIENT_PATH) # TODO(jimhug): I don't like chdir's in scripts...

  parser = Flags()
  options, args = parser.parse_args()
  #if len(args) < 1 or not options.out or not options.target:
  #  parser.print_help()
  #  return 1

  SWARM_PATH = 'samples/swarm'
  for file in HTML_FILES:
    infile = join(SWARM_PATH, file)
    convertOne(infile, options)

if __name__ == '__main__':
  main()
