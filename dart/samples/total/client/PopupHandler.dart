// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class PopupHandler {
  // references to CSS classes for fade in and fade out transitions
  static final String _fadeInClass = "fadeIn";
  static final String _fadeOutClass = "fadeOut";
  static PopupHandler _instance; // singleton

  Element _activePopup;

  factory PopupHandler(Document doc) {
    if (_instance == null) {
      _instance = new PopupHandler._internal(doc);
    }
    return _instance;
  }

  PopupHandler._internal(Document doc) {
    Element body = doc.body;

    body.on.click.add((Event e) {
      if (_activePopup == null) {
        return;
      }
      Element target = e.target;
      while (target != null && target != body && target != _activePopup) {
        target = target.parent;
      }
      if (target != _activePopup) {
        deactivatePopup();
      }
    }, true);
  }

  void activatePopup(Element popup, int x, int y) {
    if (_activePopup != null) {
      deactivatePopup();
    }

    _activePopup = popup;
    _activePopup.style.setProperty("left", HtmlUtils.toPx(x));
    _activePopup.style.setProperty("top", HtmlUtils.toPx(y));

    Set<String> classes = _activePopup.classes;
    classes.remove(_fadeOutClass);
    classes.add(_fadeInClass);
  }

  void deactivatePopup() {
    if (_activePopup == null) {
      return;
    }
    Set<String> classes = _activePopup.classes;
    classes.remove(_fadeInClass);
    classes.add(_fadeOutClass);
    _activePopup = null;
  }
}
