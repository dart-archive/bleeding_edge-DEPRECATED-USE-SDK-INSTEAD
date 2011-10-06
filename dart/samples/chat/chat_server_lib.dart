// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("chat_server");
#import("http.dart");
#import("../../client/json/dart_json.dart");


class ServerMain {
  ServerMain.start()
      : _statusPort = new ReceivePort(),
        _serverPort = null {
    new ChatServer().spawn().then((SendPort port) {
      _serverPort = port;
      start();
    });
  }

  void start() {
    // Handle status messages from the server.
    _statusPort.receive(
        void _(var message, SendPort replyTo) {
          String status = message.message;
          print("Received status: $status");
        });

    // Send server start message to the server.
    var command = new ChatServerCommand.start(ChatServer.DEFAULT_HOST,
                                              ChatServer.DEFAULT_PORT,
                                              true);
    _serverPort.send(command, _statusPort.toSendPort());
  }

  void shutdown() {
    // Send server stop message to the server.
    _serverPort.send(new ChatServerCommand.stop(), _statusPort.toSendPort());
    _statusPort.close();
  }

  ReceivePort _statusPort;  // Port for receiving messages from the server.
  SendPort _serverPort;  // Port for sending messages to the server.
}


class User {
  User(this._handle) {
    // TODO(sgjesse) Generate more secure and unique session id's.
    _sessionId = 'a' + ((Math.random() * 1000000).toInt()).toString();
    markActivity();
  }

  void markActivity() => _lastActive = new Date.now();
  Duration idleTime(Date now) => now.difference(_lastActive);

  String get handle() => _handle;
  String get sessionId() => _sessionId;

  String _handle;
  String _sessionId;
  Date _lastActive;
}


class Message {
  static final int JOIN = 0;
  static final int MESSAGE = 1;
  static final int LEAVE = 2;
  static final int TIMEOUT = 2;
  static final List<String> _typeName =
      const [ "join", "message", "leave", "timeout"];

  Message.join(this._from)
      : _received = new Date.now(), _type = JOIN;
  Message(this._from, this._message)
      : _received = new Date.now(), _type = MESSAGE;
  Message.leave(this._from)
      : _received = new Date.now(), _type = LEAVE;
  Message.timeout(this._from)
      : _received = new Date.now(), _type = TIMEOUT;

  User get from() => _from;
  Date get received() => _received;
  String get message() => _message;
  void set messageNumber(int n) => _messageNumber = n;

  Map toMap() {
    Map map = new Map();
    map["from"] = _from.handle;
    map["received"] = _received.toString();
    map["type"] = _typeName[_type];
    if (_type == MESSAGE) map["message"] = _message;
    map["number"] = _messageNumber;
    return map;
  }

  User _from;
  Date _received;
  int _type;
  String _message;
  int _messageNumber;
}


class Topic {
  static final int DEFAULT_IDLE_TIMEOUT = 60 * 60 * 1000;  // One hour.
  Topic()
      : _activeUsers = new Map(),
        _messages = new List(),
        _nextMessageNumber = 0,
        _callbacks = new Map();

  int get activeUsers() => _activeUsers.length;

  User _userJoined(String handle) {
    User user = new User(handle);
    _activeUsers[user.sessionId] = user;
    Message message = new Message.join(user);
    _addMessage(message);
    return user;
  }

  User _userLookup(String sessionId) => _activeUsers[sessionId];

  void _userLeft(String sessionId) {
    User user = _userLookup(sessionId);
    Message message = new Message.leave(user);
    _addMessage(message);
    _activeUsers.remove(sessionId);
  }

  bool _addMessage(Message message) {
    message.messageNumber = _nextMessageNumber++;
    _messages.add(message);

    // Send the new message to all polling clients.
    List messages = new List();
    messages.add(message.toMap());
    _callbacks.forEach(
        void _(String sessionId, Function callback) {
          callback(messages);
        });
    _callbacks = new Map();
  }

  bool _userMessage(Map requestData) {
    String sessionId = requestData["sessionId"];
    User user = _userLookup(sessionId);
    if (user == null) return false;
    String handle = user.handle;
    String messageText = requestData["message"];
    if (messageText == null) return false;

    // Add new message.
    Message message = new Message(user, messageText);
    _addMessage(message);
    user.markActivity();

    return true;
  }

