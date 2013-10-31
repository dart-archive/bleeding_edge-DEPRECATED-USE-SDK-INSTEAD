// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/// Runs a HTTP server on localhost that mimics the behavoir of pub.dartlang.org
/// and serves files on pub requests. Files to be stored are on disk in the file
/// system.
///
/// The port for the server and the base directory of the data to be served
/// should be passed in as arguments

library pub_package_server;

import 'dart:async';
import 'dart:io';

const LOG_REQUESTS = true;

String baseDir;

main(List<String> options) {
  int port;
  if (options.length != 1) {
    print('Insufficient arguments \npub_package_server serverDataLocation');
    exit(64);
  }

  baseDir = options.first;

  HttpServer.bind("localhost", 0).then((server) {
    port = server.port;
    server.listen(requestReceivedHandler);
    print('Server is using port $port');
  });
}

// TODO(keertip): add support for pub commands other than install
void requestReceivedHandler(request) {
  if (LOG_REQUESTS) {
    print("Request: ${request.method} ${request.uri}");
  }

  var response = request.response;
  try {
    var path = request.uri.path.replaceFirst("/", "");
    response.persistentConnection = false;

    var file;
    if(FileSystemEntity.isDirectorySync('$baseDir$path')){
      file = new File('$baseDir$path.txt');
    } else {
      file = new File('$baseDir$path');
    }

    file.readAsBytes().then((data) {
      response.statusCode = 200;
      response.contentLength = data.length;
      response.add(data);
      response.close();
    }).catchError((e, trace) {
      print(trace);
      response.statusCode = 404;
      response.contentLength = 0;
      response.close();
    });
  } catch (e) {
    print(e);
    response.statusCode = 500;
    response.close();
    return;
  }
}

