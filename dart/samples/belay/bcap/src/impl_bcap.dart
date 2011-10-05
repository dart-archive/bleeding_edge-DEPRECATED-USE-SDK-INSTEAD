// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('impl_bcap');
#import('../../../../client/json/json.dart');
#import('../bcap.dart');

// This is the implementation of the base Bcap protocol.

interface BcapInterface {
  // Internally, Bcap implemetnations are invoked with this interface, where
  // the data is "processed: (serialized).
  void invoke(String method, String data, SuccessI ski, FailureI fki);
}


class BcapImpl implements Bcap {
  // This server's implementation of a Bcap transforms the invocation into an
  // internal form that:
  // a) handles request and result data in "processed" (serialized) form
  // b) insulates the caller from possible errors at the implementation
  // c) wraps sk & fk to achieve both a & b
  
  final String ser;
  final BcapServerImpl server;
  BcapImpl(String this.ser, BcapServerImpl this.server) {}
  
  String serialize() { return ser; }
  
  static void defaultFK(BcapError err) {}
  static void defaultSK(String result) {}

  void invoke(String method, data, Success sk, Failure fk) {
    String serData;
    if (method == "PUT" || method == "POST") {
      serData = server.dataPreProcess(data);
    } else {
      if (data !== null) {
        throw ("Capability.invoke $method called with request data: $data");
      }
      serData = "";
    }
    SuccessI ski =
      (sk === null)
        ? defaultSK
        : (method == "GET" || method == "POST")
            ? (String result){ sk(server.dataPostProcess(result)); }
            : (String result){ sk(null); };

    FailureI fki = (fk !== null) ? fk : defaultFK;
    
    server.privateInvoke(ser, method, serData, ski, fki);
  }

  void get_(sk = null, fk = null) { invoke("GET", null, sk, fk); }
  void put(arg, sk = null, fk = null) { invoke("PUT", arg, sk, fk); }
  void post(arg, sk = null, fk = null) { invoke("POST", arg, sk, fk); }
  void delete(sk = null, fk = null) { invoke("DELETE", null, sk, fk); }

  bool operator ==(Object other) {
    if (other is Bcap) {
      Bcap cap = other;
      return serialize() == cap.serialize();
    } else {
      return false;
    }
  }
}





class BcapHandlerInterface implements BcapInterface {
  // These object's respond to the internal form of an invocation and call a
  // handler's methods to implement it. It insulates the handler from possibly
  // malformed internal invocations.
  
  final BcapServerImpl server;
  final BcapHandler handler;

  BcapHandlerInterface(BcapServerImpl this.server, BcapHandler this.handler) { }

  void invoke(String method, String data, SuccessI ski, FailureI fki) {
    var bcapData;
    if (method == "GET" || method == "DELETE") {
      if (data !== "") {
        fki(BcapError.badRequest);
        return;
      }
    } else {
      bcapData = server.dataPostProcess(data);
    }

    Success sk;
    if (method == "PUT" || method == "DELETE") {
      sk = void _(var result) {
        if (result === null) ski(null);
        else fki(BcapError.internalServerError);
      };
    } else if (method == "GET" || method == "POST") {
      sk = void _(var result) {
        ski(server.dataPreProcess(result));
      };
    }
    
    Failure fk = fki;

    try {
      switch (method) {
        case "GET":    handler.get_(sk, fk); break;
        case "PUT":    handler.put(bcapData, sk, fk); break;
        case "POST":   handler.post(bcapData, sk, fk); break;
        case "DELETE": handler.delete(sk, fk); break;
        default: fki(BcapError.methodNotAllowed);
      }
    }
    catch (final e) {
      // throw; // enable this line for debugging of the cap server
      fki(BcapError.internalServerError);
    }
  }
}


class BcapNullInterface implements BcapInterface {
  BcapNullInterface() {}

  void invoke(String method, String data, SuccessI ski, FailureI fki) {
    fki(nullError);
  }

  static final BcapError nullError = const BcapError(500, 'Null Bcap');
}




class BcapServerImpl implements BcapServer {
  String instanceID;
  Map<String, BcapInterface> implMap;
  Map<String, Object> reviveMap;
  Reviver reviver;
  Resolver resolver;
  SaveState saveState;
  
  static BcapInterface deadInterface = null;

  BcapServerImpl(String this.instanceID, String snapshot = null,
                 SaveState this.saveState = null)
    : implMap = new Map<String, BcapInterface>(),
      reviveMap = new Map<String, Object>(),
      reviver = (BcapHandler _(String key) { return null; }) {
    if (snapshot != null) {
      Map<String, Object> snap = JSON.parse(snapshot);
      reviveMap = snap["map"];
      instanceID = snap["id"];
    }

    if (deadInterface === null) deadInterface = new BcapNullInterface();
  }

