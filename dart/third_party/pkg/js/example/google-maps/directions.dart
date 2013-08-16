// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// A JS Interop sample accessing the Google Maps API.  The sample is based on
// the directions-panel example here:
// https://google-developers.appspot.com/maps/documentation/javascript/examples/directions-panel

import 'dart:html';
import 'package:js/js.dart' as js;

var maps;
var directionsDisplay;
var directionsService;

void main() {
  // Save the JS google.maps namespace for convenience.  It must be retained
  // as it's used beyond this scope.
  maps = js.retain(js.context.google.maps);

  // Allocate a new JS Map with the following options.  See:
  // https://developers.google.com/maps/documentation/javascript/reference#Map
  var myOptions = js.map({
    'zoom': 9,
    'mapTypeId': maps.MapTypeId.ROADMAP,
    'center': new js.Proxy(maps.LatLng, 47.6097, -122.3331)
  });
  var map = new js.Proxy(maps.Map, query('#map_canvas'), myOptions);

  // Allocate a new JS DirectionsRenderer to display directions on the page.
  // See
  // https://developers.google.com/maps/documentation/javascript/reference#DirectionsRenderer
  directionsDisplay =
      js.retain(new js.Proxy(maps.DirectionsRenderer,
                                js.map({'map': map})));
  directionsDisplay.setPanel(query('#directions_panel'));

  // Allocate a new JS DirectionService to forward requests to the server.
  // See:
  // https://developers.google.com/maps/documentation/javascript/reference#DirectionsService
  directionsService = js.retain(new js.Proxy((maps.DirectionsService)));

  var control = query('#control');
  control.style.display = 'block';
  map.controls[maps.ControlPosition.TOP].push(control);

  // Recalculate the route when the start or end points are changed.
  query('#start').onChange.listen(calcRoute);
  query('#end').onChange.listen(calcRoute);
}

void calcRoute(e) {
  final panel = query('#directions_panel');
  final SelectElement start = query('#start');
  final SelectElement end = query('#end');

  panel.innerHtml = "<b>Thinking...</b>";

  // Submit a new directions request.
  final request = js.map({
    'origin': start.value,
    'destination': end.value,
    'travelMode': maps.DirectionsTravelMode.DRIVING
  });

  // The routing callback is only called once.
  directionsService.route(request, new js.Callback.once((response, status) {
    if (status == maps.DirectionsStatus.OK) {
      document.query('#directions_panel').innerHtml = "";
      directionsDisplay.setDirections(response);
    } else {
      document.query('#directions_panel').innerHtml = "<b>Err, try flying.</b>";
    }
  }));
  print('Live ${js.proxyCount()} proxies out of ${js.proxyCount(all: true)} ever allocated.');
}
