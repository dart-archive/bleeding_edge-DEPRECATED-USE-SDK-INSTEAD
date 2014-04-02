// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library vm_view_element;

import 'observatory_element.dart';
import 'package:observatory/service.dart';
import 'package:polymer/polymer.dart';

@CustomTag('vm-view')
class VMViewElement extends ObservatoryElement {
  @published VM vm;
  @published DartError error;

  VMViewElement.created() : super.created();

  void refresh(var done) {
    vm.reload().whenComplete(done);
  }
}
