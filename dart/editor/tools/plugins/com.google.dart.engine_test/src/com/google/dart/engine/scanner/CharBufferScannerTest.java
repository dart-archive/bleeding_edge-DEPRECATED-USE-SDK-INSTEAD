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

import com.google.dart.engine.error.AnalysisError;
import com.google.dart.engine.error.AnalysisErrorListener;

import java.io.IOException;
import java.nio.CharBuffer;

public class CharBufferScannerTest extends AbstractScannerTest {
  @Override
  protected Token scan(String source) {
    try {
      CharBuffer buffer = CharBuffer.wrap(source);
      CharBufferScanner scanner = new CharBufferScanner(null, buffer, new AnalysisErrorListener() {
        @Override
        public void onError(AnalysisError event) {
          // Ignore errors
        }
      });
      return scanner.tokenize();
    } catch (IOException exception) {
      fail("Unexpected exception: " + exception.getMessage());
      return null;
    }
  }
}
