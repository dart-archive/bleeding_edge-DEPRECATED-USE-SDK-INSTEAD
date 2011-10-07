// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("http_test.dart");
#import("../../../chat/http.dart");
#import("../../../../client/json/dart_json.dart");
#import("../../../chat/chat_server_lib.dart");


class TestServerMain {
  TestServerMain()
      : _statusPort = new ReceivePort(),
        _serverPort = null {
    new TestServer().spawn().then((SendPort port) {
      _serverPort = port;
    });
  }

  void setServerStartedHandler(void startedCallback(int port)) {
    _startedCallback = startedCallback;
  }

  void start() {
    // Handle status messages from the server.
    _statusPort.receive(
        void _(var status, SendPort replyTo) {
          if (status.isStarted) {
            _startedCallback(status.port);
          }
        });

    // Send server start message to the server.
    var command = new TestServerCommand.start();
    _serverPort.send(command, _statusPort.toSendPort());
  }

  void shutdown() {
    // Send server stop message to the server.
    _serverPort.send(new TestServerCommand.stop(), _statusPort.toSendPort());
    _statusPort.close();
  }

  void chunkedEncoding() {
    // Send chunked encoding message to the server.
    _serverPort.send(
        new TestServerCommand.chunkedEncoding(), _statusPort.toSendPort());
    _statusPort.close();
  }

  ReceivePort _statusPort;  // Port for receiving messages from the server.
  SendPort _serverPort;  // Port for sending messages to the server.
  var _startedCallback;
}


class TestServerCommand {
  static final START = 0;
  static final STOP = 1;
  static final CHUNKED_ENCODING = 1;

  TestServerCommand.start() : _command = START;
  TestServerCommand.stop() : _command = STOP;
  TestServerCommand.chunkedEncoding() : _command = CHUNKED_ENCODING;

  bool get isStart() => _command == START;
  bool get isStop() => _command == STOP;
  bool get isChunkedEncoding() => _command == CHUNKED_ENCODING;

  int _command;
}


class TestServerStatus {
  static final STARTED = 0;
  static final STOPPED = 1;
  static final ERROR = 2;

  TestServerStatus.started(this._port) : _state = STARTED;
  TestServerStatus.stopped() : _state = STOPPED;
  TestServerStatus.error() : _state = ERROR;

  bool get isStarted() => _state == STARTED;
  bool get isStopped() => _state == STOPPED;
  bool get isError() => _state == ERROR;

  int get port() => _port;

  int _state;
  int _port;
}


class TestServer extends Isolate {
  // Echo the request content back to the response.
  void _echoHandler(HTTPRequest request, HTTPResponse response) {
    Expect.equals("POST", request.method);
    request.dataEnd =
        void _(String body) {
          response.writeString(body);
          //response.contentLength = body.length;
          //response.writeList(body.charCodes(), 0, body.length);
          response.writeDone();
        };
  }

  // Echo the request content back to the response.
  void _zeroToTenHandler(HTTPRequest request, HTTPResponse response) {
    Expect.equals("GET", request.method);
    request.dataEnd =
        void _(String body) {
          response.writeString("01234567890");
          response.writeDone();
        };
  }

  // Return a 404.
  void _notFoundHandler(HTTPRequest request, HTTPResponse response) {
    response.statusCode = HTTPStatus.NOT_FOUND;
    response.setHeader("Content-Type", "text/html; charset=UTF-8");
    response.writeString("Page not found");
    response.writeDone();
  }

