// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library time_server;

import "dart:io";

const HOST = "127.0.0.1";
const PORT = 8080;

const LOG_REQUESTS = true;

void main() {
  HttpServer.bind(HOST, PORT).then((HttpServer server) {
    server.listen(requestReceivedHandler);
  });

  print("Serving the current time on http://${HOST}:${PORT}.");
}

void requestReceivedHandler(HttpRequest request) {
  if (LOG_REQUESTS) {
    print("Request: ${request.method} ${request.uri}");
  }

  String text = createHtmlResponse();

  request.response.headers.set(
      HttpHeaders.CONTENT_TYPE, "text/html; charset=UTF-8");
  request.response.write(text);
  request.response.close().catchError(print);
}

String createHtmlResponse() {
  return
'''
<html>
  <style>
    body { background-color: teal; }
    p { background-color: white; border-radius: 8px;
        border:solid 1px #555; text-align: center; padding: 0.5em;
        font-family: "Lucida Grande", Tahoma; font-size: 18px; color: #555; }
  </style>
  <body>
    <br/><br/>
    <p>Current time: ${new DateTime.now()}</p>
  </body>
</html>
''';
}
