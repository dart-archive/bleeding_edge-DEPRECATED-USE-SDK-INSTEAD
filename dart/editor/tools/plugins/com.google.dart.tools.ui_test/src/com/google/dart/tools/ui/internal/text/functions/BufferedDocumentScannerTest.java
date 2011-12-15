/*
 * Copyright 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.functions;

import junit.framework.TestCase;

import org.eclipse.jface.text.Document;

public class BufferedDocumentScannerTest extends TestCase {
  public void test_BufferedDocumentScanner_peek() {
    BufferedDocumentScanner scanner = new BufferedDocumentScanner(10);
    String content = "1234567890abcdefghijklmnopqrstuvwxyz";
    int length = content.length();
    scanner.setRange(new Document(content), 0, length);
    for (int i = 0; i < length; i++) {
      assertEquals("Wrong value at " + i, content.charAt(i), scanner.peek(0));
      scanner.read();
    }
  }

  public void test_BufferedDocumentScanner_peek_crossBuffer() {
    int bufferLength = 10;
    int halfOffset = 3;
    BufferedDocumentScanner scanner = new BufferedDocumentScanner(bufferLength);
    String content = "1234567890abcdefghijklmnopqrstuvwxyz";
    int length = content.length();
    scanner.setRange(new Document(content), 0, length);
    for (int i = 0; i < bufferLength - halfOffset; i++) {
      scanner.read();
    }
    assertEquals("Wrong value at " + (bufferLength + halfOffset),
        content.charAt(bufferLength + halfOffset), scanner.peek(halfOffset + halfOffset));
  }
}
