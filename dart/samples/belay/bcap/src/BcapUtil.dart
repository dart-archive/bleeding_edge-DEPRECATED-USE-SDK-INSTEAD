// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class BcapUtil {

  static String uuidv4() {
    int r() => (Math.random() * 0x10000).floor().toInt();
    String s(int x) {
      var x16 = "000" + x.toRadixString(16);
      return x16.substring(x16.length - 4, x16.length);
    }
    String u() => s(r());
    String v() => s(r() & 0x0fff | 0x4000);
    String w() => s(r() & 0x3fff | 0x8000);
    
    return "${u()}${u()}-${u()}-${v()}-${w()}-${u()}${u()}${u()}";
  }

  static final nullInstID = "00000000-0000-0000-0000-000000000000";
  static final nullCapID = "00000000-0000-0000-0000-000000000000";

  static String encodeSer(String instID, String capID) {
    return "urn:x-cap:$instID:$capID";
  }

  static RegExp serRE = null;
  static List<String> decodeSer(String ser) {
/*
    TODO(mzero): RegExp's just don't seem to work

    if (serRE === null) {
      serRE = new RegExp("^urn:x-cap:([-0-9a-f]{36}):([-0-9a-f]{36})\$", "");
    }
    Match m = serRE.firstMatch(ser);
    return (m === null) ? [nullInstID, nullCapID] : [m.group(1), m.group(2)];
*/
    // TODO(mzero): remove this hack once RegExp's work
    if (ser.substring(0,10) != "urn:x-cap:" || ser.length != 83) {
      return [nullInstID, nullCapID];
    }
    var instID = ser.substring(10, 46);
    var capID = ser.substring(47, 83);
    return [instID, capID];
  }

  static String decodeInstID(String ser) { return decodeSer(ser)[0]; }
  static String decodeCapID(String ser) { return decodeSer(ser)[1]; }
  
}

class BcapLogger {
  final String module;
  BcapLogger(String this.module) { }
  
  void log(String level, x) {
    print("$module [$level] $x");
  }
  void debug(x) { log("DEBUG", x); }
  void info(x)  { log("INFO ", x); }
  void warn(x)  { log("WARN ", x); }
}

