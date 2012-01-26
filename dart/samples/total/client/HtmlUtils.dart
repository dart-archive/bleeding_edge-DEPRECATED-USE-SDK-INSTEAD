// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class HtmlUtils {

  // Strips the 'px' (pixels) suffix from a string CSS value and parses the
  // result as an integer.
  static int fromPx(String px) => Math.parseInt(px.substring(0, px.length - 2));

  // Workaround until String.replaceAll is functional
  static String quoteHtml(String s) {
    StringBuffer sb = new StringBuffer();
    int last = 0;
    for (int i = 0; i < s.length; i++) {
      switch (s[i]) {
      case "<":
        sb.add(s.substring(last, i));
        sb.add("&lt;");
        last = i + 1;
        break;
      case ">":
        sb.add(s.substring(last, i));
        sb.add("&gt;");
        last = i + 1;
        break;
      case "&":
        sb.add(s.substring(last, i));
        sb.add("&amp;");
        last = i + 1;
        break;
      case "\"":
        sb.add(s.substring(last, i));
        sb.add("&quot;");
        last = i + 1;
        break;
      }
    }
    sb.add(s.substring(last, s.length));
    return sb.toString();
  }

  static void setIntegerProperty(Element element, String property, int value, String units) {
    element.style.setProperty(property, "${value}${units}", "");
  }

  // Appends 'px' (pixels) to an integer for use in CSS values.
  static String toPx(int length) => "${length}px";
}
