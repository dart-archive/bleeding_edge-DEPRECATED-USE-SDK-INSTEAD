// Generated Dart class from HTML template.
// DO NOT EDIT.

class FlatPadUI {
  Map<String, Object> _scopes;
  Element _fragment;


  // Elements bound to a variable:
  var keySeven;
  var keyEight;
  var keyNine;
  var keyPlus;
  var keyMinus;
  var keyFour;
  var keyFive;
  var keySix;
  var keyStar;
  var keySlash;
  var keyOne;
  var keyTwo;
  var keyThree;
  var keyEqual;
  var keyClear;
  var keyZero;
  var keyDot;

  FlatPadUI() : _scopes = new Map<String, Object>() {
    // Insure stylesheet for template exist in the document.
    add_calcui_templatesStyles();

    _fragment = new DocumentFragment();
    var e0 = new Element.html('<div class="calc-pad"></div>');
    _fragment.elements.add(e0);
    var e1 = new Element.html('<div class="calc-row"></div>');
    e0.elements.add(e1);
    keySeven = new Element.html('<span class="calc-num">7</span>');
    e1.elements.add(keySeven);
    keyEight = new Element.html('<span class="calc-num">8</span>');
    e1.elements.add(keyEight);
    keyNine = new Element.html('<span class="calc-num">9</span>');
    e1.elements.add(keyNine);
    keyPlus = new Element.html('<div class="op-plus-base op-plus"></div>');
    e1.elements.add(keyPlus);
    keyMinus = new Element.html('<div class="op-minus-base op-minus"></div>');
    e1.elements.add(keyMinus);
    var e2 = new Element.html('<div class="calc-row"></div>');
    e0.elements.add(e2);
    keyFour = new Element.html('<span class="calc-num">4</span>');
    e2.elements.add(keyFour);
    keyFive = new Element.html('<span class="calc-num">5</span>');
    e2.elements.add(keyFive);
    keySix = new Element.html('<span class="calc-num">6</span>');
    e2.elements.add(keySix);
    keyStar = new Element.html('<div class="op-mult-base op-mult"></div>');
    e2.elements.add(keyStar);
    keySlash = new Element.html('<div class="op-div-base op-div"></div>');
    e2.elements.add(keySlash);
    var e3 = new Element.html('<div class="calc-row"></div>');
    e0.elements.add(e3);
    keyOne = new Element.html('<span class="calc-num">1</span>');
    e3.elements.add(keyOne);
    keyTwo = new Element.html('<span class="calc-num">2</span>');
    e3.elements.add(keyTwo);
    keyThree = new Element.html('<span class="calc-num">3</span>');
    e3.elements.add(keyThree);
    keyEqual = new Element.html('<div class="op-equal-base op-equal"></div>');
    e3.elements.add(keyEqual);
    keyClear = new Element.html('<div class="op-arrow-base op-arrow"></div>');
    e3.elements.add(keyClear);
    var e4 = new Element.html('<div class="calc-row"></div>');
    e0.elements.add(e4);
    keyZero = new Element.html('<span class="calc-num">0</span>');
    e4.elements.add(keyZero);
    keyDot = new Element.html('<span class="calc-period"></span>');
    e4.elements.add(keyDot);
    var e5 = new Element.html('<span style="font-size:9pt;">&nbsp;&nbsp;</span>');
    keyDot.elements.add(e5);
    var e6 = new Text('.');
    keyDot.elements.add(e6);
    var e7 = new Element.html('<span style="font-size:9pt;">&nbsp;&nbsp;</span>');
    keyDot.elements.add(e7);
  }

  Element get root => _fragment;

