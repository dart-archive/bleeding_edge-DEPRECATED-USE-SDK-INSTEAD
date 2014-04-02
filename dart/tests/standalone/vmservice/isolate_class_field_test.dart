// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library isolate_class_list_test;

import 'dart:async';
import 'test_helper.dart';
import 'package:expect/expect.dart';

class BananaClassTest {
  int port;
  String isolate_id;
  String class_id;
  BananaClassTest(this.port, this.isolate_id, this.class_id);

  _testFieldA(Map field) {
    Expect.equals('Field', field['type']);
    Expect.equals('a', field['user_name']);
    Expect.equals('a', field['name']);
    Expect.equals(false, field['final']);
    Expect.equals(false, field['static']);
    Expect.equals(false, field['const']);
    Expect.equals('dynamic', field['guard_class']);
    Expect.equals(true, field['guard_nullable']);
    Expect.equals('variable', field['guard_length']);
  }

  _testFieldFinalFixedLengthList(Map field) {
    Expect.equals('Field', field['type']);
    Expect.equals('final_fixed_length_list', field['user_name']);
    Expect.equals('final_fixed_length_list', field['name']);
    Expect.equals('Float32List', field['guard_class']['user_name']);
    Expect.equals(true, field['final']);
    Expect.equals(false, field['static']);
    Expect.equals(false, field['const']);
    Expect.equals(4, field['guard_length']);
  }

  _testFieldFixedLengthList(Map field) {
    Expect.equals('Field', field['type']);
    Expect.equals('fixed_length_list', field['user_name']);
    Expect.equals('fixed_length_list', field['name']);
    Expect.equals(false, field['final']);
    Expect.equals(false, field['static']);
    Expect.equals(false, field['const']);
    Expect.equals('Float32List', field['guard_class']['user_name']);
    Expect.equals('variable', field['guard_length']);
  }

  _testFieldName(Map field) {
    Expect.equals('Field', field['type']);
    Expect.equals('name', field['user_name']);
    Expect.equals('name', field['name']);
    Expect.equals(false, field['final']);
    Expect.equals(false, field['static']);
    Expect.equals(false, field['const']);
    Expect.equals('String', field['guard_class']['user_name']);
    Expect.equals(false, field['guard_nullable']);
    Expect.equals('variable', field['guard_length']);
  }

  Future makeRequest() {
    var fields = ['final_fixed_length_list', 'fixed_length_list', 'name', 'a'];
    var helper =
      new ClassFieldRequestHelper(port, isolate_id, class_id, fields);
    return helper.makeRequest().then((_) {
      _testFieldA(helper.fields['a']);
      _testFieldFinalFixedLengthList(helper.fields['final_fixed_length_list']);
      _testFieldFixedLengthList(helper.fields['fixed_length_list']);
      _testFieldName(helper.fields['name']);
    });
  }
}

class BadBananaClassTest {
  int port;
  String isolate_id;
  String class_id;
  BadBananaClassTest(this.port, this.isolate_id, this.class_id);

  _testFieldV(Map field) {
    Expect.equals('Field', field['type']);
    Expect.equals('v', field['user_name']);
    Expect.equals('v', field['name']);
    Expect.equals(false, field['final']);
    Expect.equals(false, field['static']);
    Expect.equals(false, field['const']);
    Expect.equals('double', field['guard_class']['user_name']);
    Expect.equals(true, field['guard_nullable']);
    Expect.equals('variable', field['guard_length']);
  }

  _testFieldC(Map field) {
    Expect.equals('Field', field['type']);
    Expect.equals('c', field['user_name']);
    Expect.equals('c', field['name']);
    Expect.equals(true, field['final']);
    Expect.equals(false, field['static']);
    Expect.equals(true, field['final']);
    Expect.equals('int', field['guard_class']['user_name']);
    Expect.equals(false, field['guard_nullable']);
    Expect.equals('variable', field['guard_length']);
  }

  _testFieldFinalFixedLengthList(Map field) {
    Expect.equals('Field', field['type']);
    Expect.equals('final_fixed_length_list', field['user_name']);
    Expect.equals('final_fixed_length_list', field['name']);
    Expect.equals('Float32List', field['guard_class']['user_name']);
    Expect.equals(true, field['final']);
    Expect.equals(false, field['static']);
    Expect.equals(false, field['const']);
    Expect.equals('variable', field['guard_length']);
  }

  _testFieldFixedLengthArray(Map field) {
    Expect.equals('Field', field['type']);
    Expect.equals('fixed_length_array', field['user_name']);
    Expect.equals('fixed_length_array', field['name']);
    Expect.equals('List', field['guard_class']['user_name']);
    Expect.equals(true, field['final']);
    Expect.equals(false, field['static']);
    Expect.equals(false, field['const']);
    Expect.equals(3, field['guard_length']);
  }

  Future makeRequest() {
    var fields = ['final_fixed_length_list', 'fixed_length_array', 'v', 'c'];
    var helper =
      new ClassFieldRequestHelper(port, isolate_id, class_id, fields);
    return helper.makeRequest().then((_) {
      _testFieldV(helper.fields['v']);
      _testFieldC(helper.fields['c']);
      _testFieldFinalFixedLengthList(helper.fields['final_fixed_length_list']);
      _testFieldFixedLengthArray(helper.fields['fixed_length_array']);
    });
  }
}

class ClassTableTest extends VmServiceRequestHelper {
  ClassTableTest(port, id) :
      super('http://127.0.0.1:$port/$id/classes/');

  String banana_class_id;
  String bad_banana_class_id;

  onRequestCompleted(Map reply) {
    ClassTableHelper helper = new ClassTableHelper(reply);
    Expect.isTrue(helper.classExists('Banana'));
    banana_class_id = helper.classId('Banana');
    Expect.isTrue(helper.classExists('BadBanana'));
    bad_banana_class_id = helper.classId('BadBanana');
  }
}

class VMTest extends VmServiceRequestHelper {
  VMTest(port) : super('http://127.0.0.1:$port/vm');

  String _isolateId;
  onRequestCompleted(Map reply) {
    VMTester tester = new VMTester(reply);
    tester.checkIsolateCount(1);
    _isolateId = tester.getIsolateId(0);
  }
}

main() {
  var process = new TestLauncher('field_script.dart');
  process.launch().then((port) {
    var test = new VMTest(port);
    test.makeRequest().then((_) {
      var classTableTest = new ClassTableTest(port, test._isolateId);
      classTableTest.makeRequest().then((_) {
        var bananaClassTest =
            new BananaClassTest(port, test._isolateId,
                                classTableTest.banana_class_id);
        var badBananaClassTest =
            new BadBananaClassTest(port, test._isolateId,
                                   classTableTest.bad_banana_class_id);
        Future.wait([bananaClassTest.makeRequest(),
                     badBananaClassTest.makeRequest()]).then((_) {
          process.requestExit();
        });
      });
    });
  });
}
