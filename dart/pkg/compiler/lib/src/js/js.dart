// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library js;

import 'precedence.dart';
import '../util/characters.dart' as charCodes;
import '../util/util.dart';
import '../io/code_output.dart' show CodeBuffer;
import '../io/source_information.dart' show SourceInformation;
import '../js_emitter/js_emitter.dart' show USE_NEW_EMITTER;

// TODO(floitsch): remove this dependency (currently necessary for the
// CodeBuffer).
import '../dart2jslib.dart' as leg;

import '../dump_info.dart';

part 'nodes.dart';
part 'builder.dart';
part 'printer.dart';
part 'template.dart';