  // CSS class selectors for this template.
  static String get calcPad => "calc-pad";
  static String get calcRow => "calc-row";
  static String get calcNum => "calc-num";
  static String get calcPeriod => "calc-period";
  static String get calcOpButton => "calc-op-button";
  static String get calcNumHover => "calc-num-hover";
  static String get calcNumActive => "calc-num-active";
  static String get calcPeriodHover => "calc-period-hover";
  static String get calcPeriodActive => "calc-period-active";
  static String get calcPeriodError => "calc-period-error";
  static String get opPlusBase => "op-plus-base";
  static String get opPlus => "op-plus";
  static String get opPlusHover => "op-plus-hover";
  static String get opPlusActive => "op-plus-active";
  static String get opMinusBase => "op-minus-base";
  static String get opMinus => "op-minus";
  static String get opMinusHover => "op-minus-hover";
  static String get opMinusActive => "op-minus-active";
  static String get opMultBase => "op-mult-base";
  static String get opMult => "op-mult";
  static String get opMultHover => "op-mult-hover";
  static String get opMultActive => "op-mult-active";
  static String get opDivBase => "op-div-base";
  static String get opDiv => "op-div";
  static String get opDivHover => "op-div-hover";
  static String get opDivActive => "op-div-active";
  static String get opEqualBase => "op-equal-base";
  static String get opEqual => "op-equal";
  static String get opEqualHover => "op-equal-hover";
  static String get opEqualActive => "op-equal-active";
  static String get opArrowBase => "op-arrow-base";
  static String get opArrow => "op-arrow";
  static String get opArrowHover => "op-arrow-hover";
  static String get opArrowActive => "op-arrow-active";

  // Injection functions:
  // Each functions:

  // With functions:

  // CSS for this template.
  static const String stylesheet = '''

.calc-pad {
  background-color: #222;
  width: 241px;
  height: 168px;
  padding-top: 4px;
  padding-bottom: 4px;
  cursor: default;
}

.calc-row {
  color: #ffffff;
  font-size: 25pt;
  font-family: arial;
  font-weight: bold;
  padding-top: 2px;
  padding-left: 12px;
}

.calc-num {
  padding-left: 8px;
  padding-right: 8px;
  margin-right: 11px;
  cursor: pointer;
}

.calc-period {
  padding-left: 7px;
  padding-right: 7px;
  margin-right: 11px;
  cursor: pointer;
  font-size: 24pt;
}

.calc-op-button {
  padding-left: 12px;
  padding-right: 19px;
  color: #808080;
  font-size: 20pt;
  vertical-align: middle;
}

.calc-num:hover {
  width: 100px;
  height: 100px;
  background: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 50px;
  border-radius: 50px;
  color: #000;
}

.calc-num-hover {
  width: 100px;
  height: 100px;
  background: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 50px;
  border-radius: 50px;
  color: #000;
}

.calc-num:active {
  width: 100px;
  height: 100px;
  background: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 50px;
  border-radius: 50px;
  color: #ffffff;
}

.calc-num-active {
  width: 100px;
  height: 100px;
  background: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 50px;
  border-radius: 50px;
  color: #ffffff;
}

.calc-period:hover {
  width: 100px;
  height: 100px;
  background: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 50px;
  border-radius: 50px;
  color: #000;
}

.calc-period-hover {
  width: 100px;
  height: 100px;
  background: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 50px;
  border-radius: 50px;
  color: #000;
}

.calc-period:active {
  width: 100px;
  height: 100px;
  background: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 50px;
  border-radius: 50px;
  color: #ffffff;
}

.calc-period-active {
  width: 100px;
  height: 100px;
  background: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 50px;
  border-radius: 50px;
  color: #ffffff;
}

.calc-period-error {
  width: 100px;
  height: 100px;
  background: #cd5c5c;
  -moz-border-radius: 50px;
  -webkit-border-radius: 50px;
  border-radius: 50px;
  color: #b22222;
}

.op-plus-base {
  width: 35px;
  height: 35px;
  position: absolute;
  top: 185px;
  left: 158px;
}

.op-plus {
  background-image: url(plus.png);
  background-repeat: no-repeat;
}

.op-plus:hover {
  background-image: url(plus_black.png);
  background-repeat: no-repeat;
  background-color: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-plus-hover {
  background-image: url(plus_black.png);
  background-repeat: no-repeat;
  background-color: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-plus:active {
  background-image: url(plus_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-plus-active {
  background-image: url(plus_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-minus-base {
  width: 35px;
  height: 35px;
  position: absolute;
  top: 185px;
  left: 198px;
}

.op-minus {
  background-image: url(minus.png);
  background-repeat: no-repeat;
}

.op-minus:hover {
  background-image: url(minus_black.png);
  background-repeat: no-repeat;
  background-color: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-minus-hover {
  background-image: url(minus_black.png);
  background-repeat: no-repeat;
  background-color: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-minus:active {
  background-image: url(minus_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-minus-active {
  background-image: url(minus_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-mult-base {
  width: 35px;
  height: 35px;
  position: absolute;
  top: 228px;
  left: 158px;
}

.op-mult {
  background-image: url(mult.png);
  background-repeat: no-repeat;
}

.op-mult:hover {
  background-image: url(mult_black.png);
  background-repeat: no-repeat;
  background-color: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-mult-hover {
  background-image: url(mult_black.png);
  background-repeat: no-repeat;
  background-color: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-mult:active {
  background-image: url(mult_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-mult-active {
  background-image: url(mult_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-div-base {
  width: 35px;
  height: 35px;
  position: absolute;
  top: 228px;
  left: 198px;
}

.op-div {
  background-image: url(div.png);
  background-repeat: no-repeat;
}

.op-div:hover {
  background-image: url(div_black.png);
  background-repeat: no-repeat;
  background-color: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-div-hover {
  background-image: url(div_black.png);
  background-repeat: no-repeat;
  background-color: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-div:active {
  background-image: url(div_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-div-active {
  background-image: url(div_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-equal-base {
  width: 35px;
  height: 35px;
  position: absolute;
  top: 268px;
  left: 158px;
}

.op-equal {
  background-image: url(equal.png);
  background-repeat: no-repeat;
}

.op-equal:hover {
  background-image: url(equal_black.png);
  background-repeat: no-repeat;
  background-color: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-equal-hover {
  background-image: url(equal_black.png);
  background-repeat: no-repeat;
  background-color: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-equal:active {
  background-image: url(equal_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-equal-active {
  background-image: url(equal_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-arrow-base {
  width: 35px;
  height: 35px;
  position: absolute;
  top: 268px;
  left: 198px;
}

.op-arrow {
  background-image: url(arrow.png);
  background-repeat: no-repeat;
}

.op-arrow:hover {
  background-image: url(arrow_black.png);
  background-repeat: no-repeat;
  background-color: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-arrow-hover {
  background-image: url(arrow_black.png);
  background-repeat: no-repeat;
  background-color: #d3d3d3;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-arrow:active {
  background-image: url(arrow_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

.op-arrow-active {
  background-image: url(arrow_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  -moz-border-radius: 50px;
  -webkit-border-radius: 20px;
  border-radius: 20px;
  background-position: center;
}

  ''';

