// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// A very incomplete Dart layer over the Google Maps JS API, v3.
// See https://developers.google.com/maps/documentation/javascript/reference
// for the underlying JS API.

#library('maps');
#import('dart:html');
#import('js.dart', prefix: 'js');

void initialize() {
  js.register('google.maps.LatLng',
              {
                'lat': 'function (obj) { return obj.lat(); }',
                'lng': 'function (obj) { return obj.lng(); }'
              },
              (obj) => new LatLng(obj['lat'], obj['lng']));
  js.register('google.maps.LatLngBounds',
              {
                'sw': 'function (obj) { return obj.getSouthWest(); }',
                'ne': 'function (obj) { return obj.getNorthEast(); }'
              },
              (obj) => new LatLngBounds(obj['sw'], obj['ne']));
}

class GMap extends js.Serializable {
  String _selector;

  GMap(this._selector, options) {
    js.invoke('''
function (selector, options) {
  var element = document.querySelector(selector);
  var map = new google.maps.Map(element, options);
  element._dart_map = map;
}''', [_selector, options]);
  }

  List<MVCArray> get controls => new MVCMapControlArrayList(this);

  serialize() =>
      encode('function (selector) { return document.querySelector(selector)._dart_map; }',
             [_selector]);
}

class LatLng extends js.Serializable {
  final num _lat;
  final num _lng;

  LatLng(this._lat, this._lng);

  serialize() =>
      encode('function (lat, lng) { return new google.maps.LatLng(lat, lng); }',
             [_lat, _lng]);
}

class LatLngBounds extends js.Serializable {
  final LatLng _sw;
  final LatLng _ne;

  LatLngBounds(this._sw, this._ne);

  serialize() =>
      encode('function (sw, ne) { return new google.maps.LatLngBounds(sw, ne); }',
                            [_sw, _ne]);
}

class MapTypeId {
  static const ROADMAP = 'roadmap';
}

class DirectionsService {
  DirectionsService() {
    js.eval('window._dart_map_directions_service = new google.maps.DirectionsService();');
  }

  void route(Map request, void handler(DirectionsResult result, var status)) {
    js.invoke('''
function (request, callback) {
  _dart_map_directions_service.route(request, callback);
}
''', [request, js.callback(handler)]);
  }
}

class ControlPosition {
  static const TOP = 2;
}

class DirectionsResult {
}

class DirectionsTravelMode {
  static const DRIVING = 'DRIVING';
  static const BICYCLING = 'BICYCLING';
  static const WALKING = 'WALKING';
}

class DirectionsStatus {
  static const OK = 'OK';
}

class DirectionsRenderer extends js.Serializable {
  GMap map;

  DirectionsRenderer(GMap this.map) {
    js.invoke('''
function (map) {
  map._dart_map_directions_renderer = new google.maps.DirectionsRenderer();
  map._dart_map_directions_renderer.setMap(map);
}
''', [map]);
  }

  void setDirections(var directions) {
    js.invoke('''
function (map, directions) {
  map._dart_map_directions_renderer.setDirections(directions);
}
''', [map, directions]);
  }

  void setPanel(String selector) {
    js.invoke('''
function (renderer, selector) {
  var panel = document.querySelector(selector);
  renderer.setPanel(panel);
}
''', [this, selector]);
  }

  serialize() =>
      encode('function (map) { return map._dart_map_directions_renderer; }',
                            [map]);
} 

abstract class MVCArray extends js.Serializable {
}

class MVCMapControlArray extends MVCArray {
  GMap _map;
  final _key;

  MVCMapControlArray(this._map, this._key);

  serialize() =>
      encode('function (map, key) { return map.controls[key]; }',
                  [_map, _key]);


  void push(String selector) {
    js.invoke('''
function (array, selector) {
  var node = document.querySelector(selector);
  array.push(node);
}
''', [this, selector]);
  }
}

// TODO(vsm): Properly implement List.
class MVCMapControlArrayList implements List<MVCArray> {
  GMap _map;

  MVCMapControlArrayList(this._map);

  operator[](key) => new MVCMapControlArray(_map, key);
}
