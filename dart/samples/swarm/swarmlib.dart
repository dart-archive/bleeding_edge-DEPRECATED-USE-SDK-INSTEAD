// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("swarmlib");

#import('dart:coreimpl');
#import('dart:json');
#import('dart:html');
#import('dart:math', prefix: 'Math');
#import('../ui_lib/base/base.dart');
#import('../ui_lib/view/view.dart');
#import('../ui_lib/observable/observable.dart');
#import('../ui_lib/touch/touch.dart');
#import('../ui_lib/util/utilslib.dart');

#source('App.dart');
#source('BiIterator.dart');
#source('ConfigHintDialog.dart');
#source('HelpDialog.dart');
#source('SwarmState.dart');
#source('SwarmViews.dart');
#source('SwarmApp.dart');
#source('DataSource.dart');
#source('Decoder.dart');
#source('UIState.dart');
#source('Views.dart');
#source('CSS.dart');

// TODO(jimhug): Remove this when deploying.
#source('CannedData.dart');
