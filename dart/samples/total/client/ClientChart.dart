// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ClientChart extends Chart {

  static final List<String> _strokeColors =
     const ["#ff0000", "#00ff00", "#0000ff", "#ffff00", "#ff00ff", "#00ffff", "#000000"];

  CanvasElement _canvas;

  ClientChart(this._canvas) : super() { }

  void render() { }
}

class ClientLineChart extends ClientChart {

  ClientLineChart(CanvasElement canvas) : super(canvas) {
    _width = _canvas.width;
    _height = _canvas.height;
  }

  void render() {
    CanvasRenderingContext2D ctx = _canvas.getContext("2d");
    ctx.setFillColor("#ffffff");
    ctx.clearRect(0, 0, _width, _height);

    int tickStep;
    int tickMin;
    int tickMax;

    double range = _maxValue - _minValue;
    if (range == 0) {
      tickStep = 1;
      int m = _minValue.floor().toInt();
      tickMin = m - 5;
      tickMax = m + 5;
    } else {
      double log10Range = Math.log(range) / Math.log(10.0);
      tickStep = Math.pow(10.0, (log10Range).floor()).toInt() ~/ 2;
      tickMin = (_minValue / tickStep).floor().toInt() * tickStep;
      tickMax = (_maxValue / tickStep).floor().toInt() * tickStep;
    }

    int padding = 30;

    ctx.font = "Gill Sans 8px";
    ctx.lineWidth = 1.0;

    ctx.setStrokeColor("#000000");
    ctx.beginPath();
    ctx.moveTo(padding, padding);
    ctx.lineTo(padding, height - padding);
    ctx.lineTo(width - padding, height - padding);
    ctx.stroke();

    int scaleY = (height - 2 * padding) ~/ range;
    double zeroY = (height - padding) - ((0.0 - _minValue) * scaleY);
    if (zeroY >= padding && zeroY <= height - padding) {
      ctx.beginPath();
      ctx.moveTo(padding, zeroY);
      ctx.lineTo(width - padding, zeroY);
      ctx.stroke();

      double textWidth = ctx.measureText("0").width.toDouble();
      ctx.fillText("0", padding - textWidth, zeroY);
    }

    int tickLen = 6;
    for (int tick = tickMin; tick <= tickMax; tick += tickStep) {
      double y = (height - padding) - ((tick - _minValue) * scaleY);
      if (y >= padding && y <= height - padding) {
        ctx.beginPath();
        ctx.moveTo(padding - tickLen / 2, y);
        ctx.lineTo(padding + tickLen / 2, y);
        ctx.stroke();

        String label = tick.toString();
        double textWidth = ctx.measureText(label).width.toDouble();
        ctx.fillText(label, padding - textWidth, y);
      }
    }

    for (int tick = padding; tick <= width - padding; tick += 50) {
      ctx.beginPath();
      ctx.moveTo(tick, height - padding - tickLen / 2);
      ctx.lineTo(tick, height - padding + tickLen / 2);
      ctx.stroke();
    }

    for (int col = _firstCol; col <= _lastCol; col++) {
      List<double> data = getSeries(col);
      int scaleX = (width - 2 * padding) ~/ data.length;
      scaleY = (height - 2 * padding) ~/ range;

      ctx.setStrokeColor(_strokeColors[col - _firstCol]);
      ctx.beginPath();

      for (int row = 0; row < data.length; row++) {
        double x = row * scaleX + padding;
        double y = (height - padding) - ((data[row] - _minValue) * scaleY);
        if (row == 0) {
          ctx.moveTo(x, y);
        } else {
          ctx.lineTo(x, y);
        }
      }
      ctx.stroke();
    }
  }
}
