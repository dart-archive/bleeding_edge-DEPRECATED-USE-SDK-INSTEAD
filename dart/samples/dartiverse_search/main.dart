// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library dartiverse_search;

import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:http_server/http_server.dart' as http_server;
import 'package:http/http.dart' as http_client;
import 'package:route/server.dart';
import 'package:logging/logging.dart' show Logger, Level;

part 'search_engine.dart';
part 'stack_overflow_search_engine.dart';
part 'github_search_engine.dart';


final Logger log = new Logger('DartiverseSearch');


// List of search-engines used.
final searchEngines = [
  new StackOverflowSearchEngine(),
  new GithubSearchEngine()
];


/**
 * Handle an established [WebSocket] connection.
 *
 * The web-socket can send search requests as JSON-formatted messages,
 * that will be responded to with a series of results and finally a done
 * message.
 */
void handleWebSocket(WebSocket webSocket) {
  log.info('New web-socket connection');

  // Listen for incoming data. We expect the data to be a JSON-encoded String.
  webSocket
    .map((string) => JSON.decode(string))
    .listen((json) {
      // The JSON object should contains a 'request' entry.
      var request = json['request'];
      switch (request) {
        case 'search':
          // Initiate a new search.
          var input = json['input'];
          log.info("Searching for '$input'");
          int done = 0;
          for (var engine in searchEngines) {
            engine.search(input)
              .listen((result) {
                // The search-engine found a result. Sent it to the client.
                log.info("Got result from ${engine.name} for '$input': "
                         "${result.title}");
                var response = {
                  'response': 'searchResult',
                  'source': engine.name,
                  'title': result.title,
                  'link': result.link
                };
                webSocket.add(JSON.encode(response));
              }, onError: (error) {
                log.warning("Error while searching on ${engine.name}: $error");
              },onDone: () {
                done++;
                if (done == searchEngines.length) {
                  // All search-engines are done. Send done message to the
                  // client.
                  webSocket.add(JSON.encode({ 'response': 'searchDone' }));
                }
              });
          }
          break;

        default:
          log.warning("Invalid request '$request'.");
      }
    }, onError: (error) {
      log.warning('Bad WebSocket request');
    });
}


void main() {
  // Set up logger.
  Logger.root.level = Level.ALL;
  Logger.root.onRecord.listen((LogRecord rec) {
    print('${rec.level.name}: ${rec.time}: ${rec.message}');
  });

  var buildPath = Platform.script.resolve('build').path;
  if (!new Directory(buildPath).existsSync()) {
    log.severe("The 'build/' directory was not found. Please run 'pub build'.");
    return;
  }
  HttpServer.bind('0.0.0.0', 8080).then((server) {
    log.info("Search server is running on "
             "'http://${Platform.localHostname}:8080/'");
    var router = new Router(server);

    // The client will connect using a WebSocket. Upgrade requests to '/ws' and
    // forward them to 'handleWebSocket'.
    router.serve('/ws')
      .transform(new WebSocketTransformer())
      .listen(handleWebSocket);

    // Set up default handler. This will serve files from our 'build' directory.
    var virDir = new http_server.VirtualDirectory(buildPath);
    // Disable jail-root, as packages are local sym-links.
    virDir.jailRoot = false;
    virDir.allowDirectoryListing = true;
    virDir.directoryHandler = (dir, request) {
      // Redirect directory-requests to index.html files.
      var indexUri = new Uri.file(dir.path).resolve('index.html');
      virDir.serveFile(new File(indexUri.path), request);
    };
    virDir.serve(router.defaultStream);
  });
}