  List messagesFrom(int messageNumber, int maxMessages) {
    if (_messages.length > messageNumber) {
      if (maxMessages != null) {
        if (_messages.length - messageNumber > maxMessages) {
          messageNumber = _messages.length - maxMessages;
        }
      }
      List messages = new List();
      for (int i = messageNumber; i < _messages.length; i++) {
        messages.add(_messages[i].toMap());
      }
      return messages;
    } else {
      return null;
    }
  }

  void registerChangeCallback(String sessionId, var callback) {
    _callbacks[sessionId] = callback;
  }

  void _handleTimer(Timer timer) {
    Set inactiveSessions = new Set();
    // Collect all sessions which have not been active for some time.
    Date now = new Date.now();
    _activeUsers.forEach(
        void _(String sessionId, User user) {
          if (user.idleTime(now).inMilliseconds > DEFAULT_IDLE_TIMEOUT) {
            inactiveSessions.add(sessionId);
          }
        });
    // Terminate the inactive sessions.
    inactiveSessions.forEach(
        void _(String sessionId) {
          Function callback = _callbacks.remove(sessionId);
          if (callback != null) callback(null);
          User user = _activeUsers.remove(sessionId);
          Message message = new Message.timeout(user);
          _addMessage(message);
        });

  }

  Map<String, User> _activeUsers;
  List<Message> _messages;
  int _nextMessageNumber;
  Map<String, Function> _callbacks;
}


class ChatServerCommand {
  static final START = 0;
  static final STOP = 1;

  ChatServerCommand.start([String this._host = ChatServer.DEFAULT_HOST,
                           int this._port = ChatServer.DEFAULT_PORT,
                           bool this._logging = false])
      : _command = START;
  ChatServerCommand.stop() : _command = STOP;

  bool get isStart() => _command == START;
  bool get isStop() => _command == STOP;

  String get host() => _host;
  int get port() => _port;
  bool get logging() => _logging;

  int _command;
  String _host;
  int _port;
  bool _logging;
}


class ChatServerStatus {
  static final STARTING = 0;
  static final STARTED = 1;
  static final STOPPING = 2;
  static final STOPPED = 3;
  static final ERROR = 4;

  ChatServerStatus(this._state, this._message);
  ChatServerStatus.starting() : _state = STARTING;
  ChatServerStatus.started(this._port) : _state = STARTED;
  ChatServerStatus.stopping() : _state = STOPPING;
  ChatServerStatus.stopped() : _state = STOPPED;
  ChatServerStatus.error() : _state = ERROR;

  bool get isStarting() => _state == STARTING;
  bool get isStarted() => _state == STARTED;
  bool get isStopping() => _state == STOPPING;
  bool get isStopped() => _state == STOPPED;
  bool get isError() => _state == ERROR;

  int get state() => _state;
  String get message() {
    if (_message != null) return _message;
    switch (_state) {
      case STARTING: return "Server starting";
      case STARTED: return "Server listening";
      case STOPPING: return "Server stopping";
      case STOPPED: return "Server stopped";
      case ERROR: return "Server error";
    }
  }

  int get port() => _port;

  int _state;
  String _message;
  int _port;
}


class ChatServer extends Isolate {
  static final DEFAULT_PORT = 8123;
  static final DEFAULT_HOST = "127.0.0.1";
  static final String redirectPageHtml = """
<html>
<head><title>Welcome to the dart server</title></head>
<body><h1>Redirecting to the front page...</h1></body>
</html>""";
  static final String notFoundPageHtml = """
<html><head>
<title>404 Not Found</title>
</head><body>
<h1>Not Found</h1>
<p>The requested URL was not found on this server.</p>
</body></html>""";

  void _sendJSONResponse(HTTPResponse response, Map responseData) {
    response.setHeader("Content-Type", "application/json; charset=UTF-8");
    response.writeString(JSON.stringify(responseData));
    response.writeDone();
  }

