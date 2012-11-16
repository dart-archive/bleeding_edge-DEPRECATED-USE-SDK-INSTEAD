/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.engine.scanner;

import com.google.dart.engine.error.GatheringErrorListener;
import com.google.dart.engine.source.TestSource;

public class StringScannerTest extends AbstractScannerTest {
  public void test_offsetDelta() {
    int offsetDelta = 42;
    GatheringErrorListener listener = new GatheringErrorListener();
    StringScanner scanner = new StringScanner(null, offsetDelta, "a", listener);
    Token result = scanner.tokenize();
    assertNotNull(result);
    assertEquals(offsetDelta, result.getOffset());
  }

  @Override
  protected Token scan(String source, GatheringErrorListener listener) {
    StringScanner scanner = new StringScanner(null, source, listener);
    Token result = scanner.tokenize();
    listener.setLineInfo(new TestSource(), scanner.getLineStarts());
    return result;
  }
}
