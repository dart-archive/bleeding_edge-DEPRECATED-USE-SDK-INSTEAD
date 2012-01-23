// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('vm');
#import('node.dart');

class vm native "require('vm')" {
  static void runInThisContext(String code, [String filename]) native;
  static void runInNewContext(String code, [var sandbox, String filename])
    native;
  static Script createScript(String code, [String filename]) native;
  static Context createContext([sandbox]) native;
  static runInContext(String code, Context context, [String filename]) native;
}

interface Context {}

class Script native "vm.Script" {
  void runInThisContext() native;
  void runInNewContext([Map sandbox]) native;
}

