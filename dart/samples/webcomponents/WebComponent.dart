// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

abstract class WebComponent {
  abstract Element get element();
  abstract void created();
  abstract void inserted();
  abstract void attributeChanged(
      String name, String oldValue, String newValue);
  abstract void removed();
}
