// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('BuzzerServer');
#import('../../../client/base/base.dart');
#import('../../../client/fling/fling.dart');
#import('../bcap/bcap.dart');
#import('../bcap/bcap_fling.dart');

//
// Data model
//

class Buzz {
  DateTime time;
  String body;
  String via;
  Buzz(this.time, this.body, this.via) { }

  Map toJSON() {
    return { 'time': time.toString(), 'body': body, 'via': via };
  }
}

class Feed {

  String title;
  String name;
  String location;
  List<Buzz> posts;
  String clientSnapshot;

  Feed(this.title) {
    posts = [];
  }
}

BcapLogger logger = new BcapLogger("Buzzer");

class PostHandler extends BcapFlingHandler {

  BcapFlingServer capServer;
  Feed feed;

  PostHandler(this.capServer, this.feed) : super() { }

  void post() {
    var request = bcapRequest();
    Buzz buzz = new Buzz(new DateTime.now(), request["body"],
                         request.containsKey("via") ? request["via"] : null);
    feed.posts.addLast(buzz);
    logger.info("New buzz: " + request["body"]);
    bcapResponse(true);
  }

}

class ReadHandler extends BcapFlingHandler {

  BcapFlingServer capServer;
  List<Buzz> posts;

  ReadHandler(this.capServer, this.posts) : super() { }

  void get_() {
    var resp = [];
    for (Buzz buzz in posts) {
      resp.addLast(buzz.toJSON());
    }
    bcapResponse(resp);
  }

}

class SnapshotHandler extends BcapFlingHandler {
  
  BcapFlingServer capServer;
  Feed feed;

  SnapshotHandler(this.capServer, this.feed) : super() { }

  void post() {
    var data = bcapRequest();
    feed.clientSnapshot = data;
    bcapResponse(true);
  }

}

class LaunchHandler extends BcapFlingHandler {

  BcapFlingServer capServer;
  Feed feed;

  LaunchHandler(this.capServer, this.feed) : super() { }
  
  void get_() {
    logger.info("GET /belay/launch");

    bcapResponse({
      'page': {
           'html': capServer.serverUrl('/buzzer.html'),
           'window': { 'width': 300, 'height': 400 }
         },
         'info': {
           'title': feed.title,
           'postCap': capServer.grant(new PostHandler(capServer, feed)),
           'readCap': capServer.grant(new ReadHandler(capServer, feed.posts)),
           'snapshot': feed.clientSnapshot,
           'snapshotCap':
            capServer.grant(new SnapshotHandler(capServer, feed)),
           'readChitURL': capServer.serverUrl("/chit-24.png"),
           'postChitURL': capServer.serverUrl("/chit-25.png")
         }
    });
  }

}

class BuzzerServer {
  BcapFlingServer capServer;
  HttpServer server;

  Map<String, Feed> database;

  void generateProfile(HttpRequest req, HttpResponse resp) {
    if (req.method !== 'POST') {
      BcapFlingUtil.errorMethodNotAllowed(resp, req.method);
      return;
    }

    String feedId = BcapUtil.uuidv4();
  
    Feed feed = new Feed(BcapFlingUtil.bcapRequest(req)["title"]);
    database[feedId] = feed;
    
    var launchCap = capServer.grant(new LaunchHandler(capServer, feed));

    BcapFlingUtil.bcapResponse(resp, {
      'launch': launchCap,
      'icon': capServer.serverUrl("/tool-buzzer.png"),
      'name': feed.title
    });
  }

  BuzzerServer() {

    server = new HttpServer();
    database = new Map<String, Feed>();
    capServer = new BcapFlingServer("http://localhost:9014", server);

    server.handle("/belay/generateProfile",
      void _(req, resp) { generateProfile(req, resp); });
    
    server.handle("/", ClientApp.create("static"));

    server.listen(9014);
    logger.info("Started server at http://localhost:9014/");
  }
}

void main() {
  BuzzerServer buzzer = new BuzzerServer();
  Fling.goForth();
}

