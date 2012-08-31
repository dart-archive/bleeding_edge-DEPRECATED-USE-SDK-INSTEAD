// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('slider_sample');

#import('dart:html');
#import('../ui_lib/view/view.dart');

const menuItems = const["Apple", "Banana", "Cherry", "Durian"];

main() {
  var sliderMenu = new SliderMenu(menuItems, (selectedText) {
    displaySelection(selectedText);
  });
  query("#menu").nodes.add(sliderMenu.node);

  query('#next').on.click.add((e) {
    sliderMenu.selectNext(true);
  });

  query('#prev').on.click.add((e) {
    sliderMenu.selectPrevious(true);
  });

  document.on.keyDown.add((KeyboardEvent event) {
    switch (event.keyIdentifier) {
      case KeyName.LEFT:
        sliderMenu.selectPrevious(true);
        break;
      case KeyName.RIGHT:
        sliderMenu.selectNext(true);
        break;
    }
  });
  
  sliderMenu.enterDocument();
}

void displaySelection(String selectedText) {
  query("#notes").text = "Selection: ${selectedText}";  
}
