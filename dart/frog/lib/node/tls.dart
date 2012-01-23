// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('tls');

// module tls

// Not implemented yet.

/*
typedef void SecureConnectionListener(CleartextStream cleartextStream);

class tls native "require('tls')" {
  static TlsServer createServer(Map<String, Object> options,
      [SecureConnectionListener secureConnectionListener]) native;
  static CleartextStream connect(int port,
      [String host, Map<String,Object> options,
      SecureConnectionListener secureConnectListener]) native;
  static SecurePair createSecurePair([Credentials credentials, bool isServer,
      bool requestCert, bool rejectUnauthorized]) native;
}

typedef void SecureListener();

class SecurePair native "require('tls').SecurePair" {
  // event secure
  void emitSecure()
    native "this.emit('secure');";
  void addListenerSecure(SecureListener listener)
    native "this.addListener('secure', listener);";
  void onSecure(SecureListener listener)
    native "this.on('secure', listener);";
  void onceSecure(SecureListener listener)
    native "this.once('secure', listener);";
  void removeListenerSecure(SecureListener listener)
    native "this.removeListener('secure', listener);";
  List<ServerCloseListener> listenersSecure()
    => new NativeListPrimitiveElement<SecureListener>(
      _listeners('secure'));
  
  CleartextStream cleartext;
  Stream encrypted;
}

class TlsStream native "require('tls').Stream" {
  
}

class TlsServer native "require('tls').Stream" {
  
}

typedef void SecureConnectListener();

class CleartextStream implements ReadWriteStream
    native "*CleartextStream" {
      
  // Event secureConnect
  void emitSecureConnect()
    native "this.emit('secureConnect');";
  void addListenerSecureConnect(SecureConnectListener listener)
    native "this.addListener('secureConnect', listener);";
  void onSecureConnect(SecureConnectListener listener)
    native "this.on('secureConnect', listener);";
  void onceSecureConnect(SecureConnectListener listener)
    native "this.once('secureConnect', listener);";
  void removeListenerSecureConnect(SecureConnectListener listener)
    native "this.removeListener('secureConnect', listener);";
  List<ServerCloseListener> listenersSecureConnect()
    => new NativeListPrimitiveElement<SecureConnectListener>(
      _listeners('secureConnect'));
  
  boolean authorized;
  String authorizationError;
  Map<String,Object> getPeerCertificate() native;
  Map<String,String> address() native;
  Map<String,String> remoteAddress;
  String remoteAddress;
  int remotePort;
}

*/
