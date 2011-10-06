// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

typedef void PickerListener(String selectedColor);

class ColorPicker {

  static final List hexValues = ["00", "33", "66", "99", "CC", "FF"];
  static final int COLS = 18;
  // Block height, width, padding
  static final int BH = 10;
  static final int BW = 10;
  static final int BP = 1;
  final List<PickerListener> _listeners;
  HTMLCanvasElement canvasElement;
  String _selectedColor = 'red';
  final int height = 160;
  final int width = 180;
  CanvasRenderingContext2D ctx;
  
  ColorPicker(this.canvasElement) :
    _listeners = []
  {
    ctx = canvasElement.getContext("2d");
    drawPalette();
    addHandlers();
    showSelected();
  }
  
  String get selectedColor() => _selectedColor;
  
  void onMouseMove(MouseEvent event) {
    int x = event.offsetX;
    int y = event.offsetY - 40;
    if (( y < 0) || (x >= width)) {
      return;
    }
    ctx.setFillStyle(getHexString(getColorIndex(x, y)));
    ctx.fillRect(0, 0, width/2, 30);
  }

  void onMouseDown(MouseEvent event) {
    Element elt = event.target;
    event.cancelBubble = true;
    int x = event.offsetX;
    int y = event.offsetY - 40;
    if ((y < 0) || (x >= width))
      return;
    setSelected(getColorIndex(x, y));
    fireSelected();
  }

  /**
   * Adds a [PickerListener] to receive updates.
   */
  void addListener(PickerListener listener) {
    _listeners.add(listener);
  }

  void addHandlers() {
    canvasElement.onmousemove = (e) { onMouseMove(e); };
    canvasElement.onmousedown = (e) { onMouseDown(e); };
  }

   void drawPalette() {
    int i=0;
    for (int r=0; r < 256; r+=51) {
      for (int g=0; g < 256; g+=51) {
        for (int b=0; b < 256; b+=51) {
          String color = getHexString(i);
          ctx.setFillStyle(color);
          int x = BW * (i % COLS);
          int y = BH * (i ~/ COLS) + 40;
          ctx.fillRect(x + BP, y + BP, BW - 2 * BP, BH - 2 * BP);
          i++;
        }
      }
    }
  }

  void fireSelected() {
    for (final listener in _listeners) {
      listener(_selectedColor);
    }
  }

  int getColorIndex(int x, int y) {
    // Get color index 0-215 using row, col
    int i = y ~/ BH * COLS + x ~/ BW;
    return i;
  }

  void showSelected() {
    ctx.setFillStyle(_selectedColor);
    ctx.fillRect(width / 2, 0, width / 2, 30);
    ctx.setFillStyle("white");
    ctx.fillRect(0, 0, width / 2, 30);
  }
  
  void setSelected(num i) {
    _selectedColor = getHexString(i.floor());
    showSelected();
    fireSelected();
  }
 
  String getHexString(int i) {
    int r = i ~/ 36;
    int g = (i % 36) ~/ 6;
    int b = i % 6;
    return '#${hexValues[r]}${hexValues[g]}${hexValues[b]}';
  }
  
}
