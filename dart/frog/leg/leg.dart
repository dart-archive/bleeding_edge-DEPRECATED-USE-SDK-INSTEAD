// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('leg');

#import('io/io.dart', prefix: 'io');

#import('elements/elements.dart');
#import('scanner/scannerlib.dart');
#import('scanner/scanner_implementation.dart');
#import('ssa/ssa.dart');
#import('tree/tree.dart');
#import('util/util.dart');

#source('compiler.dart');
#source('resolver.dart');
#source('script.dart');
#source('tree_validator.dart');
#source('typechecker.dart');
#source('universe.dart');
#source('warnings.dart');

final bool GENERATE_SSA_TRACE = false;

void unreachable() {
  throw const Exception("Internal Error (Leg): UNREACHABLE");
}