  // Stylesheet class selectors:
  String safeHTML(String html) {
    // TODO(terry): Escaping for XSS vulnerabilities TBD.
    return html;
  }
}
class ButtonPadUI {
  Map<String, Object> _scopes;
  Element _fragment;


  // Elements bound to a variable:
  var keySeven;
  var keyEight;
  var keyNine;
  var keyPlus;
  var keyMinus;
  var keyFour;
  var keyFive;
  var keySix;
  var keyStar;
  var keySlash;
  var keyOne;
  var keyTwo;
  var keyThree;
  var keyEqual;
  var keyClear;
  var keyZero;
  var keyDot;

  ButtonPadUI() : _scopes = new Map<String, Object>() {
    // Insure stylesheet for template exist in the document.
    add_calcui_templatesStyles();

    _fragment = new DocumentFragment();
    var e0 = new Element.html('<div class="button-pad"></div>');
    _fragment.elements.add(e0);
    var e1 = new Element.html('<div class="button-row"></div>');
    e0.elements.add(e1);
    keySeven = new Element.html('<span class="button-num">7</span>');
    e1.elements.add(keySeven);
    keyEight = new Element.html('<span class="button-num">8</span>');
    e1.elements.add(keyEight);
    keyNine = new Element.html('<span class="button-num">9</span>');
    e1.elements.add(keyNine);
    keyPlus = new Element.html('<div class="buttonop-plus-base buttonop-plus"></div>');
    e1.elements.add(keyPlus);
    keyMinus = new Element.html('<div class="buttonop-minus-base buttonop-minus"></div>');
    e1.elements.add(keyMinus);
    var e2 = new Element.html('<div class="button-row"></div>');
    e0.elements.add(e2);
    keyFour = new Element.html('<span class="button-num">4</span>');
    e2.elements.add(keyFour);
    keyFive = new Element.html('<span class="button-num">5</span>');
    e2.elements.add(keyFive);
    keySix = new Element.html('<span class="button-num">6</span>');
    e2.elements.add(keySix);
    keyStar = new Element.html('<div class="buttonop-mult-base buttonop-mult"></div>');
    e2.elements.add(keyStar);
    keySlash = new Element.html('<div class="buttonop-div-base buttonop-div"></div>');
    e2.elements.add(keySlash);
    var e3 = new Element.html('<div class="button-row"></div>');
    e0.elements.add(e3);
    keyOne = new Element.html('<span class="button-num">1</span>');
    e3.elements.add(keyOne);
    keyTwo = new Element.html('<span class="button-num">2</span>');
    e3.elements.add(keyTwo);
    keyThree = new Element.html('<span class="button-num">3</span>');
    e3.elements.add(keyThree);
    keyEqual = new Element.html('<div class="buttonop-equal-base buttonop-equal"></div>');
    e3.elements.add(keyEqual);
    keyClear = new Element.html('<div class="buttonop-arrow-base buttonop-arrow"></div>');
    e3.elements.add(keyClear);
    var e4 = new Element.html('<div style="padding-top: 1px;" class="button-row"></div>');
    e0.elements.add(e4);
    keyZero = new Element.html('<span class="button-num">0</span>');
    e4.elements.add(keyZero);
    keyDot = new Element.html('<span class="button-period"></span>');
    e4.elements.add(keyDot);
    var e5 = new Element.html('<span style="font-size:9pt;">&nbsp;&nbsp;</span>');
    keyDot.elements.add(e5);
    var e6 = new Text('.');
    keyDot.elements.add(e6);
    var e7 = new Element.html('<span style="font-size:9pt;">&nbsp;&nbsp;</span>');
    keyDot.elements.add(e7);
  }

