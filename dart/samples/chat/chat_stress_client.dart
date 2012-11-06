// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library chat_stress_client;
import "dart:io";
import "dart:json";


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
      var responseData = JSON.parse(data);
      if (responseData["response"] != expected ) {
        printProtocolError();
        return null;
      }
      return responseData;
    }

    void leave() {
      void leaveResponseHandler(HttpClientResponse response, String data) {
        httpClient.shutdown();
      }

      Map leaveRequest = new Map();
      leaveRequest["request"] = "leave";
      leaveRequest["sessionId"] = sessionId;
      HttpClientConnection conn = httpClient.post("127.0.0.1", port, "/leave");
      conn.onRequest = (HttpClientRequest request) {
        request.outputStream.writeString(JSON.stringify(leaveRequest));
        request.outputStream.close();
      };
      conn.onResponse = (HttpClientResponse response) {
        StringInputStream stream = new StringInputStream(response.inputStream);
        StringBuffer body = new StringBuffer();
        stream.onData = () => body.add(stream.read());
        stream.onClosed = () {
          leaveResponseHandler(response, body.toString());
        };
      };
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
      HttpClientConnection conn =
          httpClient.post("127.0.0.1", port, "/receive");
      conn.onRequest = (HttpClientRequest request) {
        request.outputStream.writeString(JSON.stringify(messageRequest));
        request.outputStream.close();
      };
      conn.onResponse = (HttpClientResponse response) {
        StringInputStream stream = new StringInputStream(response.inputStream);
        StringBuffer body = new StringBuffer();
        stream.onData = () => body.add(stream.read());
        stream.onClosed = () {
          receiveResponseHandler(response, body.toString());
        };
      };
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
      HttpClientConnection conn =
      httpClient.post("127.0.0.1", port, "/message");
      conn.onRequest = (HttpClientRequest request) {
        request.outputStream.writeString(JSON.stringify(messageRequest));
        request.outputStream.close();
      };
      conn.onResponse = (HttpClientResponse response) {
        StringInputStream stream = new StringInputStream(response.inputStream);
        StringBuffer body = new StringBuffer();
        stream.onData = () => body.add(stream.read());
        stream.onClosed = () {
          sendResponseHandler(response, body.toString());
        };
      };
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
      HttpClientConnection conn = httpClient.post("127.0.0.1", port, "/join");
      conn.onRequest = (HttpClientRequest request) {
        request.outputStream.writeString(JSON.stringify(joinRequest));
        request.outputStream.close();
      };
      conn.onResponse = (HttpClientResponse response) {
        StringInputStream stream = new StringInputStream(response.inputStream);
        StringBuffer body = new StringBuffer();
        stream.onData = () => body.add(stream.read());
        stream.onClosed = () {
          joinResponseHandler(response, body.toString());
        };
      };
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
