// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library observatory_application_element;

import 'observatory_element.dart';
import 'package:observatory/app.dart';
import 'package:polymer/polymer.dart';

/// Main application tag. Responsible for instantiating an instance of
/// [ObservatoryApplication] which is passed declaratively to all child
/// elements.
@CustomTag('observatory-application')
class ObservatoryApplicationElement extends ObservatoryElement {
  @published bool devtools = false;
  @observable ObservatoryApplication app;

  ObservatoryApplicationElement.created() : super.created() {
    if (devtools) {
      app = new ObservatoryApplication.devtools();
    } else {
      app = new ObservatoryApplication();
    }
  }
}
