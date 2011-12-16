#import('../../core/actors-web.dart');
#import('dart:html');

main() {
  var actorManager;
  actorManager = new ActorManager(5);

  var button = document.query("#run-button");
  var hopselement = document.query("#hops");
  var waitelem = document.query("#wait-icon");
  button.on.click.add((Event e) {
    var hops = hopselement.value;
    button.attributes['disabled'] = "disabled";
    waitelem.style.setProperty("display", "block", "");
    actorManager.create(const ThreadRingManagerFactory(), "init", [hops]);
  });
}

class ThreadRing extends Actor {
  ActorId _next;
  int _name;

  ThreadRing() : super() {
    on["init"] = (int name) {
      _name = name;
      reply("ready", [_name-1, me]);
    };

    on["next"] = (ActorId next) {
      _next = next;
      reply("nextset");
    };

    on[""] = (int val) {
      if (val == 0) {
        ui.setStyles("#wait-icon", ["display"], [["none"]]);
        ui.innerHTML("#thread", "$_name");
        ui.innerHTML("#token", "0");
        ui.removeAttribute("#run-button", "disabled");
      }
      else {
        if (val % 1000 == 0) {
          ui.innerHTML("#thread", "$_name");
          ui.innerHTML("#token", "${val}");
        }
        send(_next, "", [val-1]);
      }
    };
  }
}
class ThreadRingFactory implements ActorFactory {
  const ThreadRingFactory();
  Actor create() => new ThreadRing();
}

class ThreadRingManager extends Actor {
  final int _N = 503;
  int _H;
  int _rets;
  List<ActorId> _ring;
  ThreadRingManager() : super() {
    on["init"] = (String hops) {
      _H = Math.parseInt(hops);
      ui.innerHTML("#thread", "0");
      ui.innerHTML("#token", "${_H}");
      _ring = new List(_N);
      _rets = 0;
      for (int i = 0; i < _N; i++) {
        create(const ThreadRingFactory(), "init", [(i+1)]);
      }
    };

    on["ready"] = (int index, ActorId id) {
      _ring[index] = id;
      _rets++;
      if (_rets == _N) {
        _rets = 0;
        for (int i = 0; i < _N; i++) {
          send(_ring[i], "next", [(_ring[(i+1)%_N])]);
        }
      }
    };

    on["nextset"] = () {
      _rets++;
      if (_rets == _N) {
        send(_ring[0], "", [_H]);
      }
    };
  }
}

class ThreadRingManagerFactory implements ActorFactory {
  const ThreadRingManagerFactory();
  Actor create() => new ThreadRingManager();
}

