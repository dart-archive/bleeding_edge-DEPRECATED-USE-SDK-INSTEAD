// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('bcap_tests');

#import('dart:html');
#import('../../../lib/unittest/unittest_html.dart');
#import('../../../lib/json/json.dart');
#import('../bcap/bcap.dart');

void main() {
  new BcapTests().run();
}

class BcapTests extends UnitTestSuite {
  BcapTests()  : super() { }
  
  void setUpTestSuite() {
    addTest(() { testUUID(); });
    addTest(() { testDecode(); });
    addTest(() { testPreProcess(); });
    addTest(() { testPostProcess(); });
    addTest(() { testPrePostProcessSimple(); });
    addTest(() { testPrePostProcessStruct(); });
    addTest(() { testBasicLifeCycle(); });
    addTest(() { testReviver(); });
    addTest(() { testCrossServerInvoke(); });
    addAsyncTest(() { testGrantGrant(); }, 1);
    // addAsyncTest(() { testIsolateInvoke(); }, 1);
    // addTest(() { testWindow(); });
  }

  void testUUID() {
    var r = BcapUtil.uuidv4();
    Expect.isTrue(r.toString() == r);
    Expect.equals(r.length, 36);
  }

  void testDecode() {
    var uuid1 = BcapUtil.uuidv4();
    var uuid2 = BcapUtil.uuidv4();
    var serCap ="urn:x-cap:$uuid1:$uuid2";
    var decoded = BcapUtil.decodeSer(serCap);
    Expect.equals(2, decoded.length);
    Expect.equals(uuid1, decoded[0]);
    Expect.equals(uuid2, decoded[1]);

    Expect.equals(uuid1, BcapUtil.decodeInstID(serCap));
    Expect.equals(uuid2, BcapUtil.decodeCapID(serCap));
  }

  void testPreProcess() {
    BcapServer server = new BcapServer(null, null);

    String ser = BcapUtil.uuidv4();
    Bcap cap = server.restore(ser);
    Expect.equals("{\"value\":{\"@\":\"$ser\"}}", server.dataPreProcess(cap));

    String ser2 = BcapUtil.uuidv4();
    Bcap cap2 = server.restore(ser2);
    var somejson = [cap2];
    Expect.equals("{\"value\":[{\"@\":\"$ser2\"}]}",
        server.dataPreProcess(somejson));


    String ser3 = BcapUtil.uuidv4();
    Bcap cap3 = server.restore(ser3);
    var somejson2 = {"foo": [cap, cap3, 5]};
    Expect.equals("{\"value\":{\"foo\":[{\"@\":\"$ser\"},{\"@\":\"$ser3\"},5]}}",
        server.dataPreProcess(somejson2));
  }

  void testPostProcess() {
    BcapServer server = new BcapServer(null, null);

    String ser = BcapUtil.uuidv4();
    Bcap cap = server.restore(ser);
    Bcap foo = server.dataPostProcess("{\"value\":{\"@\":\"$ser\"}}");
    Expect.equals(cap, foo);
  }

  void testPrePostProcessSimple() {
    BcapServer server = new BcapServer(BcapUtil.uuidv4());
    var roundTrip = Object _(Object data) {
      Expect.equals(data,
          server.dataPostProcess(server.dataPreProcess(data)));
    };

    roundTrip(null);
    roundTrip(false);
    roundTrip(true);
    roundTrip(0);
    roundTrip(501234);
    roundTrip('');
    roundTrip("\n");
  }

  void testPrePostProcessStruct() {
    BcapServer server = new BcapServer(BcapUtil.uuidv4());
    var roundTrip = Object _(Object data) {
      return server.dataPostProcess(server.dataPreProcess(data));
    };

    Expect.equals(5, roundTrip([5])[0]);
    var rtObj = roundTrip({"foo": [true, false]});
    Expect.equals(true, rtObj["foo"][0]);
    Expect.equals(false, rtObj["foo"][1]);
  }

  void testBasicLifeCycle() {
    int f(_) { return 42; }
    var bc = new BcapServer(BcapUtil.uuidv4());
    var cap = bc.grantFunc(f);

    Expect.notEquals(cap, null);
    var result = "1-not-set";
    cap.get_(void _(r) { result = r;});
    Expect.equals(42, result);

    bc.revoke(cap.serialize());
    var invoked = false;
    var resultRevoked = "revoked-not-set";
    cap.get_(void _(r) { resultRevoked = r; invoked = true; },
             void _(e) { Expect.equals(e.status, 500); });
    Expect.equals(resultRevoked, "revoked-not-set");
    Expect.isFalse(invoked);

    var cap2 = bc.grantFunc(f);
    var result2 = "2-not-set";
    Expect.notEquals(cap2.serialize(), cap.serialize());
    cap2.get_(void _(r) { result2 = r; });
    Expect.equals(42, result2);

    var cap3 = bc.grantFunc(f);
    Expect.isFalse(cap2 === cap3);
    Expect.notEquals(cap2.serialize(), cap3.serialize());
    bc.revoke(cap2.serialize());
    var result3 = "3-not-set";
    cap3.get_(void _(r) { result3 = r; });
    Expect.equals(result3, 42);
  }

