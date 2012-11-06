// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
//
// VMOptions=
// VMOptions=--short_socket_read
// VMOptions=--short_socket_write
// VMOptions=--short_socket_read --short_socket_write

#import("dart:io");
#import("dart:isolate");
#import("dart:json");
#import("../../../chat/chat_server_lib.dart");

// Message to start chat test client running in an isolate.
class ChatTestClientStart {
  ChatTestClientStart(int this.totalClients,
                      int this.messagesToSend,
                      int this.messagesToReceive,
                      int this.port);

  int totalClients;
  int messagesToSend;
  int messagesToReceive;
  int port;
}


void startChatTestClient() {
  var client = new ChatTestClient();
  port.receive(client.dispatch);
}


// Chat server test client for running in a separate isolate. When
// this test client is started it will join the chat topic, send a
// number of messages, receive the expected number of messages and
// leave the topic.
class ChatTestClient {
  SendPort statusPort;  // Port to reply to when test has finished.
  HttpClient httpClient;  // HTTP client connection factory.

  int totalClients;  // Total number of clients in the test.
  int messagesToSend;  // Number of messages to send.
  int messagesToReceive;  // Numbe rof messages expected to be received.
  int port;  // TCP/IP port for server.

  String sessionId;  // Session id when connected.
  int sendMessageNumber;  // Number of messages sent.
  int joinCount;
  int messageCount;
  int receiveMessageNumber;  // Number of messages received.

  void leave() {
    HttpClientRequest request;
    HttpClientResponse response;

    void leaveResponseHandler(String data) {
      Expect.equals(HttpStatus.OK, response.statusCode);
      var responseData = JSON.parse(data);
      Expect.equals("leave", responseData["response"]);

      // Test done.
      statusPort.send("Test succeeded", null);
    }

    Map leaveRequest = new Map();
    leaveRequest["request"] = "leave";
    leaveRequest["sessionId"] = sessionId;
    HttpClientConnection conn = httpClient.post("127.0.0.1", port, "/leave");
    conn.onRequest = (HttpClientRequest request) {
      request.outputStream.writeString(JSON.stringify(leaveRequest));
      request.outputStream.close();
    };
    conn.onResponse = (HttpClientResponse r) {
      response = r;
      StringInputStream stream = new StringInputStream(response.inputStream);
      StringBuffer body = new StringBuffer();
      stream.onData = () => body.add(stream.read());
      stream.onClosed = () => leaveResponseHandler(body.toString());
    };
  }

  void receive() {
    HttpClientRequest request;
    HttpClientResponse response;

    void receiveResponseHandler(String data) {
      Expect.equals(HttpStatus.OK, response.statusCode);
      var responseData = JSON.parse(data);
      Expect.equals("receive", responseData["response"]);
      Expect.equals(null, responseData["disconnect"]);
      for (int i = 0; i < responseData["messages"].length; i++) {
        Map message = responseData["messages"][i];
        if (message["type"] == "join") {
          joinCount++;
        } else if (message["type"] == "message") {
          messageCount++;
        } else {
          Expect.equals("leave", message["type"]);
        }
        if (totalClients == 1) {
          // Test the exact messages when this is the only client.
          Expect.equals(messagesToSend + 1, responseData["messages"].length);
          Expect.equals(i, message["number"]);
          if (i == 0) {
            Expect.equals("join", message["type"]);
          } else {
            Expect.equals("message", message["type"]);
            Expect.equals("message ${i - 1}", message["message"]);
          }
        }
        receiveMessageNumber = message["number"] + 1;
      }

      // Receive all expected messages then leave.
      if (messageCount < messagesToReceive) {
        receive();
      } else {
        Expect.equals(messagesToReceive, messageCount);
        Expect.equals(totalClients, joinCount);
        leave();
      }
    }

    Map receiveRequest = new Map();
    receiveRequest["request"] = "receive";
    receiveRequest["sessionId"] = sessionId;
    receiveRequest["nextMessage"] = receiveMessageNumber;
    HttpClientConnection conn =
        httpClient.post("127.0.0.1", port, "/receive");
    conn.onRequest = (HttpClientRequest request) {
      request.outputStream.writeString(JSON.stringify(receiveRequest));
      request.outputStream.close();
    };
    conn.onResponse = (HttpClientResponse r) {
      response = r;
      StringInputStream stream = new StringInputStream(response.inputStream);
      StringBuffer body = new StringBuffer();
      stream.onData = () => body.add(stream.read());
      stream.onClosed = () => receiveResponseHandler(body.toString());
    };
  }

  void sendMessage() {
    HttpClientRequest request;
    HttpClientResponse response;

    void sendResponseHandler(String data) {
      Expect.equals(HttpStatus.OK, response.statusCode);
      var responseData = JSON.parse(data);
      Expect.equals("message", responseData["response"]);
      sendMessageNumber++;
      if (sendMessageNumber < messagesToSend) {
        sendMessage();
      } else {
        receive();
      }
    }

    Map messageRequest = new Map();
    messageRequest["request"] = "message";
    messageRequest["sessionId"] = sessionId;
    messageRequest["message"] = "message $sendMessageNumber";
    HttpClientConnection conn =
        httpClient.post("127.0.0.1", port, "/message");
    conn.onRequest = (HttpClientRequest request) {
      request.outputStream.writeString(JSON.stringify(messageRequest));
      request.outputStream.close();
    };
    conn.onResponse = (HttpClientResponse r) {
      response = r;
      StringInputStream stream = new StringInputStream(response.inputStream);
      StringBuffer body = new StringBuffer();
      stream.onData = () => body.add(stream.read());
      stream.onClosed = () => sendResponseHandler(body.toString());
    };
  }

