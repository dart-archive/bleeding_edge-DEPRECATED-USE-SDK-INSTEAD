// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library survey.web.sruvey;

import 'dart:html';

import 'package:polymer/polymer.dart';
import 'package:survey/models.dart';

/**
 * The view for a survey element. Use this to add questions to the survey.
 */
@CustomTag('survey-element')
class SurveyElement extends PolymerElement with Observable {
  bool get applyAuthorStyles => true;
  @observable SurveyApp app = new SurveyApp();

  created() {
    super.created();
    app = appModel;
    appModel.questions = toObservable([]);
  }

  addNewQuestion(Event e, var detail, Element sender) {
    e.preventDefault();
    app.questions.add(new Question());
  }
}

