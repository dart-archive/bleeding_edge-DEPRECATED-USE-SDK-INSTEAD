#library('actors');
#source('actors.dart');
#source('actors-interface.dart');
#source('actors-ui.dart');

class _WindowActor extends Actor {
  _WindowActor() : super() {
    on["print"] = (String m) {
      print("${m}");
    };

    /**
     * Sends a message back using the 'reply' parameter after specified number
     * of milliseconds specified by 'timeout' parameter.
     */
    on["setTimeout"] = (Reply reply, int timeout) {
      new Timer(timeout, (Timer t) {reply.respond();});
    };
  }
}

class _WindowProxyImpl extends _NullWindowProxy {
  ActorId _window;
  _WindowProxyImpl(this._window);
  
  void print(String m) => _window.send("print", [m]);

  void setTimeout(Reply t, int timeout) =>
    _window.send("setTimeout", [t, timeout]);
}
