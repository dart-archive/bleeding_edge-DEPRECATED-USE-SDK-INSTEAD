// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library google_maps;

// This code is derived from
// https://developers.google.com/maps/documentation/javascript/tutorial#HelloWorld
// You can view the original JavaScript example at
// https://developers.google.com/maps/documentation/javascript/examples/map-simple

import 'dart:html' show querySelector;
import 'dart:js' show context, JsObject;

void main() {
  // The top-level getter context provides a JsObject that represents the global
  // object in JavaScript.
  final google_maps = context['google']['maps'];

  // new JsObject() constructs a new JavaScript object and returns a proxy
  // to it.
  var center = new JsObject(google_maps['LatLng'], [-34.397, 150.644]);

  var mapTypeId = google_maps['MapTypeId']['ROADMAP'];

  // new JsObject.jsify() recursively converts a collection of Dart objects
  // to a collection of JavaScript objects and returns a proxy to it.
  var mapOptions = new JsObject.jsify({
      "center": center,
      "zoom": 8,
      "mapTypeId": mapTypeId
  });

  // Nodes are passed though, or transferred, not proxied.
  new JsObject(google_maps['Map'], [querySelector('#map-canvas'), mapOptions]);
}
