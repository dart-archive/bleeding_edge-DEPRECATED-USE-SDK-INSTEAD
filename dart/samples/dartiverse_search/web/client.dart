// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:html';
import 'dart:convert';


class Client {
  static const Duration CONNECT_DELAY = const Duration(milliseconds: 500);

  WebSocket webSocket;
  final DivElement log = new DivElement();
  SearchInputElement searchElement = querySelector('#q');
  DivElement statusElement = querySelector('#status');
  DivElement resultsElement = querySelector('#results');

  Client() {
    onDisconnected();
    searchElement.onChange.listen((e) {
      search(searchElement.value);
      searchElement.value = '';
    });
  }

  void onConnected() {
    setStatus('');
    searchElement.disabled = false;
    searchElement.focus();
    webSocket.onMessage.listen((e) {
      onMessage(e.data);
    });
  }

  void onDisconnected() {
    searchElement.disabled = true;
    setStatus('Disconnected');
    webSocket = new WebSocket('ws://${Uri.base.host}:${Uri.base.port}/ws');
    webSocket.onOpen.first.then((_) {
      onConnected();
      webSocket.onClose.first.then((_) {
        print("Connection disconnected to ${webSocket.url}");
        onDisconnected();
      });
    });
    webSocket.onError.first.then((_) {
      print("Failed to connect to ${webSocket.url}");
      onDisconnected();
    });
  }

  void setStatus(String status) {
    statusElement.innerHtml = status;
  }


  void onMessage(data) {
    var json = JSON.decode(data);
    var response = json['response'];
    switch (response) {
      case 'searchResult':
        addResult(json['source'], json['title'], json['link']);
        break;

      case 'searchDone':
        setStatus(resultsElement.children.isEmpty ? "No results found" : "");
        break;

      default:
        print("Invalid response: '$response'");
    }
  }

  void addResult(String source, String title, String link) {
    var result = new DivElement();
    result.children.add(new HeadingElement.h2()..innerHtml = source);
    result.children.add(
        new AnchorElement(href: link)
        ..innerHtml = title
        ..target = '_blank');
    result.classes.add('result');
    resultsElement.children.add(result);
  }

  void search(String input) {
    if (input.isEmpty) return;
    setStatus('Searching...');
    resultsElement.children.clear();
    var request = {
      'request': 'search',
      'input': input
    };
    webSocket.send(JSON.encode(request));
  }
}


void main() {
  var client = new Client();
}