  Element get root => _fragment;

  // CSS class selectors for this template.
  static String get buttonPad => "button-pad";
  static String get buttonRow => "button-row";
  static String get buttonNum => "button-num";
  static String get buttonPeriod => "button-period";
  static String get buttonNumHover => "button-num-hover";
  static String get buttonNumActive => "button-num-active";
  static String get buttonPeriodHover => "button-period-hover";
  static String get buttonPeriodActive => "button-period-active";
  static String get buttonPeriodError => "button-period-error";
  static String get buttonopPlusBase => "buttonop-plus-base";
  static String get buttonopPlus => "buttonop-plus";
  static String get buttonopPlusHover => "buttonop-plus-hover";
  static String get buttonopPlusActive => "buttonop-plus-active";
  static String get buttonopMinusBase => "buttonop-minus-base";
  static String get buttonopMinus => "buttonop-minus";
  static String get buttonopMinusHover => "buttonop-minus-hover";
  static String get buttonopMinusActive => "buttonop-minus-active";
  static String get buttonopMultBase => "buttonop-mult-base";
  static String get buttonopMult => "buttonop-mult";
  static String get buttonopMultHover => "buttonop-mult-hover";
  static String get buttonopMultActive => "buttonop-mult-active";
  static String get buttonopDivBase => "buttonop-div-base";
  static String get buttonopDiv => "buttonop-div";
  static String get buttonopDivHover => "buttonop-div-hover";
  static String get buttonopDivActive => "buttonop-div-active";
  static String get buttonopEqualBase => "buttonop-equal-base";
  static String get buttonopEqual => "buttonop-equal";
  static String get buttonopEqualHover => "buttonop-equal-hover";
  static String get buttonopEqualActive => "buttonop-equal-active";
  static String get buttonopArrowBase => "buttonop-arrow-base";
  static String get buttonopArrow => "buttonop-arrow";
  static String get buttonopArrowHover => "buttonop-arrow-hover";
  static String get buttonopArrowActive => "buttonop-arrow-active";

  // Injection functions:
  // Each functions:

  // With functions:

