/*
 * Copyright (c) 2013, the Dart project authors.
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

package com.google.dart.engine.services.correction;

import com.google.dart.engine.error.ErrorCode;
import com.google.dart.engine.parser.ParserErrorCode;
import com.google.dart.engine.utilities.source.SourceRange;

import junit.framework.TestCase;

public class ProblemLocationTest extends TestCase {
  public void test_new() throws Exception {
    ErrorCode errorCode = ParserErrorCode.CONST_AND_FINAL;
    ProblemLocation problem = new ProblemLocation(errorCode, 1, 2, "msg");
    assertSame(errorCode, problem.getErrorCode());
    assertEquals(1, problem.getOffset());
    assertEquals(2, problem.getLength());
    assertEquals(new SourceRange(1, 2), problem.getRange());
    assertEquals("msg", problem.getMessage());
  }
}
