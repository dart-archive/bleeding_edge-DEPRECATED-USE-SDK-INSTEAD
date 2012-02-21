// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Util {
  static void abs(HTMLElement elem) {
    elem.style.setProperty("position", 'absolute');
  }

  static void rel(HTMLElement elem) {
    elem.style.setProperty("position", 'relative');
  }

  static void pos(HTMLElement elem, double x, double y) {
    elem.style.setProperty("left", x.toString() + "PX");
    elem.style.setProperty("top", y.toString() + "PX");
  }

  static void posSize(HTMLElement elem, double l, double t, double r, double b) {
    pos(elem, l, t);
    elem.style.setProperty("right", r.toString() + "PX");
    elem.style.setProperty("bottom", b.toString() + "PX");
  }

  static ClientRect bounds(HTMLElement elem) {
    return elem.getBoundingClientRect();
  }

  static void opacity(HTMLElement elem, double value) {
    elem.style.setProperty("opacity", value.toString());
  }

  static int currentTimeMillis() {
    return (new Date.now()).value;
  }

  static int clientWidth() {
    return window.document.documentElement.clientWidth;
  }

  static int clientHeight() {
    return window.document.documentElement.clientHeight;
  }
}