  // CSS for this template.
  static const String stylesheet = '''

.button-pad {
  background-color: #F4F4F4;
  width: 241px;
  height: 168px;
  padding-top: 8px;
  padding-bottom: 4px;
  cursor: default;
}

.button-row {
  color: #444;
  font-size: 16pt;
  font-family: arial;
  font-weight: bold;
  padding-bottom: 10px;
  padding-left: 12px;
  margin-top: 5px;
}

.button-num {
  padding-left: 12px;
  padding-right: 12px;
  margin-right: 6px;
  cursor: pointer;
  border: 1px solid #808080;
  padding-top: 5px;
  padding-bottom: 6px;
  background-color: #FCFCFC;
  -moz-box-shadow: 1px 1px 1px 1px #ccc;
  -webkit-box-shadow: 1px 1px 1px 1px #ccc;
  box-shadow: 1px 1px 1px 1px #ccc;
  -moz-border-radius: 3px;
  -webkit-border-radius: 3px;
  border-radius: 3px;
}

.button-period {
  padding-left: 9px;
  padding-right: 8px;
  margin-right: 6px;
  cursor: pointer;
  border: 1px solid #808080;
  padding-top: 5px;
  padding-bottom: 6px;
  background-color: #FCFCFC;
  -moz-box-shadow: 1px 1px 1px 1px #ccc;
  -webkit-box-shadow: 1px 1px 1px 1px #ccc;
  box-shadow: 1px 1px 1px 1px #ccc;
  -moz-border-radius: 3px;
  -webkit-border-radius: 3px;
  border-radius: 3px;
}

.button-num:hover {
  width: 100px;
  height: 100px;
  background: #d3d3d3;
  color: #000;
}

.button-num-hover {
  width: 100px;
  height: 100px;
  background: #d3d3d3;
  color: #000;
}

.button-num:active {
  width: 100px;
  height: 100px;
  background: #778899;
  color: #ffffff;
}

.button-num-active {
  width: 100px;
  height: 100px;
  background: #778899;
  color: #ffffff;
}

.button-period:hover {
  width: 100px;
  height: 100px;
  background: #d3d3d3;
  color: #000;
}

.button-period-hover {
  width: 100px;
  height: 100px;
  background: #d3d3d3;
  color: #000;
}

.button-period:active {
  width: 100px;
  height: 100px;
  background: #778899;
  color: #ffffff;
}

.button-period-active {
  width: 100px;
  height: 100px;
  background: #778899;
  color: #ffffff;
}

.button-period-error {
  width: 100px;
  height: 100px;
  background: #cd5c5c;
  color: #b22222;
}

.buttonop-plus-base {
  width: 35px;
  height: 35px;
  position: absolute;
  top: 185px;
  left: 158px;
}

.buttonop-plus {
  background-image: url(plus.png);
  background-repeat: no-repeat;
  background-color: #DDD;
  border: 1px solid #808080;
  -moz-box-shadow: 1px 1px 1px 1px #CCC;
  -webkit-box-shadow: 1px 1px 1px 1px #CCC;
  box-shadow: 1px 1px 1px 1px #CCC;
  -moz-border-radius: 3px;
  -webkit-border-radius: 3px;
  border-radius: 3px;
}

.buttonop-plus:hover {
  background-image: url(plus_black.png);
  background-repeat: no-repeat;
  background-color: #EEE;
  background-position: center;
}

.buttonop-plus-hover {
  background-image: url(plus_black.png);
  background-repeat: no-repeat;
  background-color: #EEE;
  background-position: center;
}

.buttonop-plus:active {
  background-image: url(plus_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  background-position: center;
}

.buttonop-plus-active {
  background-image: url(plus_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  background-position: center;
}

.buttonop-minus-base {
  width: 35px;
  height: 35px;
  position: absolute;
  top: 185px;
  left: 201px;
}

.buttonop-minus {
  background-image: url(minus.png);
  background-repeat: no-repeat;
  background-color: #DDD;
  border: 1px solid #808080;
  -moz-box-shadow: 1px 1px 1px 1px #CCC;
  -webkit-box-shadow: 1px 1px 1px 1px #CCC;
  box-shadow: 1px 1px 1px 1px #CCC;
  -moz-border-radius: 3px;
  -webkit-border-radius: 3px;
  border-radius: 3px;
}

.buttonop-minus:hover {
  background-image: url(minus_black.png);
  background-repeat: no-repeat;
  background-color: #EEE;
  background-position: center;
}

.buttonop-minus-hover {
  background-image: url(minus_black.png);
  background-repeat: no-repeat;
  background-color: #EEE;
  background-position: center;
}

.buttonop-minus:active {
  background-image: url(minus_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  background-position: center;
}

.buttonop-minus-active {
  background-image: url(minus_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  background-position: center;
}

.buttonop-mult-base {
  width: 35px;
  height: 35px;
  position: absolute;
  top: 225px;
  left: 158px;
}

.buttonop-mult {
  background-image: url(mult.png);
  background-repeat: no-repeat;
  background-color: #DDD;
  border: 1px solid #808080;
  -moz-box-shadow: 1px 1px 1px 1px #CCC;
  -webkit-box-shadow: 1px 1px 1px 1px #CCC;
  box-shadow: 1px 1px 1px 1px #CCC;
  -moz-border-radius: 3px;
  -webkit-border-radius: 3px;
  border-radius: 3px;
}

.buttonop-mult:hover {
  background-image: url(mult_black.png);
  background-repeat: no-repeat;
  background-color: #EEE;
  background-position: center;
}

.buttonop-mult-hover {
  background-image: url(mult_black.png);
  background-repeat: no-repeat;
  background-color: #EEE;
  background-position: center;
}

.buttonop-mult:active {
  background-image: url(mult_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  background-position: center;
}

.buttonop-mult-active {
  background-image: url(mult_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  background-position: center;
}

.buttonop-div-base {
  width: 35px;
  height: 35px;
  position: absolute;
  top: 225px;
  left: 201px;
}

.buttonop-div {
  background-image: url(div.png);
  background-repeat: no-repeat;
  background-color: #DDD;
  border: 1px solid #808080;
  -moz-box-shadow: 1px 1px 1px 1px #CCC;
  -webkit-box-shadow: 1px 1px 1px 1px #CCC;
  box-shadow: 1px 1px 1px 1px #CCC;
  -moz-border-radius: 3px;
  -webkit-border-radius: 3px;
  border-radius: 3px;
}

.buttonop-div:hover {
  background-image: url(div_black.png);
  background-repeat: no-repeat;
  background-color: #EEE;
  background-position: center;
}

.buttonop-div-hover {
  background-image: url(div_black.png);
  background-repeat: no-repeat;
  background-color: #EEE;
  background-position: center;
}

.buttonop-div:active {
  background-image: url(div_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  background-position: center;
}

.buttonop-div-active {
  background-image: url(div_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  background-position: center;
}

.buttonop-equal-base {
  width: 35px;
  height: 35px;
  position: absolute;
  top: 266px;
  left: 158px;
}

.buttonop-equal {
  background-image: url(equal.png);
  background-repeat: no-repeat;
  background-color: #DDD;
  border: 1px solid #808080;
  -moz-box-shadow: 1px 1px 1px 1px #CCC;
  -webkit-box-shadow: 1px 1px 1px 1px #CCC;
  box-shadow: 1px 1px 1px 1px #CCC;
  -moz-border-radius: 3px;
  -webkit-border-radius: 3px;
  border-radius: 3px;
}

.buttonop-equal:hover {
  background-image: url(equal_black.png);
  background-repeat: no-repeat;
  background-color: #EEE;
  background-position: center;
}

.buttonop-equal-hover {
  background-image: url(equal_black.png);
  background-repeat: no-repeat;
  background-color: #EEE;
  background-position: center;
}

.buttonop-equal:active {
  background-image: url(equal_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  background-position: center;
}

.buttonop-equal-active {
  background-image: url(equal_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  background-position: center;
}

.buttonop-arrow-base {
  width: 35px;
  height: 35px;
  position: absolute;
  top: 266px;
  left: 201px;
}

.buttonop-arrow {
  background-image: url(arrow.png);
  background-repeat: no-repeat;
  background-color: #DDD;
  border: 1px solid #808080;
  -moz-box-shadow: 1px 1px 1px 1px #CCC;
  -webkit-box-shadow: 1px 1px 1px 1px #CCC;
  box-shadow: 1px 1px 1px 1px #CCC;
  -moz-border-radius: 3px;
  -webkit-border-radius: 3px;
  border-radius: 3px;
}

.buttonop-arrow:hover {
  background-image: url(arrow_black.png);
  background-repeat: no-repeat;
  background-color: #EEE;
  background-position: center;
}

.buttonop-arrow-hover {
  background-image: url(arrow_black.png);
  background-repeat: no-repeat;
  background-color: #EEE;
  background-position: center;
}

.buttonop-arrow:active {
  background-image: url(arrow_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  background-position: center;
}

.buttonop-arrow-active {
  background-image: url(arrow_white.png);
  background-repeat: no-repeat;
  background-color: #778899;
  background-position: center;
}

  ''';

