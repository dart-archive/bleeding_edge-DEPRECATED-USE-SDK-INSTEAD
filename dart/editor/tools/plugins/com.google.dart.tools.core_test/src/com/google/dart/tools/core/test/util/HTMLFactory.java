/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.core.test.util;

/**
 * The class <code>HTMLFactory</code> defines utility methods to create HTML source with various
 * characteristics.
 */
public final class HTMLFactory {
  /**
   * Return valid HTML text that contains references to Dart scripts, JavaScript scripts produced by
   * dartc, JavaScript scripts that are not produced by dartc, and scripts of at least one other
   * type.
   */
  public static String allScriptTypes() {
    return "<html><head>" + "<script type=\"application/dart\" src=\"special.dart\"/>"
        + "<script type=\"text/javascript\" src=\"main.app.js\"/>"
        + "<script type=\"text/javascript\" src=\"other.js\"/>"
        + "<script type=\"text/ruby\" src=\"script.rb\"/>" + "</head><body></body></html>";
  }

  /**
   * Return valid HTML text that contains no script references of any kind.
   */
  public static String noScripts() {
    return "<html><head></head><body></body></html>";
  }

  /**
   * Prevent the creation of instances of this class.
   */
  private HTMLFactory() {

  }
}
