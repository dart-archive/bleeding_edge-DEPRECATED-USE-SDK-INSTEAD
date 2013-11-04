// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of dartiverse_search;


class GithubSearchEngine {
  String get name => 'Github';

  Stream<SearchResult> search(String input) {
    var query = {
      'q': 'language:dart $input'
    };
    var searchUri = new Uri.https(
        'api.github.com',
        '/search/repositories',
        query);
    var controller = new StreamController();
    http_client.get(searchUri)
      .then((response) {
        if (response.statusCode != HttpStatus.OK) throw "Bad status code.";
        var json = JSON.decode(response.body);
        json.putIfAbsent('items', () => []);
        json['items']
          .expand((item) {
            if (item['description'] == null ||
                item['description'].isEmpty) {
              // Skip results without a description.
              return [];
            }
            return [item];
          })
          .take(3)
          .forEach((item) {
            controller.add(new SearchResult(
                item['description'], item['html_url']));
          });
      })
      .catchError(controller.addError)
      .whenComplete(controller.close);
    return controller.stream;
  }
}