  // Stylesheet class selectors:
  String safeHTML(String html) {
    // TODO(terry): Escaping for XSS vulnerabilities TBD.
    return html;
  }
}
class SettingsDialog {
  Map<String, Object> _scopes;
  Element _fragment;


  // Elements bound to a variable:
  var settings;
  var settingsDialog;
  var simpleTitle;
  var simple;
  var buttonTitle;
  var buttons;

  SettingsDialog() : _scopes = new Map<String, Object>() {
    // Insure stylesheet for template exist in the document.
    add_calcui_templatesStyles();

    _fragment = new DocumentFragment();
    var e0 = new Element.html('<div></div>');
    _fragment.elements.add(e0);
    settings = new Element.html('<div class="setting-glyph"></div>');
    e0.elements.add(settings);
    settingsDialog = new Element.html('<div class="dialog" style="visibility: hidden;"></div>');
    e0.elements.add(settingsDialog);
    var e1 = new Element.html('<div class="dlg-area"></div>');
    settingsDialog.elements.add(e1);
    var e2 = new Element.html('<div class="dlg-subtitle">Theme:</div>');
    e1.elements.add(e2);
    simpleTitle = new Element.html('<div style="cursor: pointer;"></div>');
    e1.elements.add(simpleTitle);
    simple = new Element.html('<input class="dlg-item" name="theme" type="radio"></input>');
    simpleTitle.elements.add(simple);
    var e3 = new Element.html('<span>Simple</span>');
    simpleTitle.elements.add(e3);
    buttonTitle = new Element.html('<div style="cursor: pointer;"></div>');
    e1.elements.add(buttonTitle);
    buttons = new Element.html('<input class="dlg-item" name="theme" type="radio"></input>');
    buttonTitle.elements.add(buttons);
    var e4 = new Element.html('<span>Buttons</span>');
    buttonTitle.elements.add(e4);
  }

