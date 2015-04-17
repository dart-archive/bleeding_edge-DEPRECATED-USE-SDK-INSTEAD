// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library ssa;

import 'dart:collection';

import '../closure.dart';
import '../constant_system_dart.dart';
import '../constants/constant_system.dart';
import '../constants/expressions.dart';
import '../constants/values.dart';
import '../deferred_load.dart' show DeferredLoadTask;
import '../dart2jslib.dart' hide Selector, TypedSelector;
import '../dart_types.dart';
import '../elements/elements.dart';
import '../elements/modelx.dart'
    show ElementX,
         VariableElementX,
         ConstructorBodyElementX;
import '../helpers/helpers.dart';
import '../io/source_file.dart';
import '../io/source_information.dart';
import '../js/js.dart' as js;
import '../js_backend/js_backend.dart';
import '../js_emitter/js_emitter.dart' show CodeEmitterTask, NativeEmitter;
import '../native/native.dart' as native;
import '../resolution/operators.dart';
import '../scanner/scannerlib.dart'
    show PartialFunctionElement, Token, PLUS_TOKEN;
import '../tree/tree.dart' as ast;
import '../types/types.dart';
import '../types/constants.dart' show computeTypeMask;
import '../universe/universe.dart';
import '../util/util.dart';
import '../js/rewrite_async.dart';
import 'package:_internal/compiler/js_lib/shared/embedded_names.dart';

part 'builder.dart';
part 'codegen.dart';
part 'codegen_helpers.dart';
part 'interceptor_simplifier.dart';
part 'invoke_dynamic_specializers.dart';
part 'nodes.dart';
part 'optimize.dart';
part 'types.dart';
part 'types_propagation.dart';
part 'validate.dart';
part 'variable_allocator.dart';
part 'value_range_analyzer.dart';
part 'value_set.dart';
