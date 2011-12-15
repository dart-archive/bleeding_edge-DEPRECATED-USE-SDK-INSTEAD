/*
TODO: Use rest/spread arguments if/when (hopefully when!) those become 
available.
*/
typedef MessageHandler([var p1, var p2, var p3, var p4, var p5, var p6, var p7, 
                        var p8, var p9, var p10]);

class ActorId {

  /**
   * The send port that is used to the send a message to the destination actor.
   * This port is connected to the Receive port of the destination actor.
   */
  SendPort _sport;

  ActorId(this._sport);

  void send(String messageName, [List params=const [], ActorId replyTo=null]) =>
    _sport.send(new _Message(messageName, replyTo, params), null);
}

class _Message {
  final String messageName;
  final ActorId replyTo;
  final List params;
  _Message(this.messageName, this.replyTo, this.params);
}


class Actor {

  ReceivePort _port;
  
  /**
   * Map of message names and their associated handlers.
   */
  Map<String, MessageHandler> _handlers;
  ActorId _me;
  
  /**
   * The sender of the current message which is being processed.
   */
  ActorId _currentSender;
  
  /**
   * A proxy object to the DOM Actor.
   */
  WindowProxy _windowProxy;
  ActorId _manager;
  int _uniqueId;
  int _nextId;
  
  /**
   * The name of the current message which is being processed. 
   */
  String _currentMessage;
  
  /**
   * Determines if this actor actor can create other actors. 
   */
  bool _allowCreation;
  
  /**
   * Determines if this actor has access to the DOM.
   */
  bool _allowUIAccess;
  
  int get uid() => _uniqueId;
  
  WindowProxy get ui() => _windowProxy;

  void set ui(WindowProxy win) => _windowProxy = win;

  Map<String, MessageHandler> get on() => _handlers;

  ActorId get me() => _me;

  Actor() : this._fromPort(new ReceivePort());

  Actor._fromPort(ReceivePort p) {
    _nextId = 0;
    _handlers = new Map<String, MessageHandler>();
    _port = p;
    _me = new ActorId(_port.toSendPort());
    _port.receive((_Message m, SendPort replyTo) {
      if (m != null) {
        _currentSender = m.replyTo;
        if (m.messageName != null && m.params != null) {
          if (_handlers[m.messageName] == null) return;
          _currentMessage = m.messageName;
          switch(m.params.length) {
          case 0:
            _handlers[m.messageName]();
            break;
          case 1:
            _handlers[m.messageName](m.params[0]);
            break;
          case 2:
            _handlers[m.messageName](m.params[0], m.params[1]);
            break;
          case 3:
            _handlers[m.messageName](m.params[0], m.params[1], m.params[2]);
            break;
          case 4:
            _handlers[m.messageName](m.params[0], m.params[1], m.params[2],
                m.params[3]);
            break;
          case 5:
            _handlers[m.messageName](m.params[0], m.params[1], m.params[2],
                m.params[3], m.params[4]);
            break;
          case 6:
            _handlers[m.messageName](m.params[0], m.params[1], m.params[2],
                m.params[3], m.params[4], m.params[5]);
            break;
          case 7:
            _handlers[m.messageName](m.params[0], m.params[1], m.params[2],
                m.params[3], m.params[4], m.params[5], m.params[6]);
            break;
          case 8:
            _handlers[m.messageName](m.params[0], m.params[1], m.params[2],
               m.params[3], m.params[4], m.params[5], m.params[6], m.params[7]);
            break;
          case 9:
            _handlers[m.messageName](m.params[0], m.params[1], m.params[2],
                m.params[3], m.params[4], m.params[5], m.params[6], m.params[7],
                m.params[8]);
            break;
          case 10:
            _handlers[m.messageName](m.params[0], m.params[1], m.params[2],
                m.params[3], m.params[4], m.params[5], m.params[6], m.params[7],
                m.params[8], m.params[9]);
            break;
          default:
            break;
          }
        }
      }
    });
  }

  // Sends a message back the the actor which has sent the current message
  void reply(String messageName, [List params=const []]) =>
    send(_currentSender, messageName, params);

  // Sends a message to an actor with id specified by receiverId.
  void send(ActorId receiverId, String messageName, [List params=const []]) =>
    receiverId.send(messageName, params, me);

  // Creates an actor and sends a message to it
  void create(ActorFactory actorFactory, String messageName, 
      [List params=const[], bool allowUIAccess=true, bool allowCreation=true]) {
    if (_allowCreation)
      _manager.send("create", 
          [actorFactory, new _Message(messageName, me, params), 
           _allowUIAccess && allowUIAccess, allowCreation], me);
  }
  
  // Creates an actor in an separate isolate and sends a message to it
  void createIsolated(ActorFactory isolatedFactory, 
        String messageName, [List params=const [], 
        bool allowUIAccess=true, bool allowCreation=true]) {
    if (_allowCreation)
      _manager.send("createIsolated", 
          [isolatedFactory, new _Message(messageName, me, params),
           _allowUIAccess && allowUIAccess, allowCreation], me);
  }
  
  // Tells actor manager to shutdown the whole process.
  void halt() {
    if (_allowCreation) _manager.send("halt");
  }
  
  String genId() {
    _nextId++;
    return "${uid}_${_nextId}";
  }

  /**
   * Removes the message handler for the specified message name, 'msg'.
   */
  void removeMessage(String msg) {
    _handlers.remove(msg);
  }
  
  /**
   * Removes the message handler for current message that is being processed.
   */
  void removeCurrentMessage() {
    removeMessage(_currentMessage);
  }
  
  /**
   * Creates a new message handler with the specified message handler, 'mh'. 
   */
  Reply messageback(var mh) {
    String msgName = "__callback__${genId()}";
    on[msgName] = mh;
    return new Reply(me, msgName);
  }
}

/**
 * Interface implemented by clients to create new actors.
 */
interface ActorFactory {
  
  const ActorFactory();
  
  Actor create();
}
