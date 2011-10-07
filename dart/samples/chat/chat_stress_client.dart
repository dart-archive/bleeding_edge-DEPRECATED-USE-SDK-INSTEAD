// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("chat_stress_client.dart");
#import("http.dart");
#import("../../client/json/dart_json.dart");


class ChatStressClient {
  ChatStressClient() : verbose = false, messagesToSend = 100;

  void run() {
    HTTPClient httpClient;  // HTTP client connection factory.
    String sessionId;  // Session id when connected.
    int sendMessageCount;  // Number of messages sent.
    int messageCount;
    int receiveMessageCount;  // Number of messages received.

    int port;

    void printServerError(HTTPClientResponse response) {
      print("Server error \"" +
                     response.statusCode +
                     " " +
                     response.reasonPhrase + "\"");
    }

    void printProtocolError() {
      print("Protocol error");
    }

    Map parseResponse(HTTPClientResponse response,
                      String data,
                      String expected) {
      if (response.statusCode != HTTPStatus.OK) {
        printServerError(response);
        return null;
      }
      var responseData = JSON.parse(data);
      if (responseData["response"] != expected ) {
        printProtocolError();
        return null;
      }
      return responseData;
    }

    void leave() {
      HTTPClientRequest request;
      HTTPClientResponse response;

      void leaveResponseHandler(String data) {
        httpClient.shutdown();
      }

      Map leaveRequest = new Map();
      leaveRequest["request"] = "leave";
      leaveRequest["sessionId"] = sessionId;
      request = httpClient.open("POST", "127.0.0.1", port, "/leave");
      request.writeString(JSON.stringify(leaveRequest));
      request.responseReceived =
          void _(HTTPClientResponse r) {
            response = r;
            response.dataEnd = leaveResponseHandler;
          };
      request.writeDone();
    }

    var sendMessage;
    void receive() {
      HTTPClientRequest request;
      HTTPClientResponse response;

      void receiveResponseHandler(String data) {
        var responseData = parseResponse(response, data, "receive");
        if (responseData == null) return;
        if (responseData["disconnect"] == true) return;

        sendMessage();
      }

      Map messageRequest = new Map();
      messageRequest["request"] = "receive";
      messageRequest["sessionId"] = sessionId;
      messageRequest["nextMessage"] = receiveMessageCount;
      messageRequest["maxMessages"] = 100;
      request = httpClient.open("POST", "127.0.0.1", port, "/receive");
      request.writeString(JSON.stringify(messageRequest));
      request.responseReceived =
          void _(HTTPClientResponse r) {
            response = r;
            response.dataEnd = receiveResponseHandler;
          };
      request.writeDone();
    }

    sendMessage = void _() {
      HTTPClientRequest request;
      HTTPClientResponse response;

      void sendResponseHandler(String data) {
        var responseData = parseResponse(response, data, "message");
        if (responseData == null) return;

        sendMessageCount++;
        if (verbose) {
          if (sendMessageCount % 10 == 0) {
            print("$sendMessageCount messages");
          }
        }
        if (sendMessageCount < messagesToSend) {
          receive();
        } else {
          leave();
        }
      }

      Map messageRequest = new Map();
      messageRequest["request"] = "message";
      messageRequest["sessionId"] = sessionId;
      messageRequest["message"] = "message " + sendMessageCount;
      request = httpClient.open("POST", "127.0.0.1", port, "/message");
      request.writeString(JSON.stringify(messageRequest));
      request.responseReceived =
          void _(HTTPClientResponse r) {
            response = r;
            response.dataEnd = sendResponseHandler;
          };
      request.writeDone();
    };

    void join() {
      HTTPClientRequest request;
      HTTPClientResponse response;

      void joinResponseHandler(String data) {
        var responseData = parseResponse(response, data, "join");
        if (responseData == null) return;
        sessionId = responseData["sessionId"];

        messageCount = 0;
        sendMessageCount = 0;
        receiveMessageCount = 0;
        sendMessage();
      }

      Map joinRequest = new Map();
      joinRequest["request"] = "join";
      joinRequest["handle"] = "test1";
      request = httpClient.open("POST", "127.0.0.1", port, "/join");
      request.writeString(JSON.stringify(joinRequest));
      request.responseReceived =
          void _(HTTPClientResponse r) {
            response = r;
            response.dataEnd = joinResponseHandler;
          };
      request.writeDone();
    }

    // Create a HTTP client factory.
    httpClient = new HTTPClient();
    port = 8123;

    // Start the client by joining the chat topic.
    join();
  }

  int messagesToSend;
  bool verbose;
}


void main () {
  ChatStressClient stresser = new ChatStressClient();
  stresser.run();
}
