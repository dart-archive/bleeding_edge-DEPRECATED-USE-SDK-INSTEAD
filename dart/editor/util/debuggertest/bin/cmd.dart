
/**
 * A command-line application used to exercise the debugger.
 */
library cmd_test;

import 'dart:async';

import 'package:unittest/unittest.dart';

import '../lib/pets.dart' as pets;

num petCount = 0;
double fooCount = 10.0;

void main() {
  print("starting debuggertest");

  print(fooCount);

  // Spawn some isolates...
  pets.spawnAnimalsIsolate(1);
  pets.spawnAnimalsIsolate(2);
  pets.spawnAnimalsIsolate(3);

  var cat = pets.SPARKY;

  cat.color;
  cat.color = "dsdf";

  var testStr = "my\ncat";

  print("my ${cat} says:");

  cat.performAction();

  var fooBarBaz = 1 + 2 + 3
      + 4 + 5;

  pets.checkTypes();

  List bigArray = pets.createARealBigArray();

  pets.Ferret ferret = new pets.Ferret("Fanny");

  var dog = new pets.Dog("Scooter");

  dog.performAction();

  print(pets.Dog.staticDogCount);

  var l = pets.getLotsOfAnimals();

  print(l);

  // delay a few seconds
  new Timer(new Duration(seconds: 5), () {
    print('delayed closure');
  });

  var m = pets.getMapOfAnimals();

  print(m);

  group('test-test', () {
    test('foo', () {
      expect(false, true);
    });
  });
}