  void testReviver() {
    BcapServer bc = new BcapServer(BcapUtil.uuidv4());
    bool called = false;
    int f(_) { if (called) { return 84; } called = true; return 42; }
    reviver(key) {
      if (key == "get-f-back") {
        return new BcapFunctionHandler(f);
      }
      return null;
    }
    Bcap cap1 = bc.grantFunc(f);
    var result = 0;
    cap1.get_(void _(r) { result = r; });
    Expect.equals(result, 42);

    var snap = bc.snapshot();
    bc.revokeAll();

    BcapServer bc2 = new BcapServer(null, snap);
    bc2.setReviver(reviver);
    Bcap cap2 = bc2.grantKey("get-f-back");

    cap2.get_(void _(r) { result = r; });
    Expect.equals(result, 84);
  }

  void testCrossServerInvoke() {
    String instID1 = BcapUtil.uuidv4();
    String instID2 = BcapUtil.uuidv4();

    BcapServer bc1 = new BcapServer(instID1);
    BcapServer bc2 = new BcapServer(instID2);

    int f(_) { return 29; }

    Bcap cap1 = bc1.grantFunc(f);

    BcapServer resolver2(instID) {
      if (instID == instID1) {
        return bc1;
      }
    }
    bc2.setResolver(resolver2);

    Bcap cap2 = bc2.restore(cap1.serialize());
    var result = 0;
    bool failed = false;
    cap2.get_(void _(r) { result = r; },
              void _(e) { failed = true; });
    Expect.isFalse(failed);
    Expect.equals(result, 29);
  }

  void testGrantGrant() {
    BcapServer bc = new BcapServer(BcapUtil.uuidv4());

    Bcap f = bc.grantFunc((x) => bc.grantFunc((y) => x * y));

    f.post(2, void _(g) {
      g.post(3, void _(r) {
        Expect.equals(6, r);
        callbackDone();
      });
    });

  }


  /* Not implemented yet
  void testIsolateInvoke() {

    var testDir = "../../tests/client/bcap";
    Window child;


    String uuid = BcapUtil.uuidv4();
    BcapServer cs = new BcapServer(uuid);


    Object getSeedCap() {
      return cs.grantAsyncFunc(void _(arg, sk, fk) {
        Expect.equals(true, false);
        Expect.equals("from the child", arg);
//        child.close();
        callbackDone();
      }).serialize();
    };

    WindowTunnel tunnel = new WindowTunnel(cs, window, getSeedCap, null);
    cs.setResolver(BcapServerInterface _(instID) {
      return tunnel.getSendInterface(instID);
    });

    // TODO(arjun): abstract these into a method on WindowTunnel
    var childUuid = BcapUtil.uuidv4();
    childUuid = "fa4e6d8a-e51b-4853-831a-846a1936f9ee";
    child = window.open("$testDir/bcap_child.html", childUuid);
    tunnel.registerWindow(child, childUuid);

    
    
  }
  
  void testWindow() {
    String uuid = BcapUtil.uuidv4();
    BcapServer server = new BcapServer(uuid);

    WindowTunnel tunnel;
    var called = false;
    Bcap cap2;
    Bcap cap1 = server.grantAsyncFunc(void _(data, sk, fk) {
      String uuidChild2 = "fa4e6d8a-e51b-4853-831a-846a1936f9ee";
      Window child2 = mywin.open("../../tests/client/bcap/TestPage.html", "$uuidChild2");
      tunnel.registerWindow(child2, uuidChild2);
      called = true;
      cap2 = data;
    });

    Object getSeedCap() {
      if (!called) { return {"first": cap1.serialize()}; }
      return {"second": cap2.serialize()};
    }

    tunnel = new WindowTunnel(server, mywin, getSeedCap, null);
    String uuidChild1 = "9065b6ef-41d4-4684-879e-23794f86f1fa";
    Window child1 = mywin.open("../../tests/client/bcap/TestPage.html", "$uuidChild1");
    tunnel.registerWindow(child1, uuidChild1);

    server.setResolver(BcapServerInterface _(instID) {
      if (instID === uuid) { return server; }
      else { return tunnel.getSendInterface(instID); }
    });
  }
  */
}

