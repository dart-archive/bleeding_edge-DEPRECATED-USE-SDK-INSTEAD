
/**
 * A web application used to exercise the debugger.
 */
library web_test;

import 'dart:html';

import 'package:unittest/unittest.dart';

//import 'package:debuggertest/pets.dart';
import '../lib/pets.dart';

num rotatePos = 0;

void main([var args, var message]) {
  querySelector("#text").text = "Welcome to Dart!";

  querySelector("#text").onClick.listen(rotateText);

  testAnimals();

//  if (args == null || args.isEmpty) {
//    Uri uri = Uri.parse(window.location.href);
//    //Uri uri = Uri.base;
//    Isolate.spawnUri(uri.resolve('web.dart'), ['foooooooooo'], null);
//  } else {
//    print(args);
//  }
}

void rotateText(Event event) {
  rotatePos += 360;

  var textElement = querySelector("#text");

  textElement.style.transition = "1s";
  textElement.style.transform = "rotate(${rotatePos}deg)";

  testAnimals();
}

void testAnimals() {
  print("starting debuggertest");

  // TODO(devoncarew): if the breakpoint is set before the dart source is
  // loaded, dartium does not send us the adjusted bp location.
  var tempCat = SPARKY;

  unittestConfiguration;

  // Dartium does not support isolates
//  spawnAnimalsIsolate(1);
//  spawnAnimalsIsolate(2);
//  spawnAnimalsIsolate(3);

  tempCat.color;
  tempCat.color = "dsdf";

  var testStr = "my\ncat";

  print("${tempCat}:");

  tempCat.performAction();

  Dog.getStaticDog();

  checkTypes();

  List bigArray = createARealBigArray();

  Ferret ferret = new Ferret("Fanny");

  var dog = new Dog("Scooter");

  dog.performAction();

  var l = getLotsOfAnimals();

  print(l);

  // Throws during toString()
  var rodent = new Rodent("Skittles");

  var m = getMapOfAnimals();

  print(m);

  group('test-test', () {
    test('foo', () {
      expect(false, true);
    });
  });
}
