// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#import('dart:html');
#import('dart:math', prefix: 'Math');

class Color {
  int hue;
  double saturation;
  double lightness;

  Color(int this.hue, double this.saturation, double this.lightness);

  factory Color.rgb(double red, double green, double blue) {
    final r = red;
    final g = green;
    final b = blue;
    final max = Math.max(Math.max(r, g), b);
    final min = Math.min(Math.min(r, g), b);
    final d = max - min;

    double h;
    if (max == min) {
      h = 0.0;
    } else if (max == r) {
      h = 60 * (g-b)/d;
    } else if (max == g) {
      h = 60 * (b-r)/d + 120;
    } else { // max == b
      h = 60 * (r - g)/d + 240;
    }

    final l = (max + min)/2;

    double s;
    if (max == min) {
      s = 0.0;
    } else if (l < 0.5) {
      s = d/(2*l);
    } else {
      s = d/(2 - 2*l);
    }

    return new Color((h.round() % 360).toInt(), s, l);
  }

  factory Color.hex(String hex) => new Color.rgb(
      _parseHex(hex.substring(1, 3))/255,
      _parseHex(hex.substring(3, 5))/255,
      _parseHex(hex.substring(5, 7))/255);

  // This should be in the core library. Issue #233
  static int _parseHex(String hex) {
    final codes = hex.charCodes();
    var number = 0;
    for (var i = 0; i < codes.length; i++) {
      final code = codes[i];
      var digit;
      if (code >= 48 && code <= 57) { // 0-9
        digit = code - 48;
      } else if (code >= 97 && code <= 102) { // a-f
        digit = code - 97 + 10;
      } else {
        throw "Invalid hex string: '$hex'";
      }
      number *= 16; // shift previous digits left one place
      number += digit;
    }
    return number;
  }

  String get hex {
    final h = (hue % 360) / 360;
    final s = saturation;
    final l = lightness;

    // HSL to RGB algorithm from the CSS spec
    // http://www.w3.org/TR/css3-color/#hsl-color
    final m2 = l <= 0.5 ? l * (s + 1) : l + s - l * s;
    final m1 = l * 2 - m2;
    final r = _hueToRgb(m1, m2, h + 1/3);
    final g = _hueToRgb(m1, m2, h);
    final b = _hueToRgb(m1, m2, h - 1/3);

    return '#${_hexPair(r)}${_hexPair(g)}${_hexPair(b)}';
  }

  Color dup() => new Color(hue, saturation, lightness);

  double _hueToRgb(double m1, double m2, double h) {
    if (h < 0) h++;
    if (h > 1) h--;
    if (h * 6 < 1) return m1 + (m2 - m1) * h * 6;
    if (h * 2 < 1) return m2;
    if (h * 3 < 2) return m1 + (m2 - m1) * (2/3 - h) * 6;
    return m1;
  }

  String _hexPair(double color) {
    assert(color >= 0 && color <= 1);
    final str = (color * 0xff).round().toRadixString(16);
    return str.length == 1 ? '0$str' : str;
  }
}

SVGSVGElement logo;
InputElement hue, saturation, lightness;
Map<String, Color> defaultColors;

onSliderChange(_) {
  final hueDelta = Math.parseInt(hue.value) - 180;
  final saturationMod = Math.parseInt(saturation.value)/100;
  final lightnessMod = Math.parseInt(lightness.value)/100;

  logo.queryAll("path").forEach((p) {
    final color = defaultColors[p.id].dup();
    color.hue += hueDelta;

    if (saturationMod > 0) {
      color.saturation += saturationMod * (1 - color.saturation);
    } else {
      color.saturation += saturationMod * color.saturation;
    }

    if (lightnessMod > 0) {
      color.lightness += lightnessMod * (1 - color.lightness);
    } else {
      color.lightness += lightnessMod * color.lightness;
    }

    p.style.setProperty('fill', color.hex);
  });
}

