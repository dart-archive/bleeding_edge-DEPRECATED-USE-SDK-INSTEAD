// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('PrototypePatchingTest');

/** 
 * This simple little test ensures that the methods we add to the JavaScript 
 * objects Array and Object with frog are non-enumerable so we don't break
 * arbitrary third-party JavaScript (!).  
 */

#import('node_config.dart');
#import('../../../../lib/unittest/unittest.dart');
#import('../../../lang.dart');
#import('../../../file_system_node.dart');
#import('../../../lib/node/node.dart');

class Veggies {
  int radishes;
  Veggies() {
    radishes = 3;
  }
  String prefs() => 'I eat broccoli.';
  num get getRadishes() => radishes;
  String favorites() => "Sauteed Spinach!";
}

testMethodCount(testObject) native """
function findMethodCount(obj) {
  var count = 0;
  for (var i in obj) {
    count++;
  }
  return count;
}
return findMethodCount(testObject);""";

  

main() {
  useNodeConfiguration();

  // Get the home directory from our executable.
  var homedir = path.dirname(fs.realpathSync(process.argv[1]));

  var argv = new List.from(process.argv);
  argv.add('tests/frog/src/PrototypePatchingTest.dart');

  parseOptions(homedir, argv, new NodeFileSystem());
  initializeWorld(new NodeFileSystem());
 
  world.runCompilationPhases(); 
  var code = world.getGeneratedCode();
  List foo = new List();
  foo.add('e');
  foo.add('f');
  int count1 = testMethodCount(foo);
  foo = new List();
  int count2 = testMethodCount(foo);
 

  test('Testing method count for Lists (Arrays)', () {
    Expect.equals(true, count1 == 2);
    Expect.equals(true, count2 == 0);
  });
  
  Veggies veg =  new Veggies();
  veg.prefs();
  veg.favorites(); // Called so they don't get optimized away.
  int count3 = testMethodCount(veg);
  test('Testing method count for Objects', () {
    Expect.equals(true, count3 == 3);
  });
}

