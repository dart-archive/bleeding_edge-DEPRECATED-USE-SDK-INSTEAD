// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// A JS Interop sample accessing the Google Maps API.  The sample is based on
// the marker-simple example here:
// https://google-developers.appspot.com/maps/documentation/javascript/examples/marker-simple
//
// In this example you can see how to build a typed Dart API based on js API.
// Doing this, you will improve developper experience (content assist, errors on
// undefined members...).

import 'dart:html';
import 'package:js/js.dart' as js;

final maps = js.retain(js.context.google.maps);

class LatLng implements js.Serializable<js.Proxy> {
  final js.Proxy _proxy;

  LatLng(num lat, num lng) : this._(new js.Proxy(maps.LatLng, lat, lng));
  LatLng._(this._proxy);

  js.Proxy toJs() => _proxy;
}

class MapTypeId implements js.Serializable<String> {
  static final HYBRID = new MapTypeId._(maps.MapTypeId.HYBRID);
  static final ROADMAP = new MapTypeId._(maps.MapTypeId.ROADMAP);
  static final SATELLITE = new MapTypeId._(maps.MapTypeId.SATELLITE);
  static final TERRAIN = new MapTypeId._(maps.MapTypeId.TERRAIN);

  String _value;

  MapTypeId._(this._value);

  String toJs() => this._value;
}

class MapOptions implements js.Serializable<js.Proxy> {
  final js.Proxy _proxy;

  MapOptions() : this._(new js.Proxy(js.context.Object));
  MapOptions._(this._proxy);

  set center(LatLng center) => _proxy.center = center;
  set mapTypeId(MapTypeId mapTypeId) => _proxy.mapTypeId = mapTypeId;
  set zoom(num zoom) => _proxy.zoom = zoom;

  js.Proxy toJs() => _proxy;
}

class GMap implements js.Serializable<js.Proxy> {
  final js.Proxy _proxy;

  GMap(Element container, MapOptions options) : this._(new js.Proxy(maps.Map, container, options));
  GMap._(this._proxy);

  set center(LatLng center) => _proxy.center = center;
  set mapTypeId(MapTypeId mapTypeId) => _proxy.mapTypeId = mapTypeId;
  set zoom(num zoom) => _proxy.zoom = zoom;

  js.Proxy toJs() => _proxy;
}

class MarkerOptions implements js.Serializable<js.Proxy> {
  final js.Proxy _proxy;

  MarkerOptions() : this._(new js.Proxy(js.context.Object));
  MarkerOptions._(this._proxy);

  set position(LatLng position) => _proxy.position = position;
  set map(GMap map) => _proxy.map = map;
  set title(String title) => _proxy.title = title;

  js.Proxy toJs() => _proxy;
}

class Marker implements js.Serializable<js.Proxy> {
  final js.Proxy _proxy;

  Marker(MarkerOptions options) : this._(new js.Proxy(maps.Marker, options));
  Marker._(this._proxy);

  js.Proxy toJs() => _proxy;
}

void main() {
  js.scoped(() {
    final myLatlng = new LatLng(-25.363882,131.044922);
    final mapOptions = new MapOptions()
      ..zoom = 4
      ..center = myLatlng
      ..mapTypeId = MapTypeId.ROADMAP
      ;
    final map = new GMap(query("#map_canvas"), mapOptions);

    final marker = new Marker(new MarkerOptions()
      ..position = myLatlng
      ..map = map
      ..title = "Hello World!"
    );
  });
}