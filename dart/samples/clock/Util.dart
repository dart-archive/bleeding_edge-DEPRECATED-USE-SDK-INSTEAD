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

  static void pos(HTMLElement elem, int x, int y) {
    elem.style.setProperty("left", x.toRadixString(10) + "PX");
    elem.style.setProperty("top", y.toRadixString(10) + "PX");
  }

  static void posSize(HTMLElement elem, int l, int t, int r, int b) {
    pos(elem, l, t);
    elem.style.setProperty("right", r.toRadixString(10) + "PX");
    elem.style.setProperty("bottom", b.toRadixString(10) + "PX");
  }

  static ClientRect bounds(HTMLElement elem) {
    return elem.getBoundingClientRect();
  }

  static void opacity(HTMLElement elem, double opacity) {
    elem.style.setProperty("opacity", opacity.toRadixString(10));
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
