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
package com.google.dart.engine.html.scanner;

import com.google.dart.engine.source.Source;

import java.nio.CharBuffer;

/**
 * Instances of {@code HtmlScanner} receive and scan HTML content from a {@link Source}.<br/>
 * For example, the following code scans HTML source and returns the result:
 * 
 * <pre>
 *   HtmlScanner scanner = new HtmlScanner(source);
 *   source.getContents(scanner);
 *   return scanner.getResult();
 * </pre>
 * 
 * @coverage dart.engine.html
 */
public class HtmlScanner implements Source.ContentReceiver {
  private final String[] SCRIPT_TAG = new String[] {"script"};

  /**
   * The source being scanned (not {@code null})
   */
  private final Source source;

  /**
   * The time at which the contents of the source were last set.
   */
  private long modificationTime;

  /**
   * The scanner used to scan the source
   */
  private AbstractScanner scanner;

  /**
   * The first token in the token stream.
   */
  private Token token;

  /**
   * Construct a new instance to scan the specified source.
   * 
   * @param source the source to be scanned (not {@code null})
   */
  public HtmlScanner(Source source) {
    this.source = source;
  }

  @Override
  public void accept(CharBuffer contents, long modificationTime) {
    this.modificationTime = modificationTime;
    scanner = new CharBufferScanner(source, contents);
    scanner.setPassThroughElements(SCRIPT_TAG);
    token = scanner.tokenize();
  }

  @Override
  public void accept(String contents, long modificationTime) {
    this.modificationTime = modificationTime;
    scanner = new StringScanner(source, contents);
    scanner.setPassThroughElements(SCRIPT_TAG);
    token = scanner.tokenize();
  }

  /**
   * Answer the result of scanning the source
   * 
   * @return the result (not {@code null})
   */
  public HtmlScanResult getResult() {
    return new HtmlScanResult(modificationTime, token, scanner.getLineStarts());
  }
}
