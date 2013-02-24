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
package com.google.dart.engine.html.scanner;

/**
 * Instances of {@code HtmlScanResult} hold the result of scanning an HTML file.
 */
public class HtmlScanResult {

  /**
   * The first token in the token stream.
   */
  private final Token token;

  /**
   * The line start information that was produced.
   */
  private final int[] lineStarts;

  public HtmlScanResult(Token token, int[] lineStarts) {
    this.token = token;
    this.lineStarts = lineStarts;
  }

  /**
   * Answer the line start information that was produced.
   * 
   * @return an array of line starts (not {@code null})
   */
  public int[] getLineStarts() {
    return lineStarts;
  }

  /**
   * Answer the first token in the token stream.
   * 
   * @return the token (not {@code null})
   */
  public Token getToken() {
    return token;
  }
}
