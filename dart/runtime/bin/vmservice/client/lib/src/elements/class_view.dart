// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library class_view_element;

import 'dart:async';
import 'observatory_element.dart';
import 'package:observatory/service.dart';
import 'package:polymer/polymer.dart';

@CustomTag('class-view')
class ClassViewElement extends ObservatoryElement {
  @published ServiceMap cls;
  ClassViewElement.created() : super.created();

  Future<ServiceObject> eval(String text) {
    return cls.isolate.get(
        cls.id + "/eval?expr=${Uri.encodeComponent(text)}");
  }

  void refresh(var done) {
    cls.reload().whenComplete(done);
  }
}
