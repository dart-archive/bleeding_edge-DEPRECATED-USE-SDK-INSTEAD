// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library survey.models;

import 'package:polymer/polymer.dart';

final appModel = new SurveyApp();

/*
 * The model for the SurveyApp.
 */
class SurveyApp extends ObservableBase {
  @observable String title = '';
  @observable String description = '';
  @observable List<Question> questions;

  SurveyApp();
}

/*
 * Model for a survey question. [answerOptions] is the list of options from
 * which the user picks one or more [answers].
 */
class Question extends ObservableBase {
  @observable String text;
  @observable String helpText;
  @observable List<String> answerOptions = [];
  @observable List<String> answers = [];

  Question([this.text = '', this.helpText = '']);

  bool get isValid => text.isNotEmpty;
}