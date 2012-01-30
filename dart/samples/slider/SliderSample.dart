// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/**
 * Very simple sample app showing use of the [SliderMenu] view.
 */
class SliderSample  {

  SliderMenu sliderMenu;
  final List<String> menuItems;

  SliderSample() : menuItems = const["Apple", "Banana", "Cherry", "Durian"] {}

  void ready() {
    document.query("#status").innerHTML = "Slider Menu Sample App";
    sliderMenu = new SliderMenu(menuItems, (selectedText) {
      document.query("#message").innerHTML = "Selected '${selectedText}'";
    });
    document.query("#menu").nodes.add(sliderMenu.node);

    document.query('#next').on.click.add((e) {
      sliderMenu.selectNext(true);
    });

    document.query('#prev').on.click.add((e) {
      sliderMenu.selectPrevious(true);
    });

    sliderMenu.enterDocument();
  }

  static void main() {
    Dom.ready( () { new SliderSample().ready();} );
  }
}
