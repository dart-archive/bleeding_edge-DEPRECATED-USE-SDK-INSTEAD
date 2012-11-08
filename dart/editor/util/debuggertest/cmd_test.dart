
/**
 * A command-line application used to exercise the debugger.
 */
library cmd_test;

import 'pets.dart' as pets;

num petCount = 0;

void main() {
  print("starting debuggertest");

  // Spawn some isolates...
  pets.spawnAnimalsIsolate();
  pets.spawnAnimalsIsolate();
  pets.spawnAnimalsIsolate();

  var cat = pets.SPARKY;

  print("my ${cat} says:");

  cat.performAction();

  var dog = new pets.Dog("Scooter");

  dog.performAction();

  var l = pets.getLotsOfAnimals();

  print(l);

  var rodent = new pets.Rodent("Skittles");

  var m = pets.getMapOfAnimals();

  print(m);
}
