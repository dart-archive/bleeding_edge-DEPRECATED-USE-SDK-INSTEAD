// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Responds to commands over a socket
 */
void main() {
  List<String> argv = new Options().arguments;
  String host = argv.length > 0 ? argv[0] : "127.0.0.1";
  int    port = argv.length > 1 ? Math.parseInt(argv[1]) : 1236;
  new FrogServer().start(host, port);
}

class FrogServer {
  ServerSocket serverSocket;
  
  void start(host, port) {
    serverSocket = new ServerSocket(host, port, 5);
    if (serverSocket == null) {
      throw "can't get server socket on ${host}:${serverSocket.port}";
    }
    serverSocket.connectionHandler = (Socket connection) {
      new FrogConnection(connection).setCloseHandler(() {
        serverSocket.close();
        print(">>> Socket closed <<<");
      });
    };
    print("accepting connections on http://${host}:${serverSocket.port}");
  }
}

class FrogConnection {
  Socket socket;
  InputStream inputStream;
  OutputStream outputStream;

  FrogConnection(this.socket) {
    inputStream = socket.inputStream;
    outputStream = socket.outputStream;
    inputStream.dataHandler = processRequests;
  }
  
  void setCloseHandler(void callback()) {
    inputStream.closeHandler = callback;
  }
  
  /** Process input until the stream is closed */
  void processRequests() {
    while (true) {
      String message = readMessage();
      if (message == null) break;
      print(message);
      Map<String, Object> request = JSON.parse(message);
      sendMessage('{"id":' + request["id"] + ',"status":"complete"}');
    }
  }
  
  /** Return next message or [null] if none */
  String readMessage() {
    List<int> buf = readBytes(4);
    if (buf == null) return null;
    int messageLen = (buf[0] << 24) + (buf[1] << 16) + (buf[2] << 8) + buf[3];
    print("messageLen = " + messageLen);
    buf = readBytes(messageLen);
    if (buf == null) return null;
    // Only returns ASCII string
    return new String.fromCharCodes(buf);
  }
  
  /** Return a buffer with the specified number of bytes or null if none */
  List<int> readBytes(int numBytes) {
    int start = 0;
    List<int> buf = new List<int>(numBytes);
    while (start < numBytes) {
      int count = inputStream.readInto(buf, start, numBytes - start);
      if (count == 0) return null;
      start += count;
    }
    return buf;
  }
  
  /** Send the specified message back to the requestor */
  void sendMessage(String message) {
    List<int> charCodes = message.charCodes();
    int len = charCodes.length;
    List<int> buf = new List<int>(4);
    buf[0] = len >> 24;
    buf[1] = len >> 16;
    buf[2] = len >> 8;
    buf[3] = len;
    outputStream.write(buf);
    // Only writes lowest 8 bits... ASCII
    outputStream.write(charCodes);
  }
}

class JSON {
  static Object parse(String jsonString) {
    return new JsonParser(jsonString).parse();
  }
}

// Assumes no whitespace
class JsonParser {
  static final int COLON = 58;  // ':'.charCodeAt(0)
  static final int COMMA = 44;  // ','.charCodeAt(0)
  static final int QUOTE = 34;  // '"'.charCodeAt(0)
  static final int LBRACE = 123;  // '{'.charCodeAt(0)
  static final int RBRACE = 125;  // '}'.charCodeAt(0)
  static final int LBRACKET = 91;  // '['.charCodeAt(0)
  static final int RBRACKET = 93;  // ']'.charCodeAt(0)
  static final int ZERO = 48;  // '0'.charCodeAt(0)
  static final int NINE = 57;  // '9'.charCodeAt(0)
  
  String jsonString;
  int index = 0;
  
  JsonParser(this.jsonString);

  Object parse() {
    int ch = jsonString.charCodeAt(index++);
    if (ch == QUOTE) return parseString();
    if (ch == LBRACE) return parseMap();
    index--;
    return parseInt();
  }
  
  Map<String, Object> parseMap() {
    Map<String, Object> result = new Map();
    while (jsonString.charCodeAt(index) != RBRACE) {
      if (jsonString.charCodeAt(index) != QUOTE) throwUnexpectedChar();
      index++;
      String key = parseString();
      if (jsonString.charCodeAt(index) != COLON) throwUnexpectedChar();
      index++;
      Object value = parse();
      result[key] = value;
      if (jsonString.charCodeAt(index) == COMMA) index++;
    }
    return result;
  }
  
  String parseString() {
    int start = index;
    while (jsonString.charCodeAt(index) != QUOTE) {
      index++;
    }
    String result = jsonString.substring(start, index);
    index++;
    return result;
  }
  
  int parseInt() {
    int start = index;
    int result = 0;
    while (true) {
      int code = jsonString.charCodeAt(index);
      if ((code < ZERO) || (code > NINE)) {
        if (index == start) throwUnexpectedChar();
        return result;
      }
      result = result * 10 + code - ZERO;
      index++;
    }
  }
  
  void throwUnexpectedChar() {
    throw "unexpected JSON char " + jsonString[index] + " at " + index + " in " + jsonString;
  }
}
