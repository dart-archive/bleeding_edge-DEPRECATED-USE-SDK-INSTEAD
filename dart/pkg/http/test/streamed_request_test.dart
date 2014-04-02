// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library streamed_request_test;

import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:unittest/unittest.dart';

import 'utils.dart';

void main() {
  group('contentLength', () {
    test('defaults to null', () {
      var request = new http.StreamedRequest('POST', dummyUrl);
      expect(request.contentLength, isNull);
    });

    test('disallows negative values', () {
      var request = new http.StreamedRequest('POST', dummyUrl);
      expect(() => request.contentLength = -1, throwsArgumentError);
    });

    test('controls the Content-Length header', () {
      return startServer().then((_) {
        var request = new http.StreamedRequest('POST', serverUrl);
        request.contentLength = 10;
        request.sink.add([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
        request.sink.close();

        return request.send();
      }).then((response) {
        expect(UTF8.decodeStream(response.stream),
            completion(parse(containsPair('headers',
                containsPair('content-length', ['10'])))));
      }).whenComplete(stopServer);
    });

    test('defaults to sending no Content-Length', () {
      return startServer().then((_) {
        var request = new http.StreamedRequest('POST', serverUrl);
        request.sink.add([1, 2, 3, 4, 5, 6, 7, 8, 9, 10]);
        request.sink.close();

        return request.send();
      }).then((response) {
        expect(UTF8.decodeStream(response.stream),
            completion(parse(containsPair('headers',
                isNot(contains('content-length'))))));
      }).whenComplete(stopServer);
    });

    test('is frozen by finalize()', () {
      var request = new http.StreamedRequest('POST', dummyUrl);
      request.finalize();
      expect(() => request.contentLength = 10, throwsStateError);
    });
  });

  // Regression test.
  test('.send() with a response with no content length', () {
    return startServer().then((_) {
      var request = new http.StreamedRequest(
          'GET', serverUrl.resolve('/no-content-length'));
      request.sink.close();
      return request.send();
    }).then((response) {
      expect(UTF8.decodeStream(response.stream), completion(equals('body')));
    }).whenComplete(stopServer);
  });

}