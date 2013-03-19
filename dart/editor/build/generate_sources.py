#!/usr/bin/env python
# Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
# for details. All rights reserved. Use of this source code is governed by a
# BSD-style license that can be found in the LICENSE file.

import StringIO
import os
import sys

from os.path import join


def _print_gypi_files(out, name, files):
  out.write("    '%s': [\n" % name)
  for filename in files:
    out.write('''      r'%s',%s''' % (filename, '\n'))
  out.write("    ],\n")


def _close(file_name, output):
  if not isinstance(output, StringIO.StringIO):
    output.close()
    return
  new_text = output.getvalue()
  output.close()
  with open(file_name, 'r') as f:
    old_text = f.read()
  if old_text == new_text:
    return
  with open(file_name, 'w') as f:
    f.write(new_text)


def _make_output(fileName):
  if os.path.exists(fileName):
    return StringIO.StringIO()
  else:
    return file(fileName, 'w')


def GenerateGypiFile(sources, outputFile):
  gypi = _make_output(outputFile)
  gypi.write("{\n  'variables': {\n")
  _print_gypi_files(gypi, 'sources', sources)
  gypi.write("  },\n}\n")
  _close(outputFile, gypi)


def _list_sources(path, extension):
  sources = []
  for fullpath, dirs, filenames in os.walk(path):
    # Skip directories that start with a dot, such as ".svn".
    remove_me = [d for d in dirs if d.startswith('.')]
    for d in remove_me:
      dirs.remove(d)

    for filename in filenames:
      if (filename.endswith(extension)):
        sources.append(os.path.relpath(join(fullpath, filename),
                                       start='editor/build/generated/'))
  sources.sort()
  return sources


def _list_pkgs(path):
  result = None
  for dirpath, dirnames, filenames in os.walk(path):
    result = dirnames[:] # Copy array.
    del dirnames[:] # Stop recursion by clearing array.
  return result


def _split_sources(path, extension):
  result = []
  count = 100
  segment = -1
  sources = None
  for source in _list_sources(path, extension):
    if count == 100:
      count = 0
      segment += 1
      sources = []
      result.append(('chunk%s' % segment, sources))
    sources.append(source)
    count += 1
  return result

def _print_pkg_action(pkg, out):
  print >> out, """
        {
          'includes': [
            '%(pkg)s_sources.gypi',
          ],
          'action_name': '%(pkg)s_action',
          'inputs': [
            '%(pkg)s_sources.gypi',
            '<@(sources)',
          ],
          'outputs': [
            '<(SHARED_INTERMEDIATE_DIR)/editor_deps/%(pkg)s.stamp',
          ],
          'action': [
            'python',
            '../truncate_files.py',
            '<@(_outputs)',
          ],
          'message': 'Creating %(pkg)s time stamp.',
        },
""" % {'pkg':pkg}


def Main(argv):
  # move up to the parent 'dart' directory
  base_directory = join(os.path.dirname(argv[0]), '..', '..')
  os.chdir(base_directory)

  pkg_dir = join('editor', 'tools', 'plugins')
  out_dir = join('editor', 'build', 'generated')

  if not os.path.exists(out_dir):
    os.mkdir(out_dir)

  with open(join(out_dir, 'editor_deps.gyp'), 'w') as gyp:
    stamps = []
    print >> gyp, """
{
  'targets': [
    {
      'target_name': 'editor_deps',
      'type': 'none',
      'actions': ["""

    for pkg, sources in _split_sources(pkg_dir, '.java'):
      GenerateGypiFile(sources, join(out_dir, '%s_sources.gypi' % pkg))
      _print_pkg_action(pkg, gyp)
      stamps.append("'<(SHARED_INTERMEDIATE_DIR)/editor_deps/%s.stamp'" % pkg)

    print >> gyp, """
        {
          'action_name': 'editor_stamp_action',
          'inputs': [
            %s,
          ],
          'outputs': [
            '<(SHARED_INTERMEDIATE_DIR)/editor_deps/editor.stamp',
          ],
          'action': [
            'python',
            '../truncate_files.py',
            '<@(_outputs)',
          ],
          'message': 'Creating editor time stamp.',
        },
      ],
    },
  ],
}
""" % (",\n".join(stamps))

if __name__ == '__main__':
  sys.exit(Main(sys.argv))
