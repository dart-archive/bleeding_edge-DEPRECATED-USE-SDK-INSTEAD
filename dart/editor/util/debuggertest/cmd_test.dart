
/**
 * A command-line application used to exercise the debugger.
 */
#library("cmd_test");

#import("pets.dart");

num petCount = 0;

void main() {
  print("starting debuggertest");

  // TODO(devoncarew): cmd-line: bp here stops at next line
  var tempCat = SPARKY;

  print("${tempCat}:");

  tempCat.performAction();

  var dog = new Dog("Scooter");

  dog.performAction();

  var l = getLotsOfAnimals();

  print(l);

  // TODO(devoncarew): cmd-line: display maps better
  // TODO(devoncarew): cmd-line: display the type of the object
  var m = getMapOfAnimals();

  print(m);
}
