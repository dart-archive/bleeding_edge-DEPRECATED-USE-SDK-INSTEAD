// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import('dart:html');
#import('dart:json');

/**
 *  Issue wraps JSON structure that describes a bug.
 */
class Issue {
  final json;
  String id, state, title, content;
  Issue(this.json) {
    id = json[@"issues$id"][@"$t"];
    state = json[@"issues$state"][@"$t"];
    title = json[@"title"][@"$t"];
    content = json[@"content"][@"$t"];
  }
  String toHTML() => "<h2>$title (id=$id $state)</h2><pre>$content</pre>";
}

/**
 * Decodes JSON into a list of Issues.
 */
List<Issue> getIssues(json) {
  return json["feed"]["entry"].map((data) => new Issue(data));
}

/**
 * Iterates over the recieved issues and construct HTML for them.
 */
void processJson(json) {
  StringBuffer buffer = new StringBuffer();
  getIssues(json).forEach((Issue issue) => buffer.add(issue.toHTML()));
  query("#container").innerHTML = buffer.toString();
}

/**
 * Sends a XMLHTTPRequest and returns a future fo the date.
 */
Future<Dynamic> requestJson(String url) {
  Completer c = new Completer<Dynamic>();
  void callback(XMLHttpRequest xhr) {
    if (xhr.readyState == XMLHttpRequest.DONE) {
      c.complete(JSON.parse(xhr.response));
    }
  };
  new XMLHttpRequest.get(url, callback);
  return c.future;
}

void main() {
  // Requests new issues (can=new) from the Dart issue database.
  final String url =
    "https://code.google.com/feeds/issues/p/dart/issues/full?alt=json&can=new";
  requestJson(url).then(processJson);
}
