
library pets;

import 'dart:async';
import 'dart:collection';
import 'dart:isolate';
import 'dart:math';

final num MAX_CATS = 10;

final SPARKY = const Cat("Sparky");

final CHIPPY = const _Chipmunk("Chi\np\npy");

abstract class Animal {
  bool livesWith(Animal other);
  void performAction();
}

class Cat implements Animal {
  static final String BEST_CAT_NAME = "Spooky";

  static final BLACK_CAT = const Cat("Midnight");

  final String name;

  final bool _declawed = false;

  const Cat(this.name);

  bool livesWith(Animal other) => other is Cat;

  final String _color = "blue";

  String get color {
    return _color;
  }

  set color(String val) {
    // nothing to do
    val = "${val}";
  }

  void performAction() {
    print("m${_eo()}w");
  }

  String toString() => "cat ${name}";

  String _eo() {
    return "eo";
  }
}

class Rodent implements Animal {
  final String name;

  const Rodent(this.name);

  bool livesWith(Animal other) => other is Rodent;

  void performAction() {

  }

  String toString() {
    var temp = "123";

    if (true) {
      throw "123456";
    }

    return "won't get here...";
  }
}

class _Chipmunk implements Animal {
  final String name;

  const _Chipmunk(this.name);

  bool livesWith(Animal other) => other is _Chipmunk;

  void performAction() { }

  String toString() => name;
}

class FloppyEars {
  // TODO(devoncarew): both: EAR_COUNT is not displayed
  static final int EAR_COUNT = 2;

}

class Dog extends FloppyEars implements Animal {
  static final String BEST_DOG_NAME = "Chips";

  String name;

  bool collar;

  int fleaCount;

  DateTime bornOn;

  Dog(this.name) {
    var rand = new Random();
    fleaCount = rand.nextInt(10);
    bornOn = new DateTime.now();
  }

  Dog.withFleas(this.name, this.fleaCount) {
    bornOn = new DateTime.now();
  }

  bool livesWith(Animal other) => true;

  void performAction() {
    String name = "iHideAnInstanceVariable";

    print("bark");

    var closure = () {
      // TODO: we want the call frame name to be Dog.performAction.{}
      print("bark");
      print("bark");
      print("bark");
    };

    closure();
  }

  String toString() => "dog ${name}";
}

/**
 * Ferrets return null for their toString().
 */
class Ferret extends Animal {
  String name;
  int clawCount;
  
  Ferret(this.name) {
    clawCount = 4;
  }
  
  bool livesWith(Animal other) => false;
  
  void performAction() {
    
  }

  String toString() {
    return null;
  }
}

List<Animal> getLotsOfAnimals() {
  return [
      new Dog("Scooter"),
      new Cat("Munchkins"),
      new Cat("1月27日(金曜日)"),
      SPARKY,
      new Dog.withFleas(Dog.BEST_DOG_NAME, 2),
      new Dog("Scooter"),
      new Cat("Munchkins"),
      new Cat("1月27日(金曜日)"),
      new Dog("Scooter"),
      new Cat("Munchkins"),
      new Cat("1月27日(金曜日)"),
      new Dog("Scooter"),
      new Cat("Munchkins"),
      new Cat("1月27日(金曜日)"),
      new Dog("Scooter"),
      new Cat("Munchkins"),
      new Cat("1月27日(金曜日)"),
      SPARKY,
  ];
}

Map<String, Animal> getMapOfAnimals() {
  // var map = {};
  Map<String, Animal> map = new Map<String, Animal>();

  for (var animal in getLotsOfAnimals()) {
    map[animal.toString()] = animal;
  }

  map["and then"] = SPARKY;

  return map;
}

void testInfinity() {
  var infTest = 1 / 0;

  print(infTest); // Infinity
}

void spawnAnimalsIsolate() {
  spawnFunction(_spawnAnimals);
}

void _spawnAnimals() {
  int count = new Random().nextInt(10);

  print("isolate started");
      
  new Timer(new Duration(seconds: count), () {
    print("isolate finished after ${count} seconds");
  });
}

void checkTypes() {
  // TODO: arrays vs lists
  List<String> list = new List<String>();
  list.add("one");
  list.add("two");
  list.add("three");
  var arr = ["dsdf", "dsss", "sfs23"];

  // TODO: DoubleLinkedQueue
  DoubleLinkedQueue queue = new DoubleLinkedQueue();
  queue.add("foo");
  queue.add("bar");
  queue.add(123);

  // TODO: _LinkedHashMapImpl
  var map = {};
  map["one"] = 1;
  map["two"] = 2;
  map["three"] = 3;

  // should display as null
  var bob = null;

  // TODO: empty lists
  var emptyList = [];

  // TODO: empty maps
  var emptyMap = {};

  // display sets (HashSet)
  var set = new Set();
  set.add(1);
  set.add(2);
  set.add(2);
  set.add("what");

  print("types");
}

void createARealBigArray() {
  var arr = new List<int>(120);

  for (int i = 0; i < arr.length; i++) {
    arr[i] = i * 10;
  }

  print("big array created");

  arr[0]++;
}