  void join() {
    HttpClientRequest request;
    HttpClientResponse response;

    void joinResponseHandler(String data) {
      Expect.equals(HttpStatus.OK, response.statusCode);
      var responseData = JSON.parse(data);
      Expect.equals("join", responseData["response"]);
      sessionId = responseData["sessionId"];
      Expect.isTrue(sessionId != null);

      joinCount = 0;
      messageCount = 0;
      sendMessageNumber = 0;
      receiveMessageNumber = 0;
      sendMessage();
    }

    Map joinRequest = new Map();
    joinRequest["request"] = "join";
    joinRequest["handle"] = "test1";
    HttpClientConnection conn = httpClient.post("127.0.0.1", port, "/join");
    conn.onRequest = (HttpClientRequest request) {
      request.outputStream.writeString(JSON.stringify(joinRequest));
      request.outputStream.close();
    };
    conn.onResponse = (HttpClientResponse r) {
      response = r;
      StringInputStream stream = new StringInputStream(response.inputStream);
      StringBuffer body = new StringBuffer();
      stream.onData = () => body.add(stream.read());
      stream.onClosed = () => joinResponseHandler(body.toString());
    };
  }

  void dispatch(message, replyTo) {
    totalClients = message.totalClients;
    messagesToSend = message.messagesToSend;
    messagesToReceive = message.messagesToReceive;
    port = message.port;
    statusPort = replyTo;

    // Create a HTTP client factory.
    httpClient = new HttpClient();

    // Start the client by joining the chat topic.
    join();
  }
}


class TestMain {
  TestMain.run(int this.clientCount, int this.messageCount)
      : serverStatusPort = new ReceivePort(),
        serverPort = null,
        finishedClients = 0 {
    serverPort = spawnFunction(startChatServer);
    start();
  }

  void start() {
    // Handle status messages from the server.
    serverStatusPort.receive(
        (var message, SendPort replyTo) {
          if (message.isStarted) {
            // When the server is started start all test clients.
            for (int i = 0; i < clientCount; i++) {
              ChatTestClientStart command =
                  new ChatTestClientStart(clientCount,
                                          messageCount,
                                          messageCount * this.clientCount,
                                          message.port);
              clientPorts[i].send(command, clientStatusPorts[i].toSendPort());
            }
          } else if (message.isError) {
            print("Could not start server - probably error \"Address already in use\" from bind.");
            serverStatusPort.close();
          }
        });

    // Prepare the requested number of clients.
    clientPorts = new List<SendPort>(clientCount);
    int liveClientsCount = 0;
    clientStatusPorts = new List<ReceivePort>(clientCount);
    for (int i = 0; i < clientCount; i++) {
      ReceivePort statusPort = new ReceivePort();
      statusPort.receive((var message, SendPort replyTo) {
        // First and only message from the client indicates that
        // the test is done.
        Expect.equals("Test succeeded", message);
        statusPort.close();
        finishedClients++;

        // If all clients are finished shutdown the server.
        if (finishedClients == clientCount) {
          // Send server stop message to the server.
          serverPort.send(new ChatServerCommand.stop(),
                          serverStatusPort.toSendPort());

          // Close the last port to terminate the test.
          serverStatusPort.close();
        }
      });

      clientStatusPorts[i] = statusPort;
      clientPorts[i] = spawnFunction(startChatTestClient);
      liveClientsCount++;
      if (liveClientsCount == clientCount) {
        // Once all clients are running send server start message to
        // the server. Use port 0 for an ephemeral port. The actual
        // port will be returned with the server started
        // message. Use a backlog equal to the client count to avoid
        // connection issues.
        serverPort.send(new ChatServerCommand.start("127.0.0.1",
                                                    0,
                                                    backlog: clientCount),
                        serverStatusPort.toSendPort());
      }
    }
  }

  int clientCount;  // Number of clients to run.
  int messageCount;  // Number of messages per clients to send.
  int finishedClients;  // Number of clients finished.

  // Ports for communicating with the server.
  SendPort serverPort;
  ReceivePort serverStatusPort;
  // Ports for communicating with the clients.
  List<SendPort> clientPorts;
  List<ReceivePort> clientStatusPorts;
}


void testOneClient() {
  TestMain testMain = new TestMain.run(1, 5);
}


void testTwoClients() {
  TestMain testMain = new TestMain.run(2, 5);
}


void testTwoClientsMoreMessages() {
  TestMain testMain = new TestMain.run(2, 10);
}


void testTenClients() {
  TestMain testMain = new TestMain.run(10, 2);
}


void main() {
  testOneClient();
  testTwoClients();
  testTwoClientsMoreMessages();
  testTenClients();
}