void main() {
  defaultColors = {};

  logo = new SVGElement.svg("""
<svg xmlns="http://www.w3.org/2000/svg"
     version="1.1"
     width="371.6655"
     height="374.14087">
  <filter id="inverse"> 
     <feComponentTransfer> 
         <feFuncR type="table" tableValues="1 0"/> 
         <feFuncG type="table" tableValues="1 0"/> 
         <feFuncB type="table" tableValues="1 0"/> 
     </feComponentTransfer> 
  </filter>
  <path
     d="m 101.86949,101.86487 -24.192001,-24.192004 0.088,174.807994 0.296,8.164 c 0.12,3.848 0.84,8.18 2.012,12.684 l 191.615991,67.55601 47.89201,-21.216 0.016,-0.056 -217.728,-217.748 z"
     id="path2900"
     style="fill: #31beb2" />
  <path
     d="m 80.073489,273.32486 0.02,0.008 c -0.02,-0.084 -0.052,-0.168 -0.076,-0.252 0.028,0.084 0.036,0.16 0.056,0.244 z m 239.524001,46.28401 m -0.016,0.056 -47.89201,21.216 -191.591991,-67.54801 c 3.656,14.044 11.764,29.83201 20.476001,38.45601 l 62.52,62.172 139.052,0.18 17.452,-54.532 -0.016,0.056 z"
     id="path2902"
     style="fill: #75ccc3" />
  <path
     d="M 77.673489,77.668866 3.1974828,190.16487 c -6.192,6.616 -3.11599997,20.244 6.8520002,30.276 l 42.996005,43.35199 27.028001,9.528 c -1.172,-4.504 -1.892,-8.832 -2.012,-12.684 l -0.296,-8.164 -0.092,-174.803994 0,0 z"
     id="path2904"
     style="fill: #008bc9" />
  <path
     d="m 273.26148,79.920866 c -4.48,-1.148 -8.808,-1.856 -12.708,-1.98 l -8.64,-0.308 -174.239991,0.036 241.960001,241.940004 0.02,-0.008 21.24,-47.93601 -67.63201,-191.743994 z"
     id="path2906"
     style="fill: #0082c4" />
  <path
     d="m 273.04948,79.876866 c 0.072,0.02 0.148,0.044 0.22,0.064 l -0.004,-0.02 c -0.076,-0.02 -0.144,-0.024 -0.216,-0.044 z m 38.66001,20.576004 c -8.792,-8.860004 -24.37601,-16.912004 -38.44001,-20.512004 l 67.62801,191.727994 -21.24,47.93601 -0.02,0.008 51.928,-16.592 0.1,-142.368 -59.956,-60.2 z"
     id="path2908"
     style="fill: #008bc9" />
  <path
     d="M 263.78548,53.048866 220.45349,10.028873 c -10.02,-9.92799955 -23.664,-13.0239996 -30.26,-6.8439996 L 77.677489,77.672866 l 174.239991,-0.036 8.64,0.308 c 3.9,0.124 8.228,0.832 12.708,1.98 l -9.48,-26.876 z m -186.111991,24.62"
     id="path2910"
     style="fill: #75ccc3" />
</svg>
""");

  query("#icon").elements.add(logo);
  logo.queryAll("path").forEach((p) {
    defaultColors[p.id] = new Color.hex(p.style.getPropertyValue('fill'));
  });

  hue = document.query("input[name=hue]");
  hue.on.change.add(onSliderChange);
  saturation = document.query("input[name=saturation]");
  saturation.on.change.add(onSliderChange);
  lightness = document.query("input[name=lightness]");
  lightness.on.change.add(onSliderChange);

  document.query("input[name=invert]").on.change.add((Event e) {
    InputElement invert = e.target;
    if (invert.checked) {
      logo.classes = ['inverse'];
    } else {
      logo.classes = [];
    }
  });
}
