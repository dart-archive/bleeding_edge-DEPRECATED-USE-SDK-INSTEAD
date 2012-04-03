// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Util {
  static void abs(Element elem) {
    elem.style.position = 'absolute';
  }

  static void rel(Element elem) {
    elem.style.position = 'relative';
  }

  static void pos(Element elem, double x, double y) {
    elem.style.left = "${x}PX";
    elem.style.top = "${y}PX";
  }

  static void posSize(Element elem, double l, double t, double r, double b) {
    pos(elem, l, t);
    elem.style.right = "${r}PX";
    elem.style.bottom = "${b}PX";
  }

  static ClientRect bounds(Element elem) {
    return elem.$dom_getBoundingClientRect();
  }

  static void opacity(Element elem, double value) {
    elem.style.opacity = value.toString();
  }

  static int currentTimeMillis() {
    return (new Date.now()).value;
  }

  static int clientWidth() {
    return window.innerWidth;
  }

  static int clientHeight() {
    return window.innerHeight;
  }
}
