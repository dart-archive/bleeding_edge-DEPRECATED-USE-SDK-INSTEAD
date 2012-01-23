#library('echoServer');

#import('../../lib/node/node.dart');
#import('../../lib/node/net.dart');

void main() {
  // Note: the callback function must use the type "Socket" for the socket argument,
  // otherwise frog will strip out the Socket.write method.
  var server = net.createServer((Socket socket) {
    socket.write("Echo server\r\n");
    socket.pipe(socket);
  });

  server.listen(1337, "127.0.0.1");
  print('Listening on port 1337');
}