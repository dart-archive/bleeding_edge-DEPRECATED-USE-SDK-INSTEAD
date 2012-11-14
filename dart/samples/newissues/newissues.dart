// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:html';
import 'dart:json';

/// Issue wraps JSON structure that describes a bug.
class Issue {
  final json;
  Issue(this.json);

  void addTo(Element div) {
    div.elements.add(new Element.tag("h2")..text = json[r"title"][r"$t"]);
    div.elements.add(new Element.tag("pre")..text = json[r"content"][r"$t"]);
  }
}

/// Decodes JSON into a list of Issues.
List<Issue> getIssues(json) {
  var issues = json["feed"]["entry"];
  if (issues == null) return null; 
  return issues.map((data) => new Issue(data));
}

/// Iterates over the recieved issues and construct HTML for them.
void processJson(json) {
  Element div = query("#content");
  List<Issue> list = getIssues(json);
  if (list == null) {
    div.elements.add(new Element.tag("h2")..text = "... no issues found.");
  } else {
    getIssues(json).forEach((Issue i) => i.addTo(div));
  }
}

/// Sends a HTTPRequest and returns a future fo the date.
Future<dynamic> requestJson(String url) {
  Completer c = new Completer<dynamic>();
  void callback(HttpRequest req) {
    if (req.readyState == HttpRequest.DONE) {
      c.complete(JSON.parse(req.response));
    }
  };
  new HttpRequest.get(url, callback);
  return c.future;
}

void main() {
  // Requests new issues from the Dart issue database as json.
  const String url = "https://code.google.com/feeds/issues/p/dart/issues/full";
  const String options = "alt=json&can=new";
  requestJson("$url?$options").then(processJson);
}