  Element get root => _fragment;

  // CSS class selectors for this template.
  static String get settingGlyph => "setting-glyph";
  static String get dialog => "dialog";
  static String get dlgArea => "dlg-area";
  static String get dlgItem => "dlg-item";
  static String get dlgSubtitle => "dlg-subtitle";
  static String get expressionInput => "expression-input";

  // Injection functions:
  // Each functions:

  // With functions:

  // CSS for this template.
  static const String stylesheet = '''

.setting-glyph {
  position: absolute;
  left: 239px;
  top: 10px;
  background-image: url(settings.png);
  background-repeat: no-repeat;
  width: 2px;
  height: 20px;
  z-index: 1000;
  padding-left: 2px;
  padding-right: 2px;
  background-color: transparent;
  border-color: transparent;
  border-style: solid;
  border-top-width: 1px;
  border-left-width: 0px;
  border-right-width: 3px;
  border-bottom-width: 2px;
  cursor: pointer;
}

.setting-glyph:hover {
  background-color: #333 !important;
}

.dialog {
  font-size: 10pt;
  font-weight: normal;
  margin-top: 26px;
  position: absolute;
  left: 14px;
  background-color: #d3d3d3;
  width: 227px;
  padding-bottom: 3px;
  border: 1px solid #000;
}

.dlg-area {
  margin-left: 25px;
}

.dlg-item {
  margin-left: 10px;
  cursor: pointer;
}

.dlg-subtitle {
  font-weight: bold;
  margin-top: 3px;
}

.expression-input {
  width: 205px;
  font-family: arial;
  font-weight: bold;
}

  ''';

