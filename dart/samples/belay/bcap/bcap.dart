// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('bcap');
#import('src/impl_bcap.dart');
#source('src/BcapUtil.dart');

// This library implements a Dart binding of the Bcap protocol.

// When running in a specific environment, you will probably want a
// different Bcap library that offers more specific features tailored
// for that environment. In particular:
//    When running in the browser:       bcap_client.dart
//    When running via the fling server: bcap_fling.dart

class BcapError {
  final int status;
  final String message;
  
  const BcapError([int this.status = 500, String this.message = '']);
  
  static final BcapError badRequest = const BcapError(400, 'Bad Request');
  static final BcapError notFound = const BcapError(404, 'Not Found');
  static final BcapError methodNotAllowed = const BcapError(405, 'Method Not Allowed');
  static final BcapError internalServerError = const BcapError(500, 'Internal Server Error');
}


typedef void Success(/* JSON */ result);
typedef void Failure(BcapError err);



interface Bcap {
  // Bcaps are always serializable
  String serialize();

  void get_([Success sk, Failure fk]);
  void put(/* JSON */ data, [Success sk, Failure fk]);
  void post(/* JSON */ data, [Success sk, Failure fk]);
  void delete([Success sk, Failure fk]);
}

class BcapHandler {
  // A handler is a client written implementation of a Bcap
  BcapHandler() {}
  
  // This base implementation returns methodNotAllowed for all methods
  void get_(Success sk, Failure fk) { fk(defaultError()); }
  void put(var data, Success sk, Failure fk) { fk(defaultError()); }
  void post(var data, Success sk, Failure fk) { fk(defaultError()); }
  void delete(Success sk, Failure fk) { fk(defaultError()); }
  
  BcapError defaultError() => BcapError.methodNotAllowed;
}


typedef BcapSyncFunction(var data);

class BcapFunctionHandler extends BcapHandler {
  // A wrapper around a synchronoous function that provides a handler
  final BcapSyncFunction f;
  BcapFunctionHandler(BcapSyncFunction this.f) : super() {}

  void get_(Success sk, Failure fk) { sk(f(null)); }
  void post(var data, Success sk, Failure fk) { sk(f(data)); }
  void put(var data, Success sk, Failure fk) { sk(f(data)); }
}

typedef BcapAsyncFunction(var data, Success sk, Failure fk);

class BcapAsyncFunctionHandler extends BcapHandler {
  // A wrapper around an asynchronoous function that provides a handler
  final BcapAsyncFunction f;
  BcapAsyncFunctionHandler(BcapAsyncFunction this.f) : super() {}

  void get_(sk, fk) { f(null, sk, fk); }
  void post(arg, sk, fk) { f(arg, sk, fk); }
  void put(arg, sk, fk) { f(arg, sk, fk); }
}


typedef void SuccessI(String result);
typedef void FailureI(BcapError err);

interface BcapServerInterface {
  void invoke(String ser, String method, String data, SuccessI ski, FailureI fki);
}

typedef BcapHandler Reviver(String key);
typedef BcapServerInterface Resolver(String instID);
typedef void SaveState(String state);

interface BcapServer extends BcapServerInterface factory BcapServerImpl {
  BcapServer(String instanceID, [String snapshot, SaveState saveState]);

  void setReviver(Reviver newReviver);
  void setResolver(Resolver newResolver);

  Object dataPreProcess(Object data);
  Object dataPostProcess(Object data);

  String snapshot();

  Bcap grant(BcapHandler handler, [String key]);
  Bcap grantFunc(BcapSyncFunction f, [String key]);
  Bcap grantAsyncFunc(BcapAsyncFunction f, [String key]);

  Bcap grantKey(String key);
  Bcap restore(String ser);

  void revoke(String ser);
  void revokeAll();
}

