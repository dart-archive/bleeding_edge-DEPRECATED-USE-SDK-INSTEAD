// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class Settings {
  // theme values:
  static const THEME_SIMPLE = 1;
  static const THEME_BUTTON = 2;

  SettingsDialog ui;
  int theme;

  bool _dialogOpened;

  Settings(this.ui, this.theme) : _dialogOpened = false {
    ui.settings.on.click.add((MouseEvent e) {
      mySettings.dialog();
      e.cancelBubble = true;
    });
    ui.simpleTitle.on.click.add((MouseEvent e) {
      ui.simple.checked = true;
    });
    ui.buttonTitle.on.click.add((MouseEvent e) {
      ui.buttons.checked = true;
    });

  }

  /**
   * Settings dialog
   */
  void dialog() {
    decorateDropdown();

    if (!_dialogOpened) {
      ui.simple.checked = theme == THEME_SIMPLE;
      ui.buttons.checked = theme == THEME_BUTTON;

      ui.settingsDialog.style.visibility = "visible";

      _dialogOpened = true;
    } else {
      close();
    }
  }

  void decorateDropdown() {
    ui.settings.style.backgroundColor = _dialogOpened ?
      "transparent" : "#333";
  }

  bool get isOpen() => _dialogOpened;

  /*
   * Optional MouseEvent if passed if the source of event is from outside of
   * the dialog then the dialog can be closed.
   */
  void close([MouseEvent e]) {
    if (isOpen) {
      if (ui.simple.checked) {
        theme = THEME_SIMPLE;
      } else if (ui.buttons.checked) {
        theme = THEME_BUTTON;
      }

      if (e == null || !ui.settingsDialog.contains(e.srcElement)) {
        decorateDropdown();
        ui.settingsDialog.style.visibility = "hidden";
        _dialogOpened = false;
      }
    }
  }

  bool get isSimple() => theme == THEME_SIMPLE;
  bool get isButton() => theme == THEME_BUTTON;

}
