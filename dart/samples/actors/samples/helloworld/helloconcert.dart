#import('../../core/actors-term.dart');

main() {
  new ActorManager(2).create(const HelloConcertFactory(), "run");
}

class HelloConcert extends Actor {
  int done;
  final N;
  HelloConcert() : N = 200, super() {
    done = 0;
    on["run"] = () {
      for (int i = 0; i < N; i++) 
        create(const HelloWorldFactory(), "say hello", ["World $i"]);
    };
    on["done"] = () {
      done++;
      if (done == N) halt(); 
    };
  }
}

class HelloWorld extends Actor {
  HelloWorld() : super() {
    on["say hello"] = (var who) {
      ui.print("Hello $who!");
      reply("done");
    };
  }
}

class HelloConcertFactory implements ActorFactory {
  const HelloConcertFactory();
  Actor create() => new HelloConcert(); 
}

class HelloWorldFactory implements ActorFactory {
  const HelloWorldFactory();
  Actor create() => new HelloWorld(); 
}
