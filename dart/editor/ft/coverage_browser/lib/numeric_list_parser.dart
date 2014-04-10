// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/// Parser combinator to recognize lists of numbers
/// and create an equivalent dart object.

library numlist;

import 'package:petitparser/petitparser.dart';

/// Parse the [string] and return a list of numbers represented by it.
List<num> parseNumericList(String string) {
  var parser = new _NumericListParser();
  var data = parser.parse(string);
  if (data.isSuccess) {
    List<num> list = data.value;
    return list;
  }
  throw new Exception("Invalid list of num '$string'");
}

/**
 * Defines the grammar for a simple language that recognizes
 * lists of numbers: [1, 2.3, -7, 3e4]
 */
class _NumericListGrammar extends CompositeParser {

  @override
  void initialize() {

    def('start', ref('array').end());

    def('array',
      char('[').trim()
        .seq(ref('elements').optional())
        .seq(char(']').trim()));

    def('elements',
      ref('value').separatedBy(char(',').trim(), includeSeparators: false));

    def('value',
      ref('numberToken'));

    def('numberToken', ref('numberPrimitive').flatten().trim());

    def('numberPrimitive',
      char('-').optional()
        .seq(char('0').or(digit().plus()))
        .seq(char('.').seq(digit().plus()).optional())
        .seq(anyIn('eE').seq(anyIn('-+').optional()).seq(digit().plus()).optional()));
  }
}

/**
 * Defines actions to perform that create lists of numbers when
 * corresponding grammar rules fire.
 */
class _NumericListParser extends _NumericListGrammar {

  @override
  void initialize() {
    super.initialize();

    action('array', (each) => each[1] != null ? each[1] : new List());

    action('numberToken', (each) {
      var floating = double.parse(each);
      var integral = floating.toInt();
      if (floating == integral && each.indexOf('.') == -1) {
        return integral;
      } else {
        return floating;
      }
    });
  }
}