  void privateInvoke(String ser, String method, String data,
                     SuccessI ski, FailureI fki) {
    var instID = BcapUtil.decodeInstID(ser);
    if (instID == instanceID) {
      BcapInterface handlerInterface = getInterface(ser);
      handlerInterface.invoke(method, data, ski, fki);
      return;
    } else {
      var publicInterface = resolver(instID);
      if (publicInterface !== null) {
        publicInterface.invoke(ser, method, data, ski, fki);
        return;
      }
    }
    deadInterface.invoke(method, data, ski, fki);
  }
  
  void invoke(String ser, String method, String data,
              SuccessI ski, FailureI fki) {
    getInterface(ser).invoke(method, data, ski, fki);
  }

  BcapInterface getInterface(String ser) {
    String capID = BcapUtil.decodeCapID(ser);
    if (!implMap.containsKey(capID)) {
      if (reviveMap.containsKey(capID)) {
        Map info = reviveMap[capID];
        if (info.containsKey("restoreKey")) {
          BcapHandler h = reviver(info["restoreKey"]);
          if (h != null) {
            implMap[capID] = new BcapHandlerInterface(this, h);
          }
        }
      }
    }
    return implMap.containsKey(capID) ? implMap[capID] : deadInterface;
  }

  void setReviver(Reviver newReviver) { reviver = newReviver; }
  void setResolver(Resolver newResolver) { resolver = newResolver; }

  // NOTE(jpolitz): we really wish we had the second argument
  // to stringify that would let us instrument the parsing
  // TODO(jpolitz): cyclic check
  /* JSON */ dataPreProcess(Object data) {
    Object decapitate(Object json) {
      switch (true) {
        case json is List:
          List arr = json;
          List newArr = new List(arr.length);
          for (int i = 0; i < arr.length; i++) {
            newArr[i] = decapitate(arr[i]);
          }
          return newArr;

        case json is Map:
          Map<String, Object> map = json;
          Map<String, Object> newMap = map;
          map.forEach(void _(String key, Object val) {
            newMap[key]= decapitate(val);
          });
          return newMap;

        case json is Bcap:
          Bcap cap = json;
          return {'@': cap.serialize()};

        default:
          return json;
      }
    }
    return JSON.stringify({"value": decapitate(data)});
  }

  // NOTE(jpolitz): we really wish we had the second argument
  // to parse that would let us instrument the parsing
  Object dataPostProcess(String data) {
    if (data == null || data.trim() == '') {
      return null;
    }

    Object capitate(Object json) {
      switch (true) {
        case json is List:
          List arr = json;
          List newArr = new List(arr.length);
          for (int i = 0; i < arr.length; i++) {
            newArr[i] = capitate(arr[i]);
          };
          return newArr;

        case json is Map:
          Map<String, Object> map = json;
          Map<String, Object> newMap = new Map();
          var cap = null;
          map.forEach(void _(String key, Object val) {
            if (key == "@") {
              if (map.length == 1) {
                cap = new BcapImpl(val, this);
              } else {
                throw "dataPostProcess: Can't have @ as a key in a multi-map";
              }
            } else {
              newMap[key]= capitate(val);
            }
          });
          if (cap !== null) { return cap; }
          return newMap;


        default:
          return json;
      }
    }

    Map jsonBundle = JSON.parse(data);
    return capitate(jsonBundle["value"]);
  }

  String snapshot() {
    return JSON.stringify({
      "id": instanceID,
      "map": reviveMap
    });
  }

  void revoke(String ser) {
    String capID = BcapUtil.decodeCapID(ser);
    implMap.remove(capID);
    reviveMap.remove(capID);
  }

  void revokeAll() {
    implMap.clear();
    reviveMap.clear();
  }

  Bcap _mint(String capID) {
    var ser = BcapUtil.encodeSer(instanceID, capID);
    var cap = new BcapImpl(ser, this);
    return cap;
  }

  Bcap _grant(BcapInterface iface, String key) {
    String capID = BcapUtil.uuidv4();

    implMap[capID] =
      (iface === null) ? deadInterface : iface;

    if (key !== null) reviveMap[capID] = {"restoreKey": key };
    if (saveState !== null) {
      saveState(snapshot());
    }
    return _mint(capID);
  }

  Bcap grant(BcapHandler handler, String key = null) {
    return _grant(new BcapHandlerInterface(this, handler), key);
  }

  Bcap grantKey(String key) {
    return grant(reviver(key), key);
  }

  Bcap grantFunc(BcapSyncFunction f, String key = null) {
    return grant(new BcapFunctionHandler(f), key);
  }

  Bcap grantAsyncFunc(BcapAsyncFunction f, String key = null) {
    return grant(new BcapAsyncFunctionHandler(f), key);
  }

  Bcap restore(String ser) {
    return new BcapImpl(ser, this);
  }
}


