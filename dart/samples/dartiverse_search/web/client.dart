// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:async';
import 'dart:convert';
import 'dart:html';


class Client {
  static const Duration RECONNECT_DELAY = const Duration(milliseconds: 500);

  bool connectPending = false;
  String mostRecentSearch = null;
  WebSocket webSocket;
  final DivElement log = new DivElement();
  SearchInputElement searchElement = querySelector('#q');
  DivElement statusElement = querySelector('#status');
  DivElement resultsElement = querySelector('#results');

  Client() {
    searchElement.onChange.listen((e) {
      search(searchElement.value);
      searchElement.value = '';
    });
    connect();
  }

  void connect() {
    connectPending = false;
    webSocket = new WebSocket('ws://${Uri.base.host}:${Uri.base.port}/ws');
    webSocket.onOpen.first.then((_) {
      onConnected();
      webSocket.onClose.first.then((_) {
        print("Connection disconnected to ${webSocket.url}.");
        onDisconnected();
      });
    });
    webSocket.onError.first.then((_) {
      print("Failed to connect to ${webSocket.url}. "
            "Run bin/server.dart and try again.");
      onDisconnected();
    });
  }

  void onConnected() {
    setStatus('');
    searchElement.disabled = false;
    searchElement.focus();
    webSocket.onMessage.listen((e) {
      handleMessage(e.data);
    });
  }

  void onDisconnected() {
    if (connectPending) return;
    connectPending = true;
    setStatus('Disconnected. Start \'bin/server.dart\' to continue.');
    searchElement.disabled = true;
    new Timer(RECONNECT_DELAY, connect);
  }

  void setStatus(String status) {
    statusElement.innerHtml = status;
  }


  void handleMessage(data) {
    var json = JSON.decode(data);
    var response = json['response'];
    switch (response) {
      case 'searchResult':
        addResult(json['source'], json['title'], json['link']);
        break;

      case 'searchDone':
        setStatus(resultsElement.children.isEmpty
             ? "$mostRecentSearch: No results found"
             : "$mostRecentSearch: "
               "${resultsElement.children.length} results found");
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
    setStatus('Searching for $input...');
    resultsElement.children.clear();
    var request = {
      'request': 'search',
      'input': input
    };
    webSocket.send(JSON.encode(request));
    mostRecentSearch = input;
  }
}


void main() {
  var client = new Client();
}
