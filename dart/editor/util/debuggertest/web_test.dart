
/**
 * A web application used to exercise the debugger.
 */
#library("web_test");

#import("dart:html");
#import("pets.dart");

num rotatePos = 0;

// TODO(devoncarew): both: what is the best ordering of the fields?
// By order of their declaration? How do I get that?

// TODO(devoncarew): I can still crash dartium w/ breakpoints

void main() {
  query("#text").text = "Welcome to Dart!";

  query("#text").on.click.add(rotateText);

  testAnimals();
}

void rotateText(Event event) {
  rotatePos += 360;

  var textElement = query("#text");

  textElement.style.transition = "1s";
  textElement.style.transform = "rotate(${rotatePos}deg)";

  testAnimals();
}

void testAnimals() {
  print("starting debuggertest");

  // TODO(devoncarew): if the breakpoint is set before the dart source is
  // loaded, dartium does not send us the adjusted bp location.
  var tempCat = SPARKY;

  print("${tempCat}:");

  tempCat.performAction();

  var dog = new Dog("Scooter");

  dog.performAction();

  var l = getLotsOfAnimals();

  print(l);

  // Throws during toString()
  var rodent = new Rodent("Skittles");

  // TODO(devoncarew): dartium: display maps better
  var m = getMapOfAnimals();

  print(m);
}
