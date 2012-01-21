#library('httpServer');

#import('../../lib/node/node.dart');
#import('../../lib/node/http.dart');

void main() {
  // Note: the callback function must use the actual types for the arguments,
  // otherwise frog will strip out the type's methods.
  http.createServer((ServerRequest req, ServerResponse res) {
    res.writeHead(200, '', {'Content-Type': 'text/plain'});
    res.end('Hello World\n');
  }).listen(1337, "127.0.0.1");
  console.log('Server running at http://127.0.0.1:1337/');
}