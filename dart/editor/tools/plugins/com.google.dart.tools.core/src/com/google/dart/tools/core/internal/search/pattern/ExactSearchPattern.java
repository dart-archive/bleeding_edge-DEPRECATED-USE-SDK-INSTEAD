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
package com.google.dart.tools.core.internal.search.pattern;

import com.google.dart.tools.core.internal.util.CharOperation;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.search.MatchQuality;
import com.google.dart.tools.core.search.SearchPattern;

/**
 * Instances of the class <code>ExactSearchPattern</code> implement a search pattern that matches
 * elements whose name matches a specified identifier exactly.
 */
public class ExactSearchPattern implements SearchPattern {
  /**
   * The identifier that matching elements must be equal to.
   */
  private char[] identifier;

  /**
   * A flag indicating whether a case sensitive match is to be performed.
   */
  private boolean caseSensitive;

  /**
   * Initialize a newly created search pattern to match elements whose names begin with the given
   * prefix.
   * 
   * @param identifier the identifier that matching elements must be equal to
   * @param caseSensitive <code>true</code> if a case sensitive match is to be performed
   */
  public ExactSearchPattern(String identifier, boolean caseSensitive) {
    this.identifier = identifier.toCharArray();
    this.caseSensitive = caseSensitive;
  }

  @Override
  public MatchQuality matches(DartElement element) {
    String name = element.getElementName();
    if (name == null) {
      return null;
    }
    if (CharOperation.equals(identifier, name.toCharArray(), caseSensitive)) {
      return MatchQuality.EXACT;
    }
    return null;
  }
}
