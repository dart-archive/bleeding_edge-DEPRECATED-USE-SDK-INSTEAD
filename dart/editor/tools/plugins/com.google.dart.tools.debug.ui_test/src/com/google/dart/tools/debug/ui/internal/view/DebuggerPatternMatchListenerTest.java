/*
 * Copyright (c) 2014, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.dart.tools.debug.ui.internal.view;

import junit.framework.TestCase;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DebuggerPatternMatchListenerTest extends TestCase {
  Pattern dartiumPattern1 = Pattern.compile(DebuggerPatternMatchListener.DARTIUM_PATTERN_1);
  Pattern dartiumPattern2 = Pattern.compile(DebuggerPatternMatchListener.DARTIUM_PATTERN_2);
  Pattern unitTestPattern = Pattern.compile(DebuggerPatternMatchListener.UNITTEST_PATTERN);

  public void testDartiumPattern1() {
    final String test1 = "(http://127.0.0.1:3030/Users/util/debuggertest/web_test.dart:33:14)";

    Matcher matcher = dartiumPattern1.matcher(test1);

    assertEquals(true, matcher.find());
    assertEquals("http://127.0.0.1:3030/Users/util/debuggertest/web_test.dart", matcher.group(1));
    assertEquals("33", matcher.group(2));
  }

  public void testDartiumPattern2() {
    final String test1 = "(file:///Users/devoncarew/projects/dart/dart/editor/util/debuggertest/cmd_test.dart:30:11)";

    Matcher matcher = dartiumPattern1.matcher(test1);

    assertEquals(true, matcher.find());
    assertEquals(
        "file:///Users/devoncarew/projects/dart/dart/editor/util/debuggertest/cmd_test.dart",
        matcher.group(1));
    assertEquals("30", matcher.group(2));
  }

  public void testDartiumPattern3() {
    final String test1 = "(web.dart:10)";

    Matcher matcher = dartiumPattern2.matcher(test1);

    assertEquals(true, matcher.find());
    assertEquals("web.dart", matcher.group(1));
    assertEquals("10", matcher.group(2));
  }

  public void testUnitTestPattern1() {
    final String test1 = "  cmd.dart 67:13                                        main.<fn>.<fn>";

    Matcher matcher = unitTestPattern.matcher(test1);

    assertEquals(true, matcher.find());
    assertEquals("cmd.dart", matcher.group(1));
    assertEquals("67", matcher.group(2));
  }

  public void testUnitTestPattern2() {
    final String test1 = "  package:unittest/src/test_case.dart 109:30            _run.<fn>";

    Matcher matcher = unitTestPattern.matcher(test1);

    assertEquals(true, matcher.find());
    assertEquals("package:unittest/src/test_case.dart", matcher.group(1));
    assertEquals("109", matcher.group(2));
  }

  public void testUnitTestPattern3() {
    final String test1 = "  dart:async/zone.dart 717                              _rootRunUnary";

    Matcher matcher = unitTestPattern.matcher(test1);

    assertEquals(true, matcher.find());
    assertEquals("dart:async/zone.dart", matcher.group(1));
    assertEquals("717", matcher.group(2));
  }

}
