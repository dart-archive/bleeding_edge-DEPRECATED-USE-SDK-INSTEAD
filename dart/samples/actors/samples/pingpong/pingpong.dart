#import('../../core/actors-web.dart');

class PingPong extends Actor {
  String _name;
  
  PingPong() : super() {  
    on["init"] = (String name) {
      _name = name;
      if (name == "Ping") {
        create(const PingPongFactory(), "init", ["Pong"]);
        ui.create("div", ["pingpong"]);
      }
      else {
        reply("ping", [1, me]);
      }
    };

    on["ping"] = (int v, ActorId other) {
      ui.innerHTML("#pingpong", "${_name}: $v");
      ui.setTimeout(new Reply.trigger(other, "ping", [v+1, me]), 1000);
    };
  }
}

class PingPongFactory implements ActorFactory {
  const PingPongFactory();
  Actor create() => new PingPong();
}

main() {
  new ActorManager(2).create(const PingPongFactory(), "init", ["Ping"]);
}

