// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

typedef void ZoomListener(double oldZoom, double newZoom);

/**
 * This class tracks changing zoom factor in browser and notifies about this asynchroniously.
 */
class ZoomTracker {
  List<ZoomListener> _listeners;
  Window _window;
  Point _zeroPoint;
  double _zoom;
  Element _zoomElement;

  ZoomTracker(this._window) {
    _listeners = new List<ZoomListener>();
    // Append element with known CSS location
    _zoomElement = new Element.tag("div");
    _zoomElement.id = "zoomTracker";
    _zoomElement.style.setProperty("position", "absolute");
    _zoomElement.style.setProperty("top", "-100px");
    _zoomElement.style.setProperty("height", "0px");
    _zoomElement.style.setProperty("visibility", "hidden");
    document.body.nodes.add(_zoomElement);
    // Prepare initial state
    _zeroPoint = new Point(0, 0);
    _zoom = _getZoom();
    // Schedule zoom check every 100ms
    _window.setInterval(() {
      double newZoom = _getZoom();
      if (newZoom != _zoom) {
        _listeners.forEach((ZoomListener listener) {
          listener(_zoom, newZoom);
        });
        _zoom = newZoom;
      }
    }, 100);
  }

  void addListener(ZoomListener listener) {
    if (_listeners.isEmpty() || _listeners.indexOf(listener, 0) == -1) {
      _listeners.add(listener);
    }
  }

  void removeListener(ZoomListener listener) {
    bool found = false;
    _listeners = _listeners.filter(bool _(element) => found || !(found = (element == listener)));
  }

  double _getZoom() {
    Point zoomPoint = _window.webkitConvertPointFromNodeToPage(_zoomElement, _zeroPoint);
    return zoomPoint.y / -100.0;
  }
}
