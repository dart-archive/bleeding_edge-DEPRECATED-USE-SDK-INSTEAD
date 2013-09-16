
/**
 * A command-line application used to exercise the debugger.
 */
library cmd_test;

import 'dart:async';

import '../lib/pets.dart' as pets;

num petCount = 0;
double fooCount = 10.0;

void main() {
  print("starting debuggertest");

  // Spawn some isolates...
//  pets.spawnAnimalsIsolate();
//  pets.spawnAnimalsIsolate();
//  pets.spawnAnimalsIsolate();

  var cat = pets.SPARKY;

  cat.color;
  cat.color = "dsdf";

  var testStr = "my\ncat";

  print("my ${cat} says:");

  cat.performAction();

  var fooBarBaz = 1 + 2 + 3 
      + 4 + 5;
  
  pets.checkTypes();

  pets.createARealBigArray();

  pets.Ferret ferret = new pets.Ferret("Fanny");
  
  var dog = new pets.Dog("Scooter");

  dog.performAction();

  var l = pets.getLotsOfAnimals();

  print(l);

  // delay a few seconds
  new Timer(new Duration(seconds: 5), () {
    print('delayed closure');
  });
  
  var m = pets.getMapOfAnimals();

  print(m);
}
