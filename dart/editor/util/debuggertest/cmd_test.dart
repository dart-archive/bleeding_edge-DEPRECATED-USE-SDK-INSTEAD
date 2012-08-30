
/**
 * A command-line application used to exercise the debugger.
 */
#library("cmd_test");

#import('pets.dart', prefix: 'pets');

num petCount = 0;

void main() {
  print("starting debuggertest");

  var cat = pets.SPARKY;

  print("my ${cat} says:");

  cat.performAction();

  var dog = new pets.Dog("Scooter");

  dog.performAction();

  var l = pets.getLotsOfAnimals();

  print(l);

  // TODO(devoncarew): cmd-line: display maps better
  // TODO(devoncarew): cmd-line: display the type of the object
  var m = pets.getMapOfAnimals();

  print(m);
}
