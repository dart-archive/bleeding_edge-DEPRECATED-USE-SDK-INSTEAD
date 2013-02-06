// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

import 'dart:async';
import 'dart:html';
import 'dart:json' as jsonlib;

/// Issue wraps JSON structure that describes a bug.
class Issue {
  final json;
  Issue(this.json);

  void addTo(Element div) {
    div.children.add(new Element.tag("h2")..text = json[r"title"][r"$t"]);
    div.children.add(new Element.tag("p")..text = json[r"content"][r"$t"]);
  }
}

/// Decodes JSON into a list of Issues.
List<Issue> getIssues(json) {
  var issues = json["feed"]["entry"];
  if (issues == null) return null;
  return issues.map((data) => new Issue(data)).toList();
}

/// Iterates over the received issues and construct HTML for them.
void processJson(String jsonText) {
  var json = jsonlib.parse(jsonText);
  Element div = query("#content");
  List<Issue> list = getIssues(json);
  if (list == null) {
    div.children.add(new Element.tag("h2")..text = "... no issues found.");
  } else {
    list.forEach((Issue i) => i.addTo(div));
  }
}

/// Sends a HTTPRequest and returns a future for the data.
Future<dynamic> requestJson(String url) {
  Completer c = new Completer<dynamic>();
  void callback(HttpRequest req) {
    if (req.readyState == HttpRequest.DONE) {
      c.complete(jsonlib.parse(req.response));
    }
  };
  return HttpRequest.getString(url);
}

void main() {
  // Requests new issues from the Dart issue database as json.
  const String url = "https://code.google.com/feeds/issues/p/dart/issues/full";
  const String options = "alt=json&can=new";
  requestJson("$url?$options").then(processJson);
}
