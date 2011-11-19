// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import('../lib/node/node.dart');

void main() {
  http.createServer((ServerRequest req, ServerResponse res) {
    //print("Request for ${req.url} received.");

    var filename;
    if (req.url == '/') {
      res.setHeader('Content-Type', 'text/html');
      filename = './tip/tip.html';
    } else if (req.url == '/favicon.ico') {
      res.setHeader('Content-Type', 'image/gif');
      filename = './tip/dart_16_16.gif';
    } else {
      if (req.url.endsWith('.html')) {
        res.setHeader('Content-Type', 'text/html');
      } else if (req.url.endsWith('.css')) {
          res.setHeader('Content-Type', 'text/css');
      } else {
        res.setHeader('Content-Type', 'text/plain');
      }
      filename = '../' + req.url;
    }

    if (path.existsSync(filename)) {
      var stat = fs.statSync(filename);
      if (stat.isFile()) {
        res.statusCode = 200;
        res.end(fs.readFileSync(filename, 'utf8'));
        return;
      }
      // TODO(jimhug): Directory listings?
    }


    res.statusCode = 404;
    res.end('');
  }).listen(1337, "localhost");
  print('Server running at http://localhost:1337/');
}
