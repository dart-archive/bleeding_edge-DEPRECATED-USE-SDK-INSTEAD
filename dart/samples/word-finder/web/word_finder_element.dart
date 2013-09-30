// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library word_finder.web.word_finder_element;

import 'dart:html';
import 'dart:async';
import 'package:polymer/polymer.dart';

@CustomTag('word-finder-element')
class WordFinderElement extends PolymerElement with ObservableMixin {
  bool get applyAuthorStyles => true;

  @observable List<String> charsList = toObservable(
      ['A', 'E', 'L', 'S', 'T', '']);

  List<String> possibleWords = toObservable([
      'LEAST', 'SETAL', 'SLATE', 'STALE', 'STEAL', 'STELA', 'TAELS', 'TALES',
      'TEALS', 'TESLA', 'AE', 'AL', 'AS', 'AT', 'EL', 'ES', 'ET', 'LA', 'TA',
      'ALE', 'ALS', 'ALT', 'ATE', 'EAT', 'ELS', 'ETA', 'LAS', 'LAT', 'LEA',
      'LES', 'LET', 'SAE', 'SAL', 'SAT', 'SEA', 'SEL', 'SET', 'TAE', 'TAS',
      'TEA', 'TEL', 'ALES', 'ALTS', 'ATES', 'EAST', 'EATS', 'ETAS', 'LASE',
      'LAST', 'LATE', 'LATS', 'LEAS', 'LEST', 'LETS', 'SALE', 'SALT', 'SATE',
      'SEAL', 'SEAT', 'SETA', 'SLAT', 'TAEL', 'TALE', 'TEAL', 'TEAS', 'TELA',
      'TELS']);

  List<String> formedWords = [];

  @observable int score = 0;
  @observable int maxScore = 0;
  int sourceElementIndex;
  int targetElementIndex;

  void created() {
    super.created();
    for (var word in possibleWords) {
      maxScore += word.length;
    }
  }

  void dragStartHandler(MouseEvent e, var detail, Node sender) {
    sourceElementIndex = charsList.indexOf(sender.text.trim());
    (sender as DivElement).classes.add('dashed');
  }

  void dragEnterHandler(MouseEvent e, var detail, Node sender) {
    (e.target as DivElement).classes.add('over');
  }

  void dragOverHandler(MouseEvent e, var detail, Node sender) {
    e.preventDefault();
    e.dataTransfer.dropEffect = 'move';
  }

  void dragLeaveHandler(MouseEvent e, var detail, Node sender) {
    (e.target as DivElement).classes.remove('over');
  }

  void dropHandler(MouseEvent e, var detail, Node sender) {
    e.preventDefault();
    targetElementIndex = charsList.indexOf(sender.text.trim());

    _swapChars();

    new Timer(new Duration(milliseconds: 125), () {
      _findWords();
   });
  }

  void dragEndHandler(MouseEvent e, var detail, Node sender) {
    e.preventDefault();
    e.stopPropagation();
  }

  /*
   * Checks if continous characters make a word. If they do, note the indices
   * of characters that are part of a word, and update the UI.
   */
  void _findWords() {
    var wordIndices = [];
    var wordChars = [];

    var root = getShadowRoot('word-finder-element');
    var charDivs = root.queryAll('.char');

    for (var i = 0; i < charsList.length; i++) {
      if (charsList[i].isNotEmpty) {
        wordIndices.add(i);
        wordChars.add(charsList[i]);
      }

      if (charsList[i].isEmpty || i == charsList.length - 1) {
        var word = wordChars.join();
        if (possibleWords.contains(word) && !formedWords.contains(word)) {
          formedWords.add(word);
          score += word.length;
          for (var i = 0; i < wordIndices.length; i++) {
            charDivs[i].classes.add('word');
          }
        }
        wordIndices = [];
        wordChars = [];
      }
    }
  }

  void _swapChars() {
    var temp = charsList[sourceElementIndex];
    charsList[sourceElementIndex] = charsList[targetElementIndex];
    charsList[targetElementIndex] = temp;
  }
}