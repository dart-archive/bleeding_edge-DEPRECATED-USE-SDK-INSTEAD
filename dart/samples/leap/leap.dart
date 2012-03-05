// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('leap');

#import('dart:isolate');

#import('dart:dom', prefix: 'dom');
#import('file_system_http.dart');
#import('../../lib/uri/uri.dart');
#import('../../frog/lang.dart', prefix: 'frog');
#import('../../frog/leg/elements/elements.dart');
#import('../../frog/leg/leg.dart');
#import('../../frog/leg/tree/tree.dart');
#import('../../lib/uri/uri.dart');

#source('leap_leg.dart');
#source('leap_script.dart');
