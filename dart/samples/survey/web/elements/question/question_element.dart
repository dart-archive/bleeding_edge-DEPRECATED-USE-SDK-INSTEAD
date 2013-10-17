// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library survey.web.question;

import 'dart:html';

import 'package:polymer/polymer.dart';
import 'package:survey/models.dart';

/*
 *  The QuestionElement view. Use this to set the question text, optionally
 *  provide answer options to the user, and pick the widget that the user sees.
 */
@CustomTag('question-element')
class QuestionElement extends PolymerElement with Observable {
  static const String TEXTFIELD_OPT = 'Use a text field';
  static const String ONE_FROM_MANY_OPT = 'Select one from many options';
  static const String MANY_FROM_MANY_OPT = 'Select many from many options';

  static const OPTIONS_MISSING_MESSAGE = "You didn't add any options";
  static const QUESTION_MISSING_MESSAGE = 'You forgot to add the question text';
  static const DELETE_CONFIRM_MESSAGE = 'Are you sure you want to delete '
                                        'this question?';

  // The getters are needed because static consts cannot be used in templates.
  String get textfield_opt => TEXTFIELD_OPT;
  String get one_from_many_opt => ONE_FROM_MANY_OPT;
  String get many_from_many_opt => MANY_FROM_MANY_OPT;

  bool get applyAuthorStyles => true;

  @observable Question question = new Question();
  @observable bool editing = true;
  @observable String errorMessage = '';
  @observable List<String> widgetOptions = [TEXTFIELD_OPT, ONE_FROM_MANY_OPT,
                                            MANY_FROM_MANY_OPT];
  @observable int widgetSelectedIndex = 0;
  @observable List<String> optionInputs = toObservable(['']);

  @observable String get widgetSelection => widgetOptions[widgetSelectedIndex];

  @observable bool get usingTextWidget => widgetSelectedIndex == 0;

  QuestionElement() {
    new PathObserver(this, 'widgetSelectedIndex').changes.listen((_) {
      notifyProperty(this, #widgetSelection);
    });
  }

  edit() {
    editing = true;
    errorMessage = '';
    optionInputs.add('');
  }

  addEmptyInput(Event e, var detail, Element sender) {
    e.preventDefault();
    optionInputs.add('');
  }

  void getInputValues() {
    var root = getShadowRoot('question-element');
    var ul = root.query('#input-container');
    var inputs = ul.queryAll('input');

    optionInputs.clear();
    inputs.forEach((input) {
      var val = input.value.trim();
      if (val.isNotEmpty) {
        optionInputs.add(val);
      }
    });
  }

  bool validate() {
    bool valid = true;
    if (question.isValid) {
      if (!usingTextWidget && optionInputs.length == 0) {
        errorMessage = OPTIONS_MISSING_MESSAGE;
        valid = false;
      }
    } else {
      errorMessage = QUESTION_MISSING_MESSAGE;
      valid = false;
    }
    return valid;
  }

  show(Event e, var detail, Element sender) {
    e.preventDefault();
    if (!usingTextWidget) {
      getInputValues();
    }

    if (validate()) {
      question.answerOptions = optionInputs.where((opt) {
        return opt.isNotEmpty;
      }).toList();
      editing = false;
    } else {
      optionInputs.add('');
    }
  }

  delete(Event e, var detail, Element sender) {
    if (window.confirm(DELETE_CONFIRM_MESSAGE)) {
      appModel.questions.remove(question);
    }
  }

  setSelectionAnswers(Event e, var detail, Element sender) {
    e.preventDefault();
    question.answers = detail;
  }

  setTextAnswer(Event e, var detail, var sender) {
    e.preventDefault();
    question.answers = [sender.value.trim()];
  }
}