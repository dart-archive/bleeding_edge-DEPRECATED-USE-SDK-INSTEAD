// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library chat_stress_client;
import 'dart:io';
import 'dart:json' as json;


class ChatStressClient {
  ChatStressClient() : verbose = false, messagesToSend = 100;

  void run() {
    HttpClient httpClient;  // HTTP client connection factory.
    String sessionId;  // Session id when connected.
    int sendMessageCount;  // Number of messages sent.
    int messageCount;
    int receiveMessageCount;  // Number of messages received.

    int port;

    void printServerError(HttpClientResponse response) {
      print("Server error ${response.statusCode} ${response.reasonPhrase}");
    }

    void printProtocolError() {
      print("Protocol error");
    }

    Map parseResponse(HttpClientResponse response,
                      String data,
                      String expected) {
      if (response.statusCode != HttpStatus.OK) {
        printServerError(response);
        return null;
      }
      var responseData = json.parse(data);
      if (responseData["response"] != expected ) {
        printProtocolError();
        return null;
      }
      return responseData;
    }

    void leave() {
      void leaveResponseHandler(HttpClientResponse response, String data) {
        httpClient.close();
      }

      Map leaveRequest = new Map();
      leaveRequest["request"] = "leave";
      leaveRequest["sessionId"] = sessionId;
      httpClient.post("127.0.0.1", port, "/leave")
        .then((HttpClientRequest request) {
          request.addString(json.stringify(leaveRequest));
          return request.close();
        })
        .then((HttpClientResponse response) {
          StringBuffer body = new StringBuffer();
          response.listen(
            (data) => body.write(new String.fromCharCodes(data)),
            onDone: () => leaveResponseHandler(response, body.toString()));
        });
    }

    var sendMessage;
    void receive() {
      void receiveResponseHandler(HttpClientResponse response, String data) {
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
      httpClient.post("127.0.0.1", port, "/receive")
        .then((HttpClientRequest request) {
          request.addString(json.stringify(messageRequest));
          return request.close();
        })
        .then((HttpClientResponse response) {
          StringBuffer body = new StringBuffer();
          response.listen(
            (data) => body.write(new String.fromCharCodes(data)),
            onDone: () => receiveResponseHandler(response, body.toString()));
        });
    }

    sendMessage = () {
      void sendResponseHandler(HttpClientResponse response, String data) {
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
      messageRequest["message"] = "message $sendMessageCount";
      httpClient.post("127.0.0.1", port, "/message")
        .then((HttpClientRequest request) {
          request.addString(json.stringify(messageRequest));
          return request.close();
        })
        .then((HttpClientResponse response) {
          StringBuffer body = new StringBuffer();
          response.listen(
            (data) => body.write(new String.fromCharCodes(data)),
            onDone: () => sendResponseHandler(response, body.toString()));
        });
    };

    void join() {
      void joinResponseHandler(HttpClientResponse response, String data) {
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
      httpClient.post("127.0.0.1", port, "/join")
        .then((HttpClientRequest request) {
          request.addString(json.stringify(joinRequest));
          return request.close();
        })
        .then((HttpClientResponse response) {
          StringBuffer body = new StringBuffer();
          response.listen(
            (data) => body.write(new String.fromCharCodes(data)),
            onDone: () => joinResponseHandler(response, body.toString()));
        });
    }

    // Create a HTTP client factory.
    httpClient = new HttpClient();
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
