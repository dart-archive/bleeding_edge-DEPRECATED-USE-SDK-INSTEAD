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
package com.google.dart.server.internal.remote.processor;

import com.google.dart.engine.error.ErrorCode;

import junit.framework.TestCase;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AnalysisErrorImplTest extends TestCase {
  public void test_new() throws Exception {
    ErrorCode errorCode = mock(ErrorCode.class);
    AnalysisErrorImpl error = new AnalysisErrorImpl(
        "my/file.dart",
        errorCode,
        10,
        20,
        "my message",
        "my correction");
    assertEquals("my/file.dart", error.getFile());
    assertSame(errorCode, error.getErrorCode());
    assertEquals(10, error.getOffset());
    assertEquals(20, error.getLength());
    assertEquals("my message", error.getMessage());
    assertEquals("my correction", error.getCorrection());
    assertThat(error.toString()).isNotEmpty().contains("my/file.dart").contains("10");
  }
}
