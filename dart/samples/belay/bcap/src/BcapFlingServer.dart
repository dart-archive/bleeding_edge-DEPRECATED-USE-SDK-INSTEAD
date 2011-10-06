// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class BcapFlingUtil {
  
  static Map bcapRequest(HttpRequest request) {
    Map topLevel = JSON.parse(request.body);
    return topLevel['value'];
  }

  static void xhrAccessHeader(HttpResponse response) {
    response.setHeader('Access-Control-Allow-Origin', '*');
  }
  
  static void xhrContent(HttpResponse response, String content, String contentType) {
    xhrAccessHeader(response);
    response.setStatusCode(200);
    response.setHeader('Cache-Control', 'no-cache');
    response.setHeader('Content-Type', contentType);
    response.setHeader('Expires', 'Fri, 01 Jan 1990 00:00:00 GMT');
    response.write(content);
  }
  
  static void bcapResponse(HttpResponse response, var msg) {
    xhrContent(response, JSON.stringify({ 'value': msg }), "text/plain;charset=UTF-8");
    response.finish();
  }

  static void bcapNullResponse(HttpResponse response) {
    xhrAccessHeader(response);
    response.setStatusCode(200);
    response.finish();
  }

  static void errorResponse(HttpResponse response, int code, [String msg = '']) {
    response.setStatusCode(code);
    if (msg.length > 0) {
      response.setHeader('Content-Type', "text/plain;charset=UTF-8");
      response.write(msg);
    }
    response.finish();
  }
  
  static void errorMethodNotAllowed(HttpResponse response, [String method = '']) {
    String message = "Method Not Allowed";
    if (method.length > 0) {
      message = "Method $method Not Allowed";
    }
    errorResponse(response, 405, message);
  }
}

class BcapFlingHandler {

  HttpRequest request;
  HttpResponse response;
  
  BcapFlingHandler() { }

  HttpRequestHandler handle(HttpRequest request, HttpResponse response) {
    this.request = request;
    this.response = response;
    
    try {
      switch (request.method) {
        case 'GET': get_(); break;
        case 'PUT': put(); break;
        case 'POST': post(); break;
        case 'DELETE': delete(); break;
        default: errorMethodNotAllowed();
      }
    } catch (var e) {
      response.setStatusCode(500);
    }
  }
  
  Map bcapRequest() {
    return BcapFlingUtil.bcapRequest(request);
  }

  void bcapResponse(var msg) {
    BcapFlingUtil.bcapResponse(response, msg);
  }

  void bcapNullResponse() {
    BcapFlingUtil.bcapNullResponse(response);
  }

  void errorResponse(int code, [String msg = '']) {
    BcapFlingUtil.errorResponse(response, code, msg);
  }
  void errorMethodNotAllowed() {
    BcapFlingUtil.errorMethodNotAllowed(response, request.method);
  }

  void get_() {
    errorMethodNotAllowed();
  }

  void put() {
    errorMethodNotAllowed();
  }

  void delete() {
    errorMethodNotAllowed();
  }
  
  void post() {
    errorMethodNotAllowed();
  }
}

class BcapFlingServer {

  final String baseUrl;
  final HttpServer webServer;
  final BcapLogger logger;
  final Map<String, BcapFlingHandler> pathMap;

  BcapFlingServer(this.baseUrl, this.webServer)
        : logger = new BcapLogger("BcapFlingServer"),
        pathMap = new Map<String, BcapFlingHandler>() {
    webServer.handle("/cap/", void _(HttpRequest req, HttpResponse resp) {
      logger.info("BcapFlingServer: handling ${req.requestedPath} ${req.method}");
      BcapFlingHandler handler = pathMap[req.requestedPath];
      if (handler != null) {
        handler.handle(req, resp);
      } else {
        BcapFlingUtil.errorResponse(resp, 404, "Not Found");
      }
    });
  }
  
  String serverUrl(String url) => baseUrl + url;

  Map grant(BcapFlingHandler handler) {
    var uuid = BcapUtil.uuidv4();
    String path = "/cap/$uuid";
    
    pathMap[path] = handler;
    
    logger.info("BcapFlingServer.grant: $path");
    return { '@': baseUrl + path };
  }

}


