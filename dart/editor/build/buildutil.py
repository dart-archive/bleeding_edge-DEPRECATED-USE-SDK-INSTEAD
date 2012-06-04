#!/usr/bin/python

"""Copyright (c) 2012 The Chromium Authors. All rights reserved.

Use of this source code is governed by a BSD-style license that can be
found in the LICENSE file.

Eclipse Dart Editor util functions.
"""
import os


class BuildUtil(object):
  """Utility methods for doing the build."""
  _project = 'com.google.dart.tools.deploy.feature_releng'
  _buildout = None
  _buildos = None
  _dartpath = None
  _sdkpath = None
  _pathending = 'ReleaseIA32'

  def __init__(self, buildos, buildout, dartpath):
    """Initialize this class.

    Args:
      buildos: the os the build is running under
      buildout: the build out directory
      dartpath: the path to the root of the dart source tree
    """
    self._buildos = buildos
    self._buildout = buildout
    self._dartpath = dartpath
    self._sdk_paths = {'macos': os.path.join('xcodebuild', 'ReleaseIA32'),
                       'win32': os.path.join('ReleaseIA32', 'dart-sdk'),
                       'linux': os.path.join('out', 'ReleaseIA32')}

  def SdkZipLocation(self):
    """Return the location of the sdk zip file."""
    relpath = ''
    checkpath = self._buildout
    if self._project in self._buildout:
      (head, tail) = os.path.split(checkpath)
      while tail not in self._project:
        if tail:
          relpath = os.path.join(tail, relpath)
        checkpath = head
        (head, tail) = os.path.split(checkpath)
    else:
      relpath = os.path.join(self._dartpath, self._sdk_paths[self._buildos])

    if self._pathending not in relpath:  #this handles local builds
      relpath = os.path.join(relpath, self._pathending)

    return os.path.join(self._dartpath, relpath)
