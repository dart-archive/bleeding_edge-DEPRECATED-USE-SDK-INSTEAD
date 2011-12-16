#import('../../core/actors-term.dart');


main() {
  new ActorManager(1).create(const HelloWorldFactory(), "say hello", ["World"]);
}

class HelloWorld extends Actor {
  HelloWorld() : super() {
    on["say hello"] = (var who) {
      ui.print("Hello ${who}!");
      halt();
    };
  }
}

class HelloWorldFactory implements ActorFactory {
  const HelloWorldFactory();
  Actor create() => new HelloWorld();
}
