// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library calcui;

import 'dart:html';
import 'dart:math' as Math;

part 'tape.dart';
part 'settings.dart';

var padUI;          // Calculator Pad UI.

TapeUI tapeUI;      // Tape output area UI.
Tape tape;          // Controller.

Settings mySettings;

String currentRegister;
int currentOperator;
double total;

class FlatPadUI {
  Map<String, Object> _scopes;
  DocumentFragment _fragment;

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
    _fragment = new DocumentFragment();
    var e0 = new Element.html('<div class="calc-pad"></div>');
    _fragment.children.add(e0);
    var e1 = new Element.html('<div class="calc-row"></div>');
    e0.children.add(e1);
    keySeven = new Element.html('<span class="calc-num">7</span>');
    e1.children.add(keySeven);
    keyEight = new Element.html('<span class="calc-num">8</span>');
    e1.children.add(keyEight);
    keyNine = new Element.html('<span class="calc-num">9</span>');
    e1.children.add(keyNine);
    keyPlus = new Element.html('<div class="op-plus-base op-plus"></div>');
    e1.children.add(keyPlus);
    keyMinus = new Element.html('<div class="op-minus-base op-minus"></div>');
    e1.children.add(keyMinus);
    var e2 = new Element.html('<div class="calc-row"></div>');
    e0.children.add(e2);
    keyFour = new Element.html('<span class="calc-num">4</span>');
    e2.children.add(keyFour);
    keyFive = new Element.html('<span class="calc-num">5</span>');
    e2.children.add(keyFive);
    keySix = new Element.html('<span class="calc-num">6</span>');
    e2.children.add(keySix);
    keyStar = new Element.html('<div class="op-mult-base op-mult"></div>');
    e2.children.add(keyStar);
    keySlash = new Element.html('<div class="op-div-base op-div"></div>');
    e2.children.add(keySlash);
    var e3 = new Element.html('<div class="calc-row"></div>');
    e0.children.add(e3);
    keyOne = new Element.html('<span class="calc-num">1</span>');
    e3.children.add(keyOne);
    keyTwo = new Element.html('<span class="calc-num">2</span>');
    e3.children.add(keyTwo);
    keyThree = new Element.html('<span class="calc-num">3</span>');
    e3.children.add(keyThree);
    keyEqual = new Element.html('<div class="op-equal-base op-equal"></div>');
    e3.children.add(keyEqual);
    keyClear = new Element.html('<div class="op-arrow-base op-arrow"></div>');
    e3.children.add(keyClear);
    var e4 = new Element.html('<div class="calc-row"></div>');
    e0.children.add(e4);
    keyZero = new Element.html('<span class="calc-num">0</span>');
    e4.children.add(keyZero);
    keyDot = new Element.html('<span class="calc-period"></span>');
    e4.children.add(keyDot);
    var e5 = new Element.html(
        '<span style="font-size:9pt;">&nbsp;&nbsp;</span>');
    var e6 = e5.clone(true);
    keyDot.children.add(e5);
    keyDot.appendText('.');           // Append text as last child of keyDot.
    keyDot.children.add(e6);
  }

  Node get root => _fragment.children[0];

  // CSS class selectors for flat pad UI theme.
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
}

class ButtonPadUI {
  Map<String, Object> _scopes;
  DocumentFragment _fragment;

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
    _fragment = new DocumentFragment();
    var e0 = new Element.html('<div class="button-pad"></div>');
    _fragment.children.add(e0);
    var e1 = new Element.html('<div class="button-row"></div>');
    e0.children.add(e1);
    keySeven = new Element.html('<span class="button-num">7</span>');
    e1.children.add(keySeven);
    keyEight = new Element.html('<span class="button-num">8</span>');
    e1.children.add(keyEight);
    keyNine = new Element.html('<span class="button-num">9</span>');
    e1.children.add(keyNine);
    keyPlus = new Element.html(
        '<div class="buttonop-plus-base buttonop-plus"></div>');
    e1.children.add(keyPlus);
    keyMinus = new Element.html(
        '<div class="buttonop-minus-base buttonop-minus"></div>');
    e1.children.add(keyMinus);
    var e2 = new Element.html('<div class="button-row"></div>');
    e0.children.add(e2);
    keyFour = new Element.html('<span class="button-num">4</span>');
    e2.children.add(keyFour);
    keyFive = new Element.html('<span class="button-num">5</span>');
    e2.children.add(keyFive);
    keySix = new Element.html('<span class="button-num">6</span>');
    e2.children.add(keySix);
    keyStar = new Element.html(
        '<div class="buttonop-mult-base buttonop-mult"></div>');
    e2.children.add(keyStar);
    keySlash = new Element.html(
        '<div class="buttonop-div-base buttonop-div"></div>');
    e2.children.add(keySlash);
    var e3 = new Element.html('<div class="button-row"></div>');
    e0.children.add(e3);
    keyOne = new Element.html('<span class="button-num">1</span>');
    e3.children.add(keyOne);
    keyTwo = new Element.html('<span class="button-num">2</span>');
    e3.children.add(keyTwo);
    keyThree = new Element.html('<span class="button-num">3</span>');
    e3.children.add(keyThree);
    keyEqual = new Element.html(
        '<div class="buttonop-equal-base buttonop-equal"></div>');
    e3.children.add(keyEqual);
    keyClear = new Element.html(
        '<div class="buttonop-arrow-base buttonop-arrow"></div>');
    e3.children.add(keyClear);
    var e4 = new Element.html(
        '<div style="padding-top: 1px;" class="button-row"></div>');
    e0.children.add(e4);
    keyZero = new Element.html('<span class="button-num">0</span>');
    e4.children.add(keyZero);
    keyDot = new Element.html('<span class="button-period"></span>');
    e4.children.add(keyDot);
    var e5 = new Element.html(
        '<span style="font-size:9pt;">&nbsp;&nbsp;</span>');
    var e6 = e5.clone(true);
    keyDot.children.add(e5);
    keyDot.appendText('.');           // Append text as last child of keyDot.
    keyDot.children.add(e6);
  }

  Node get root => _fragment.children[0];

  // CSS class selectors for the button UI theme.
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
}

