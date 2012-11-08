
library pets;

import 'dart:math';
import 'dart:isolate';

final num MAX_CATS = 10;

final SPARKY = const Cat("Sparky");

final CHIPPY = const _Chipmunk("Chi\np\npy");

interface Animal {
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

  Date bornOn;

  Dog(this.name) {
    var rand = new Random();
    fleaCount = rand.nextInt(10);
    bornOn = new Date.now();
  }

  Dog.withFleas(this.name, this.fleaCount) {
    bornOn = new Date.now();
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
}

List<Animal> getLotsOfAnimals() {
  return [
      new Dog("Scooter"),
      new Cat("Munchkins"),
      new Cat("1月27日(金曜日)"),
      SPARKY,
      new Dog.withFleas(Dog.BEST_DOG_NAME, 2)
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
  int count = new Random().nextInt(100);

  for (num i = 0; i < count * 1000; i++) {
    getLotsOfAnimals();
  }
}
