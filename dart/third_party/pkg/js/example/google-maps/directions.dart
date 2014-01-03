// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// A JS Interop sample accessing the Google Maps API.  The sample is based on
// the directions-panel example here:
// https://google-developers.appspot.com/maps/documentation/javascript/examples/directions-panel

import 'dart:html';
import 'package:js/js.dart' as js;

// Save the JS google.maps namespace for convenience.
final maps = js.context.google.maps;

var directionsDisplay;
var directionsService;

void main() {

  // Allocate a new JS Map with the following options.  See:
  // https://developers.google.com/maps/documentation/javascript/reference#Map
  var myOptions = js.map({
    'zoom': 9,
    'mapTypeId': maps.MapTypeId.ROADMAP,
    'center': new js.Proxy(maps.LatLng, 47.6097, -122.3331)
  });

  var map = new js.Proxy(maps.Map, querySelector('#map_canvas'), myOptions);

  // Allocate a new JS DirectionsRenderer to display directions on the page.
  // See
  // https://developers.google.com/maps/documentation/javascript/reference#DirectionsRenderer
  directionsDisplay = new js.Proxy(maps.DirectionsRenderer,
      js.map({'map': map}));
  directionsDisplay.setPanel(querySelector('#directions_panel'));

  // Allocate a new JS DirectionService to forward requests to the server.
  // See:
  // https://developers.google.com/maps/documentation/javascript/reference#DirectionsService
  directionsService = new js.Proxy(maps.DirectionsService);

  var control = querySelector('#control');
  control.style.display = 'block';
  map.controls[maps.ControlPosition.TOP].push(control);

  // Recalculate the route when the start or end points are changed.
  querySelector('#start').onChange.listen(calcRoute);
  querySelector('#end').onChange.listen(calcRoute);
}

void calcRoute(e) {
  final panel = querySelector('#directions_panel');
  final SelectElement start = querySelector('#start');
  final SelectElement end = querySelector('#end');

  panel.innerHtml = "<b>Thinking...</b>";

  // Submit a new directions request.
  final request = js.map({
    'origin': start.value,
    'destination': end.value,
    'travelMode': maps.DirectionsTravelMode.DRIVING
  });

  directionsService.route(request, (response, status) {
    if (status == maps.DirectionsStatus.OK) {
      querySelector('#directions_panel').innerHtml = "";
      directionsDisplay.setDirections(response);
    } else {
      querySelector('#directions_panel').innerHtml = "<b>Err, try flying.</b>";
    }
  });
}