class SettingsDialog {
  Map<String, Object> _scopes;
  DocumentFragment _fragment;

  // Elements bound to a variable:
  var settings;
  var settingsDialog;
  var simpleTitle;
  var simple;
  var buttonTitle;
  var buttons;

  SettingsDialog() : _scopes = new Map<String, Object>() {
    _fragment = new DocumentFragment();
    var e0 = new DivElement();
    _fragment.children.add(e0);
    settings = new Element.html('<div class="setting-glyph"></div>');
    e0.children.add(settings);
    settingsDialog = new Element.html(
        '<div class="dialog" style="visibility: hidden;"></div>');
    e0.children.add(settingsDialog);
    var e1 = new Element.html('<div class="dlg-area"></div>');
    settingsDialog.children.add(e1);
    var e2 = new Element.html('<div class="dlg-subtitle">Theme:</div>');
    e1.children.add(e2);
    simpleTitle = new Element.html('<div style="cursor: pointer;"></div>');
    e1.children.add(simpleTitle);
    simple = new Element.html(
        '<input class="dlg-item" name="theme" type="radio"></input>');
    simpleTitle.children.add(simple);
    var e3 = new Element.html('<span>Simple</span>');
    simpleTitle.children.add(e3);
    buttonTitle = new Element.html('<div style="cursor: pointer;"></div>');
    e1.children.add(buttonTitle);
    buttons = new Element.html(
        '<input class="dlg-item" name="theme" type="radio"></input>');
    buttonTitle.children.add(buttons);
    var e4 = new Element.html('<span>Buttons</span>');
    buttonTitle.children.add(e4);
  }

  Node get root => _fragment.children[0];

  // CSS class selectors for the settings dialog.
  static String get settingGlyph => "setting-glyph";
  static String get dialog => "dialog";
  static String get dlgArea => "dlg-area";
  static String get dlgItem => "dlg-item";
  static String get dlgSubtitle => "dlg-subtitle";
  static String get expressionInput => "expression-input";
}

class TapeUI {
  Map<String, Object> _scopes;
  DocumentFragment _fragment;

  // Elements bound to a variable:
  var tape;

  TapeUI() : _scopes = new Map<String, Object>() {
    _fragment = new DocumentFragment();
    tape = new Element.html('<div class="calculator-tape"></div>');
    _fragment.children.add(tape);
  }

  String lineBreak() {
    return '<div class="clear-line"></div>';
  }

  String clearCalculation() {
    return '<div class="clear-calc"></div>';
  }

  String firstOp(displayedOp, number) {
    return '<span style="font-family: courier;">${displayedOp}</span>${number}';
  }

  String displayOpAndNumber(opAsStr, number) {
    return '<div id="activeInput" class="alignleft">'
             '<span style="font-family: courier;">${opAsStr}</span>'
           '${number}</div>';
  }

  String displayOp(opAsStr) {
    return '<div id="activeInput" class="alignleft">'
             '<span style="font-family: courier;">${opAsStr}</span>'
           '</div>';
  }

  String displayEqual() {
    return '<div class="alignleft">'
             '<span style="font-family: courier;">&nbsp;&nbsp;</span>'
           '</div>';
  }

  String alignSubTotal() {
    return '<div class="alignright"></div>';
  }

  String displayTotal(formattedTotal) {
    return '<div class="alignright">= ${formattedTotal}</div>';
  }

  String displayActiveTotal() {
    return '<div id="activeTotal" class="alignright"></div>';
  }

  String displayError(err) {
    return '<div class="error">${err}</div>';
  }

  String replaceActiveOp(value) {
    return '<div class="alignleft">${value}</div>';
  }

  String replaceActiveTotal(value) {
    return '<div class="alignright">${value}</div>';
  }

  Node get root => _fragment.children[0];

  // CSS class selectors for the tape UI.
  static String get calculatorTape => "calculator-tape";
  static String get alignleft => "alignleft";
  static String get alignright => "alignright";
  static String get clearLine => "clear-line";
  static String get clearCalc => "clear-calc";
  static String get error => "error";
  static String get total => "total";
}
