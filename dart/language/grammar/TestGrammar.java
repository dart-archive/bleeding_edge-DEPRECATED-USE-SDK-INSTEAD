// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

package com.google.dart.antlr;

import org.antlr.runtime.ANTLRFileStream;
import org.antlr.runtime.CharStream;
import org.antlr.runtime.CommonTokenStream;

/**
 * Test rig for testing the Dart grammar.
 */
public class TestGrammar {
  public static void main(String... arguments) throws Exception {
    boolean hasErrors = false;
    if (arguments.length == 0) {
      throw new AssertionError("No files given on command line");
    }
    for (String argument : arguments) {
      CharStream input = new ANTLRFileStream(argument, "UTF8");
      DartLexer lexer = new DartLexer(input);
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      DartParser parser = new DartParser(tokens);
      if (argument.endsWith(".lib")) {
        parser.libraryUnit();
      } else if (argument.endsWith(".dart")) {
        parser.compilationUnit();
      } else {
        throw new AssertionError("Unknown file type: " + argument);
      }
      hasErrors |= lexer.hasErrors;
      hasErrors |= parser.hasErrors;
    }
    if (hasErrors) {
      throw new AssertionError("Parse errors");
    }
  }
}