  // The front page just redirects to index.html.
  void _frontPageHandler(HTTPRequest request, HTTPResponse response) {
    if (_redirectPage == null) {
      _redirectPage = redirectPageHtml.charCodes();
    }
    response.statusCode = HTTPStatus.FOUND;
    response.setHeader(
        "Location", "http://$_host:$_port/dart_client/index.html");
    response.contentLength = _redirectPage.length;
    response.writeList(_redirectPage, 0, _redirectPage.length);
    response.writeDone();
  }

  // Serve the content of a file.
  void _fileHandler(
      HTTPRequest request, HTTPResponse response, [String fileName = null]) {
    final int BUFFER_SIZE = 4096;
    if (fileName == null) {
      fileName = request.path.substringToEnd(1);
    }
    File file = new File(fileName, false);
    if (file != null) {
      int totalRead = 0;
      List<int> buffer = new List<int>(BUFFER_SIZE);

      String mimeType = "text/html; charset=UTF-8";
      int lastDot = fileName.lastIndexOf(".", fileName.length);
      if (lastDot != -1) {
        String extension = fileName.substringToEnd(lastDot);
        if (extension == ".js") { mimeType = "application/javascript"; }
        if (extension == ".ico") { mimeType = "image/vnd.microsoft.icon"; }
      }
      response.setHeader("Content-Type", mimeType);
      response.contentLength = file.length;

      void writeFileData() {
        while (totalRead < file.length) {
          var read = file.readList(buffer, 0, BUFFER_SIZE);
          totalRead += read;

          // Write this buffer and get a callback when it makes sense
          // to write more.
          bool allWritten = response.writeList(buffer, 0, read, writeFileData);
          if (!allWritten) break;
        }

        if (totalRead == file.length) {
          response.writeDone();
        }
      }

      writeFileData();
    } else {
      _notFoundHandler(request, response);
    }
  }

  // Serve the not found page.
  void _notFoundHandler(HTTPRequest request, HTTPResponse response) {
    if (_notFoundPage == null) {
      _notFoundPage = notFoundPageHtml.charCodes();
    }
    response.statusCode = HTTPStatus.NOT_FOUND;
    response.setHeader("Content-Type", "text/html; charset=UTF-8");
    response.contentLength = _notFoundPage.length;
    response.writeList(_notFoundPage, 0, _notFoundPage.length);
    response.writeDone();
  }

  // Unexpected protocol data.
  void _protocolError(HTTPRequest request, HTTPResponse response) {
    response.statusCode = HTTPStatus.INTERNAL_ERROR;
    response.contentLength = 0;
    response.writeDone();
  }

  // Join request:
  // { "request": "join",
  //   "handle": <handle> }
  void _joinHandler(HTTPRequest request, HTTPResponse response) {
    void dataEndHandler(String data) {
      if (data != null) {
        var requestData = JSON.parse(data);
        if (requestData["request"] == "join") {
          String handle = requestData["handle"];
          if (handle != null) {
            // New user joining.
            User user = _topic._userJoined(handle);

            // Send response.
            Map responseData = new Map();
            responseData["response"] = "join";
            responseData["sessionId"] = user.sessionId;
            _sendJSONResponse(response, responseData);
          } else {
            _protocolError(request, response);
          }
        } else {
          _protocolError(request, response);
        }
      } else {
        _protocolError(request, response);
      }
    }

    // Register callback for full request data.
    request.dataEnd = dataEndHandler;
  }

  // Leave request:
  // { "request": "leave",
  //   "sessionId": <sessionId> }
  void _leaveHandler(HTTPRequest request, HTTPResponse response) {
    void dataEndHandler(String data) {
      var requestData = JSON.parse(data);
      if (requestData["request"] == "leave") {
        String sessionId = requestData["sessionId"];
        if (sessionId != null) {
          // User leaving.
          _topic._userLeft(sessionId);

          // Send response.
          Map responseData = new Map();
          responseData["response"] = "leave";
          _sendJSONResponse(response, responseData);
        } else {
          _protocolError(request, response);
        }
      } else {
        _protocolError(request, response);
      }
    }

    request.dataEnd = dataEndHandler;
  }

