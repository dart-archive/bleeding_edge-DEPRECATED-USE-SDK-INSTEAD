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

  static void main() {
    new SocketExample().go();
  }

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
    serverSocket.setConnectionHandler(onConnect);
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
    new Timer((Timer t) {
      sendByte();
      if (bytesSent == bytesTotal) {
        sendSocket.close();
        t.cancel();
        print("finished sending");
      }
    }, 500, true );
  }

  void onConnect() {
    receiveSocket = serverSocket.accept();
    inputStream = receiveSocket.inputStream;
    receiveBytes();
  }

  void sendByte() {
    sendSocket.writeList(const [65], 0, 1);
    bytesSent++;
    print("sending byte " + bytesSent.toString());
  }

  void receiveBytes() {
    bool gotData = inputStream.read(receiveBuffer, 0, 4, bufferReady);
    if (gotData) {
      bufferReady();
    }
  }

  // receive buffer has been filled
  void bufferReady() {
    bytesReceived += receiveBuffer.length;
    print("received ${receiveBuffer.length} bytes (${bytesReceived} bytes total)");
    if (bytesReceived < bytesTotal) {
      receiveBytes();
    } else {
      receiveSocket.close();
      serverSocket.close();
      print("done");
    }
  }
}
