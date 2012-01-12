// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("compiler_util");

#import("../../frog/lang.dart");

/** Slightly friendlier interface to SourceSpan */
class SpanHelper {
  static int startLine(SourceSpan span) {
    return span.file.getLine(span.start);
  }
  
  static int startCol(SourceSpan span) {
    return span.file.getColumn(span.file.getLine(span.start), span.start);
  }
  
  static int endLine(SourceSpan span) {
    return span.file.getLine(span.end);
  }
  
  static int endCol(SourceSpan span) {    
    return span.file.getColumn(span.file.getLine(span.end), span.end);
  }    
}

