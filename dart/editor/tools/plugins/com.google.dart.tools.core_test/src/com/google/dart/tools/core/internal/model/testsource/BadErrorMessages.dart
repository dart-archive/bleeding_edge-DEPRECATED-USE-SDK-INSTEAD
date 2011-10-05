// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

/*
 * A collection of code snippets that cause the parser to crash.
 * It should generate an error message along the lines indicated
 * in the comments. Currently, the parser dies with an assertion
 * failure.
 */
class Test002 { Test002 this; int sample; } // this; is illegal
class A { static int x = 1; } // static fields may not be initialized unless const
