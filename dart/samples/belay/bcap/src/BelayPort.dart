// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.s

typedef void BelayPortReady(BelayPort port);

class BelayPort {
  static void initialize(BelayPortReady bpReady) {
    _initialize((){ bpReady(new BelayPort()); });
  }
  
  BelayPort() { }
 
  // In order to pass structured data through the underlying MessagePort, we
  // use JSON encoding between Dart and the underlying JS native methods.
  
  void postMessage(var msg) {
    _postMessage(JSON.stringify(msg));
  }
  void setOnMessage(Function callback) {
    _setOnMessage((String str) { callback(JSON.parse(str)); });
  }
  
  static void _initialize(ready) native;
  static void _postMessage(String msg) native;
  static void _setOnMessage(Function callback) native;
}

class BelayUtil {
  static Clipboard getClipboard(MouseEvent event) {
    return LevelDom.wrapClipboard(_getDataTransfer(LevelDom.unwrap(event)));
  }
  
  static String belayEncode(String data) native;
  
  static _getDataTransfer(raw) native;
}