  // Message request:
  // { "request": "message",
  //   "sessionId": <sessionId>,
  //   "message": <message> }
  void _messageHandler(HTTPRequest request, HTTPResponse response) {
    void dataEndHandler(String data) {
      _messageCount++;
      _messageRate.record(1);
      var requestData = JSON.parse(data);
      if (requestData["request"] == "message") {
        String sessionId = requestData["sessionId"];
        if (sessionId != null) {
          // New message from user.
          bool success = _topic._userMessage(requestData);

          // Send response.
          if (success) {
            Map responseData = new Map();
            responseData["response"] = "message";
            _sendJSONResponse(response, responseData);
          } else {
            _protocolError(request, response);
          }
        } else {
          _protocolError(request, response);
        }
      } else {
        _protocolError(request, response);
      }
    }

    request.dataEnd = dataEndHandler;
  }

  // Receive request:
  // { "request": "receive",
  //   "sessionId": <sessionId>,
  //   "nextMessage": <nextMessage>,
  //   "maxMessages": <maxMesssages> }
  void _receiveHandler(HTTPRequest request, HTTPResponse response) {
    void dataEndHandler(String data) {
      var requestData = JSON.parse(data);
      if (requestData["request"] == "receive") {
        String sessionId = requestData["sessionId"];
        int nextMessage = requestData["nextMessage"];
        int maxMessages = requestData["maxMessages"];
        if (sessionId != null && nextMessage != null) {

          void sendResponse(messages) {
            // Send response.
            Map responseData = new Map();
            responseData["response"] = "receive";
            if (messages != null) {
              responseData["messages"] = messages;
              responseData["activeUsers"] = _topic.activeUsers;
              responseData["upTime"] =
                  new Date.now().difference(_serverStart).inMilliseconds;
            } else {
              responseData["disconnect"] = true;
            }
            _sendJSONResponse(response, responseData);
          }

          // Receive request from user.
          List messages = _topic.messagesFrom(nextMessage, maxMessages);
          if (messages == null) {
            _topic.registerChangeCallback(sessionId, sendResponse);
          } else {
            sendResponse(messages);
          }

        } else {
          _protocolError(request, response);
        }
      } else {
        _protocolError(request, response);
      }
    }

    request.dataEnd = dataEndHandler;
  }

  void main() {
    _logRequests = false;
    _topic = new Topic();
    _serverStart = new Date.now();
    _messageCount = 0;
    _messageRate = new Rate();

    // Setup request handlers.
    _requestHandlers = new Map();
    _requestHandlers["/"] =
        (HTTPRequest request, HTTPResponse response) =>
           _frontPageHandler(request, response);
    _requestHandlers["/js_client/index.html"] =
        (HTTPRequest request, HTTPResponse response) =>
           _fileHandler(request, response);
    _requestHandlers["/js_client/code.js"] =
        (HTTPRequest request, HTTPResponse response) =>
           _fileHandler(request, response);
    _requestHandlers["/dart_client/index.html"] =
        (HTTPRequest request, HTTPResponse response) =>
           _fileHandler(request, response);
    _requestHandlers["/dart_client/out/src/chat.app.js"] =
        (HTTPRequest request, HTTPResponse response) =>
           _fileHandler(request, response);
    _requestHandlers["/favicon.ico"] =
        (HTTPRequest request, HTTPResponse response) =>
        _fileHandler(request, response, "static/favicon.ico");

    _requestHandlers["/join"] =
        (HTTPRequest request, HTTPResponse response) =>
           _joinHandler(request, response);
    _requestHandlers["/leave"] =
        (HTTPRequest request, HTTPResponse response) =>
           _leaveHandler(request, response);
    _requestHandlers["/message"] =
        (HTTPRequest request, HTTPResponse response) =>
           _messageHandler(request, response);
    _requestHandlers["/receive"] =
        (HTTPRequest request, HTTPResponse response) =>
           _receiveHandler(request, response);

    // Start a timer for cleanup events.
    _cleanupTimer =
        new Timer((timer) => _topic._handleTimer(timer), 10000, true);

    // Start timer for periodic logging.
    void _handleLogging(Timer timer) {
      if (_logging) {
        print((_messageRate.rate).toString() +
                       " messages/s (total " +
                       _messageCount +
                       " messages)");
      }
    }

    this.port.receive(
        void _(var message, SendPort replyTo) {
          if (message.isStart) {
            _host = message.host;
            _port = message.port;
            _logging = message.logging;
            replyTo.send(new ChatServerStatus.starting(), null);
            _server = new HTTPServer();
            try {
              _server.listen(
                  _host,
                  _port,
                  (HTTPRequest req, HTTPResponse rsp) =>
                  _requestReceivedHandler(req, rsp));
              replyTo.send(new ChatServerStatus.started(_server.port), null);
              _loggingTimer = new Timer(_handleLogging, 1000, true);
            } catch (var e) {
              replyTo.send(new ChatServerStatus.error(), null);
            }
          } else if (message.isStop) {
            replyTo.send(new ChatServerStatus.stopping(), null);
            _cleanupTimer.cancel();
            _server.close();
            this.port.close();
            replyTo.send(new ChatServerStatus.stopped(), null);
          }
        });
  }

