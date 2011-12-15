#library('actors');
#source('actors.dart');
#source('actors-interface.dart');
#source('actors-ui.dart');

class _WindowActor extends Actor {
  _WindowActor() : super() {
    on["print"] = (String m) {
      print("${m}");
    };
  }
}

class _WindowProxyImpl extends _NullWindowProxy {
  ActorId _window;
  _WindowProxyImpl(this._window);
  
  void print(String m) => _window.send("print", [m]);
}
