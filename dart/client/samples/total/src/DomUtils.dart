// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Utilities for [Element]s.
 */
class DomUtils {

  /**
   * Returns an element's absolute bottom coordinate in the document's coordinate system.
   */
  static int getAbsoluteBottom(Element elem) {
    return getAbsoluteTop(elem) + elem.offsetHeight;
  }

  /**
   * Returns an element's absolute left coordinate in the document's coordinate system.
   */
  static int getAbsoluteLeft(Element elem) {
    int left = 0;
    Element curr = elem;
    while (curr.offsetParent != null) {
      left -= curr.scrollLeft;
      curr = curr.parent;
    }
    while (elem != null) {
      left += elem.offsetLeft;
      elem = elem.offsetParent;
    }
    return left;
  }

  /**
   * Returns an element's absolute right coordinate in the document's coordinate system.
   */
  static int getAbsoluteRight(Element elem) {
    return getAbsoluteLeft(elem) + elem.offsetWidth;
  }

  /**
   * Returns an element's absolute top coordinate in the document's coordinate system.
   */
  static int getAbsoluteTop(Element elem) {
    int top = 0;
    Element curr = elem;
    while (curr.offsetParent != null) {
      top -= curr.scrollTop;
      curr = curr.parent;
    }
    while (elem != null) {
      top += elem.offsetTop;
      elem = elem.offsetParent;
    }
    return top;
  }

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
