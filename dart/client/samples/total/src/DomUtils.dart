// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Utilities for [Element]s.
 */
class DomUtils {
  /**
   * Removes a property on the given element's style.
   */
  static void removeStyleProperty(Element elem, String name) {
    elem.style.removeProperty(name);
  }

  /**
   * Sets a property on the given element's style.
   */
  static void setStyleProperty(Element elem, String name, String value) {
    elem.style.setProperty(name, value);
  }

  /**
   * Sets an integer property (with optional unit) on the given element's style.
   */
  static void setStylePropertyInt(Element elem, String name, int value, [String unit = ""]) {
    setStyleProperty(elem, name, value.toString() + unit);
  }
}