  // Stylesheet class selectors:
  String safeHTML(String html) {
    // TODO(terry): Escaping for XSS vulnerabilities TBD.
    return html;
  }
}
class TapeUI {
  Map<String, Object> _scopes;
  Element _fragment;


  // Elements bound to a variable:
  var tape;

  TapeUI() : _scopes = new Map<String, Object>() {
    // Insure stylesheet for template exist in the document.
    add_calcui_templatesStyles();

    _fragment = new DocumentFragment();
    tape = new Element.html('<div class="calculator-tape"></div>');
    _fragment.elements.add(tape);
  }

  String lineBreak() {
    return '''<div class="clear-line"></div>''';
  }

  String clearCalculation() {
    return '''<div class="clear-calc"></div>''';
  }

  String firstOp(displayedOp, number) {
    return '''<span style="font-family: courier;">${displayedOp}</span>${number}''';
  }

  String displayOpAndNumber(opAsStr, number) {
    return '''<div id="activeInput" class="alignleft"><span style="font-family: courier;">${opAsStr}</span>${number}</div>''';
  }

  String displayOp(opAsStr) {
    return '''<div id="activeInput" class="alignleft"><span style="font-family: courier;">${opAsStr}</span></div>''';
  }

  String displayEqual() {
    return '''<div class="alignleft"><span style="font-family: courier;">&nbsp;&nbsp;</span></div>''';
  }

  String alignSubTotal() {
    return '''<div class="alignright"></div>''';
  }

  String displayTotal(formattedTotal) {
    return '''<div class="alignright">= ${formattedTotal}</div>''';
  }

  String displayActiveTotal() {
    return '''<div id="activeTotal" class="alignright"></div>''';
  }

  String displayError(err) {
    return '''<div class="error">${err}</div>''';
  }

  String replaceActiveOp(value) {
    return '''<div class="alignleft">${value}</div>''';
  }

  String replaceActiveTotal(value) {
    return '''<div class="alignright">${value}</div>''';
  }

  Element get root => _fragment;

  // CSS class selectors for this template.
  static String get calculatorTape => "calculator-tape";
  static String get alignleft => "alignleft";
  static String get alignright => "alignright";
  static String get clearLine => "clear-line";
  static String get clearCalc => "clear-calc";
  static String get error => "error";
  static String get total => "total";

  // Injection functions:
  // Each functions:

  // With functions:

  // CSS for this template.
  static const String stylesheet = '''

.calculator-tape {
  height: 7em;
  font-size: 18pt;
  color: #000;
  background-color: #ffffff;
  width: 229px;
  border: 1px #000 solid;
  font-family: arial;
  font-weight: bolder;
  padding-right: 2px;
  padding-left: 8px;
  overflow-y: auto;
}

.calculator-tape::-webkit-scrollbar {
  width: 0px;
}

.alignleft {
  float: left;
}

.alignright {
  float: right;
  color: #d3d3d3;
  padding-right: 4px;
}

.clear-line {
  clear: both;
}

.clear-calc {
  clear: both;
  border-bottom: 1px solid #d3d3d3;
  margin-bottom: 5px;
  height: 5px;
}

.error {
  clear: both;
  background-color: #ff0000;
  color: #ffffff;
  margin-bottom: 5px;
  font-size: 10pt;
}

.total {
  clear: both;
  border-bottom: 1px solid #d3d3d3;
  margin-bottom: 5px;
}

  ''';

  // Stylesheet class selectors:
  String safeHTML(String html) {
    // TODO(terry): Escaping for XSS vulnerabilities TBD.
    return html;
  }
}


// Inject all templates stylesheet once into the head.
bool calcui_stylesheet_added = false;
void add_calcui_templatesStyles() {
  if (!calcui_stylesheet_added) {
    StringBuffer styles = new StringBuffer();

    // All templates stylesheet.
    styles.add(FlatPadUI.stylesheet);
    styles.add(ButtonPadUI.stylesheet);
    styles.add(SettingsDialog.stylesheet);
    styles.add(TapeUI.stylesheet);

    calcui_stylesheet_added = true;
    document.head.elements.add(new Element.html('<style>${styles.toString()}</style>'));
  }
}
