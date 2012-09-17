// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import('dart:html');
#import('maps.dart');

DirectionsRenderer directionsDisplay;
DirectionsService directionsService;

calcRoute(_) {
  final panel = query('#directions_panel');
  SelectElement start = query('#start');
  SelectElement end = query('#end');
  final request = {
    'origin': start.value,
    'destination': end.value,
    'travelMode': DirectionsTravelMode.DRIVING
  };
  panel.innerHTML = "<b>Thinking...</b>";
  directionsService.route(request, (response, status) {
      if (status == DirectionsStatus.OK) {
        document.query('#directions_panel').innerHTML = "";
        directionsDisplay.setDirections(response);
      } else {
        document.query('#directions_panel').innerHTML =
            "<b>Err, try flying.</b>";
      }
    });
}

main() {
  initialize();

  var myOptions = {
    'zoom': 9,
    'mapTypeId': MapTypeId.ROADMAP,
    'center': new LatLng(47.6097, -122.3331)
  };
  var map = new GMap('#map_canvas', myOptions);

  directionsDisplay = new DirectionsRenderer(map);
  directionsDisplay.setPanel('#directions_panel');
  directionsService = new DirectionsService();

  var control = document.query('#control');
  control.style.display = 'block';

  (map.controls[ControlPosition.TOP] as Dynamic).push('#control');

  query('#start').on.change.add(calcRoute);
  query('#end').on.change.add(calcRoute);
}
