// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Shows how to use Socket, ServerSocket and InputStream.
 *
 * (This deliberately sends data slowly, to show how InputStream can be
 * read from asynchronously.)
 */
class SocketExample {

  int bytesSent = 0;
  int bytesReceived = 0;
  final int bytesTotal = 8;
  final String host = "127.0.0.1";
  ServerSocket serverSocket;
  Socket sendSocket;
  Socket receiveSocket;
  InputStream inputStream;
  List receiveBuffer;

  SocketExample() {
    // fixed size buffer we use to read from the InputStream
    receiveBuffer = new List(4);
  }

  void go() {
    // initialize the server
    serverSocket = new ServerSocket(host, 0, 5);
    if (serverSocket == null) {
      throw "can't get server socket";
    }
    serverSocket.connectionHandler = onConnect;
    print("accepting connections on ${host}:${serverSocket.port}");

    // initialize the sender
    sendSocket = new Socket(host, serverSocket.port);
    if (sendSocket == null) {
      throw "can't get send socket";
    }

    // send first 4 bytes immediately
    for (int i = 0; i < 4; i++) {
      sendByte();
    }

    // send next 4 bytes slowly
    new Timer.repeating((Timer t) {
      sendByte();
      if (bytesSent == bytesTotal) {
        sendSocket.close();
        t.cancel();
        print("finished sending");
      }
    }, 500);
  }

  void onConnect(Socket connection) {
    receiveSocket = connection;
    inputStream = receiveSocket.inputStream;
    inputStream.dataHandler = receiveBytes;
  }

  void sendByte() {
    sendSocket.writeList(const [65], 0, 1);
    bytesSent++;
    print("sending byte " + bytesSent.toString());
  }

  void receiveBytes() {
    int numBytes = inputStream.readInto(receiveBuffer, 0, 4);
    if (numBytes == 0) {
      return;
    }

    bytesReceived += numBytes;
    print("received ${numBytes} bytes (${bytesReceived} bytes total)");
    if (bytesReceived >= bytesTotal) {
      receiveSocket.close();
      serverSocket.close();
      print("done");
    }
  }
}

void main() => new SocketExample().go();
