// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of vmservice_io;

String detectMimeType(String name) {
  var extensionStart = name.lastIndexOf('.');
  var extension = name.substring(extensionStart+1);
  switch (extension) {
    case 'html':
      return 'text/html; charset=UTF-8';
    case 'dart':
      return 'application/dart; charset=UTF-8';
    case 'js':
      return 'application/javascript; charset=UTF-8';
    case 'css':
      return 'text/css; charset=UTF-8';
    case 'gif':
      return 'image/gif';
    case 'png':
      return 'image/png';
    case 'jpg':
      return 'image/jpeg';
    case 'jpeg':
      return 'image/jpeg';
    case 'svg':
      return 'image/svg+xml';
    default:
      return 'text/plain';
  }
}


class Resource {
  final String name;
  final String mimeType;
  final List<int> data;
  Resource(this.name, this.mimeType, this.data);
  static final Map<String, Resource> resources = new Map<String, Resource>();
}


void _addResource(String name, List<int> data) {
  var mimeType = detectMimeType(name);
  Resource resource = new Resource(name, mimeType, data);
  Resource.resources[name] = resource;
}

void _triggerResourceLoad() native "VMServiceIO_TriggerResourceLoad";