  void _requestReceivedHandler(HTTPRequest request, HTTPResponse response) {
    if (_logRequests) {
      String method = request.method;
      String uri = request.uri;
      print("Request: $method $uri");
      print("Request headers:");
      request.headers.forEach(
          (String name, String value) => print("$name: $value"));
      print("Request parameters:");
      request.queryParameters.forEach(
          (String name, String value) => print("$name = $value"));
      print("");
    }

    var requestHandler =_requestHandlers[request.path];
    if (requestHandler != null) {
      requestHandler(request, response);
    } else {
      _notFoundHandler(request, response);
    }
  }

  String _host;
  int _port;
  HTTPServer _server;  // HTTP server instance.
  Map _requestHandlers;
  bool _logRequests;

  Topic _topic;
  Timer _cleanupTimer;
  Timer _loggingTimer;
  Date _serverStart;

  bool _logging;
  int _messageCount;
  Rate _messageRate;

  // Static HTML.
  List<int> _redirectPage;
  List<int> _notFoundPage;
}


// Calculate the rate of events over a given time range. The time
// range is split over a number of buckets where each bucket collects
// the number of events happening in that time sub-range. The first
// constructor arument specifies the time range in milliseconds. The
// buckets are in the list _buckets organized at a circular buffer
// with _currentBucket marking the bucket where an event was last
// recorded. A current sum of the content of all buckets except the
// one pointed a by _currentBucket is kept in _sum.
class Rate {
  Rate([int this._timeRange = 1000, int buckets = 10])
      : _buckets = new List(buckets + 1),  // Current bucket is not in the sum.
        _currentBucket = 0,
        _currentBucketTime = new Date.now().value,
        _bucketTimeRange = (_timeRange / buckets).toInt(),
        _sum = 0 {
    for (int i = 0; i < _buckets.length; i++) {
      _buckets[i] = 0;
    }
  }

  // Record the specified number of events.
  void record(int count) {
    _timePassed();
    _buckets[_currentBucket] = _buckets[_currentBucket] + count;
  }

  // Returns the current rate of events for the time range.
  num get rate() {
    _timePassed();
    return _sum;
  }

  // Update the current sum as time passes. If time has passed by the
  // current bucket add it to the sum and move forward to the bucket
  // matching the current time. Subtract all buckets vacated from the
  // sum as bucket for current time is located.
  void _timePassed() {
    int time = new Date.now().value;
    if (time < _currentBucketTime + _bucketTimeRange) {
      // Still same bucket.
      return;
    }

    // Add collected bucket to the sum.
    _sum += _buckets[_currentBucket];

    // Find the bucket for the current time. Subtract all buckets
    // reused from the sum.
    while (time >= _currentBucketTime + _bucketTimeRange) {
      _currentBucket = (_currentBucket + 1) % _buckets.length;
      _sum -= _buckets[_currentBucket];
      _buckets[_currentBucket] = 0;
      _currentBucketTime += _bucketTimeRange;
    }
  }

  int _timeRange;
  List<int> _buckets;
  int _currentBucket;
  int _currentBucketTime;
  num _bucketTimeRange;
  int _sum;
}
