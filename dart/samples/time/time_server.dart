// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("time_server");

#import("../chat/http.dart");

final HOST = "127.0.0.1";
final PORT = 8080;

final LOG_REQUESTS = true;

void main() {
  HTTPServer server = new HTTPServer();
  
  server.listen(HOST, PORT, (HTTPRequest req, HTTPResponse rsp) {
    requestReceivedHandler(req, rsp);
  });
  
  print("Serving the current time on http://${HOST}:${PORT}."); 
}

void requestReceivedHandler(HTTPRequest request, HTTPResponse response) {
  if (LOG_REQUESTS) {
    print("Request: ${request.method} ${request.uri}");
  }

  String htmlResponse = createHtmlResponse();
  
  response.setHeader("Content-Type", "text/html; charset=UTF-8");
  response.writeString(htmlResponse, null);
  response.writeDone();
}

String createHtmlResponse() {
  return 
'''
<html>
  <style>
    body { background-color: teal; }
    p { background-color: white; border-radius: 8px; border:solid 1px #555; text-align: center; padding: 0.5em; 
        font-family: "Lucida Grande", Tahoma; font-size: 18px; color: #555; }
  </style>
  <body>
    <br/><br/>
    <p>Current time: ${new Date.now()}</p>
  </body>
</html>
''';
}