  void main() {
    // Setup request handlers.
    _requestHandlers = new Map();
    _requestHandlers["/echo"] =
        (HTTPRequest request, HTTPResponse response) =>
           _echoHandler(request, response);
    _requestHandlers["/0123456789"] =
        (HTTPRequest request, HTTPResponse response) =>
           _zeroToTenHandler(request, response);

    this.port.receive(
        void _(var message, SendPort replyTo) {
          if (message.isStart) {
            _server = new HTTPServer();
            try {
              _chunkedEncoding = false;
              _server.listen(
                  "127.0.0.1",
                  0,
                  (HTTPRequest req, HTTPResponse rsp) =>
                  _requestReceivedHandler(req, rsp));
              replyTo.send(new TestServerStatus.started(_server.port), null);
            } catch (var e) {
              replyTo.send(new TestServerStatus.error(), null);
            }
          } else if (message.isStop) {
            _server.close();
            this.port.close();
            replyTo.send(new TestServerStatus.stopped(), null);
          } else if (message.isChunkedEncoding) {
            _chunkedEncoding = true;
          }
        });
  }

  void _requestReceivedHandler(HTTPRequest request, HTTPResponse response) {
    var requestHandler =_requestHandlers[request.path];
    if (requestHandler != null) {
      requestHandler(request, response);
    } else {
      _notFoundHandler(request, response);
    }
  }

  HTTPServer _server;  // HTTP server instance.
  Map _requestHandlers;
  bool _chunkedEncoding;
}


void testStartStop() {
  TestServerMain testServerMain = new TestServerMain();
  testServerMain.setServerStartedHandler(
      void _(int port) {
        testServerMain.shutdown();
      });
  testServerMain.start();
}


void testGET() {
  TestServerMain testServerMain = new TestServerMain();
  testServerMain.setServerStartedHandler(
      void _(int port) {
        HTTPClient httpClient = new HTTPClient();
        HTTPClientRequest request = httpClient.open("GET",
                                                    "127.0.0.1",
                                                    port,
                                                    "/0123456789");
        request.responseReceived =
            void _(HTTPClientResponse response) {
              Expect.equals(HTTPStatus.OK, response.statusCode);
              response.dataEnd =
                  void _(String body) {
                    Expect.equals("01234567890", body);
                    httpClient.shutdown();
                    testServerMain.shutdown();
                  };
            };
        request.writeDone();
      });
  testServerMain.start();
}


void testPOST(bool chunkedEncoding) {
  String data = "ABCDEFGHIJKLMONPQRSTUVWXYZ";
  final int kMessageCount = 10;

  TestServerMain testServerMain = new TestServerMain();

  void runTest(int port) {
    int count = 0;
    HTTPClient httpClient = new HTTPClient();
    void sendRequest() {
      HTTPClientRequest request = httpClient.open("POST",
                                                  "127.0.0.1",
                                                  port,
                                                  "/echo");

      request.responseReceived =
          void _(HTTPClientResponse response) {
            Expect.equals(HTTPStatus.OK, response.statusCode);
            response.dataEnd =
                void _(String body) {
                  Expect.equals(data, body);
                  count++;
                  if (count < kMessageCount) {
                    sendRequest();
                  } else {
                    httpClient.shutdown();
                    testServerMain.shutdown();
                  }
                };
          };

      if (chunkedEncoding) {
        request.writeString(data);
      } else {
        request.contentLength = data.length;
        request.writeList(data.charCodes(), 0, data.length);
      }
      request.writeDone();
    }

    sendRequest();
  }

  testServerMain.setServerStartedHandler(runTest);
  testServerMain.start();
  if (chunkedEncoding) {
    testServerMain.chunkedEncoding();
  }
}


void test404() {
  TestServerMain testServerMain = new TestServerMain();
  testServerMain.setServerStartedHandler(
      void _(int port) {
        HTTPClient httpClient = new HTTPClient();
        HTTPClientRequest request = httpClient.open("GET",
                                                    "127.0.0.1",
                                                    port,
                                                    "/thisisnotfound");
        request.writeDone();
        request.keepAlive = false;
        request.responseReceived =
            void _(HTTPClientResponse response) {
              Expect.equals(HTTPStatus.NOT_FOUND, response.statusCode);
              httpClient.shutdown();
              testServerMain.shutdown();
            };
      });
  testServerMain.start();
}


void main() {
  testStartStop();
  testGET();
  testPOST(true);
  testPOST(false);
  test404();
}
