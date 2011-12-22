
class _ActorManagerImpl extends Actor implements ActorManager {

  /**
   * This determines the next worker to be used for creating an actor.
   */
  int _nextWorkerIndex;
  
  /**
   * Total number of isolates.
   */
  int _numOfIsolates;
  
  /**
   * This determines how many of the workers have been initialized. As soon as
   * all the workers are initialized we can start off with creating actors. 
   */
  int _initdones;
  List<ActorId> _workers;
  List<ActorId> _isolatedWorkers;
  _WindowActor _windowActor;
  
  /** 
   * This integer is used to generate unique identifiers within the scope 
   * of this actor.
   */
  int _nextUniqueId;
  
  int _genNextUID() {
    _nextUniqueId++;
    return _nextUniqueId;
  }
  
  _ActorManagerImpl(this._numOfIsolates) : super() {
    _allowUIAccess = true;
    _allowCreation = true;
    _nextUniqueId = 0;
    this._uniqueId = _genNextUID();
    _workers = <ActorId>[];
    _isolatedWorkers = <ActorId>[];
    for (int i = 0; i < _numOfIsolates; i++) _workers.add(null);
    _nextWorkerIndex = 0;
    _initdones = 0;

    _windowActor = new _WindowActor();
    _windowActor._uniqueId = _genNextUID();
    _windowProxy = new _WindowProxyImpl(_windowActor.me);

    for (int i = 0; i < _numOfIsolates; i++) {
      int j = i;
      new _ActorWorkerIsolate().spawn().then((SendPort port) {
        port.send(new _Message("init", _me, [_windowProxy]), 
            null);
        _workers[j] = new ActorId(port);
      });
    }

    // This message is sent from a worker once it is initialized.
    on["initdone"] = () {
      _initdones++;
    };

    // This message is sent from an actor for creating an actor.
    on["create"] = (ActorFactory actorFactory, _Message msg,
        bool allowUIAccess, bool allowCreation) {
      if (_initdones < _numOfIsolates) {
        // Re-send the message if not all the workers are initialized.
        me.send("create", [actorFactory, msg, allowUIAccess, allowCreation], 
            _currentSender);
      }
      else {
        int workerNum;
        workerNum = _nextWorkerIndex;
        _nextWorkerIndex = (_nextWorkerIndex+1) % _numOfIsolates;
        _workers[workerNum].send("create", 
            [actorFactory, msg, _genNextUID(), allowUIAccess, allowCreation], 
            _currentSender);
      }
    };

    // This message is sent from an actor for creating an isolated actor.
    on["createIsolated"] = (ActorFactory isolatedFactory, 
        _Message msg, bool allowUIAccess, bool allowCreation) {
      new _ActorWorkerIsolate().spawn().then((SendPort port) {
        port.send(new _Message("init", _me, [_windowProxy]), 
            null);
        final workerId = new ActorId(port);
        _isolatedWorkers.add(workerId);
        workerId.send("create", 
            [isolatedFactory, msg, _genNextUID(), allowUIAccess, allowCreation], 
            _currentSender);
      });
    };

    // This message will halt the whole process. 
    on["halt"] = () {
      _windowActor._port.close();
      _port.close();
    };
  }

  void create(ActorFactory actorFactory, String messageName, 
      [List params=const [], bool allowUIAccess=true, bool allowCreation=true]) 
    =>
      me.send("create", [actorFactory, new _Message(messageName, me, params), 
                         allowUIAccess, allowCreation], me);
    
  void createIsolated(ActorFactory isolatedFactory, String messageName, 
      [List params=const [], bool allowUIAccess=true, bool allowCreation=true]) 
    => 
      me.send("createIsolated", [isolatedFactory,  
                                 new _Message(messageName, me, params), 
                                 allowUIAccess, allowCreation], me);
}

class _ActorWorkerIsolate extends Isolate {
  
  _ActorWorkerImpl _impl;
  
  _ActorWorkerIsolate() : super.heavy();

  void main() {
    _impl = new _ActorWorkerImpl(port);
  }
}


class _ActorWorkerImpl extends Actor {
  
  _ActorWorkerImpl(ReceivePort p) : super._fromPort(p) {
    on["init"] = (WindowProxy win) {
      _manager = _currentSender;
      _windowProxy = win;
      _manager.send("initdone", [], me);
    };

    // This message is sent from ActorManager for creating an actor on
    // the current worker.
    on["create"] = (ActorFactory actorFactory, _Message msg, int uid, 
        bool allowUIAccess, bool allowCreation) {
      Actor anActor = actorFactory.create();
      anActor._uniqueId = uid;
      anActor._allowCreation = allowCreation;
      if (allowCreation) anActor._manager = _manager;
      else anActor._manager = null;
      anActor._allowUIAccess = allowUIAccess;
      if (allowUIAccess) anActor._windowProxy = _windowProxy;
      else anActor._windowProxy = new _NullWindowProxy();
      anActor.me.send(msg.messageName, msg.params, msg.replyTo);
    };
  }
}

interface ActorManager default _ActorManagerImpl {

  /**
   * Creates an Actor Manager with the specified number of isolates as 
   * background processes. 
   */
  ActorManager(int numOfIsolates);

  /**
   * Creates an actor using the specified factory and sends a message to it.
   */
  create(ActorFactory actorFactory, String msgName, List params, 
      bool allowUIAccess, bool allowCreation);
  
  /**
   * Creates an actor using the specified factory in a separate worker isolate
   * and sends a message to it.
   */
  createIsolated(ActorFactory isolatedFactory, String msgName, 
      List params, bool allowUIAccess, bool allowCreation);
}

class Reply {

  /**
   * The id of actor which this Reply is going to be sent.
   */
  ActorId _to;

  /** 
   * The name of the message which is responsible to handle this Reply. 
   */
  String _msgName;

  bool _isTrigger;
  
  /**
   * The list of parameters to be used if this Reply is a trigger.
   */
  List _params;
  
  Reply(this._to, this._msgName) {
    _isTrigger = false;
  }

  /**
   * Trigger is a kind of Reply for which we can specify the parameters to 
   * response.
   */
  Reply.trigger(this._to, this._msgName, this._params) {
    _isTrigger = true;
  }

  /**
   * This method is called in order to send a response back.
   */
  void respond([List params=const [], ActorId replyTo=null]) {
    if (_isTrigger)
      _to.send(_msgName, this._params, replyTo);
    else 
      _to.send(_msgName, params, replyTo);
  }
  
  /** 
   * Returns the name of the message which is responsible to handle this 
   * Reply. 
   */
  String get message() => _msgName;
}

