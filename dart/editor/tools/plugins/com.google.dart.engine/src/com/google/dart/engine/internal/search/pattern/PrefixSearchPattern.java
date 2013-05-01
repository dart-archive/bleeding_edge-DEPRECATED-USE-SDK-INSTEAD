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
package com.google.dart.engine.internal.search.pattern;

import com.google.dart.engine.element.Element;
import com.google.dart.engine.search.MatchQuality;
import com.google.dart.engine.search.SearchPattern;

import static org.apache.commons.lang3.StringUtils.startsWith;
import static org.apache.commons.lang3.StringUtils.startsWithIgnoreCase;

/**
 * Instances of the class <code>PrefixSearchPattern</code> implement a search pattern that matches
 * elements whose name has a given prefix.
 * 
 * @coverage dart.engine.search
 */
public class PrefixSearchPattern implements SearchPattern {
  /**
   * The prefix that matching elements must start with.
   */
  private final String prefix;

  /**
   * A flag indicating whether a case sensitive match is to be performed.
   */
  private final boolean caseSensitive;

  /**
   * Initialize a newly created search pattern to match elements whose names begin with the given
   * prefix.
   * 
   * @param prefix the prefix that matching elements must start with
   * @param caseSensitive {@code true} if a case sensitive match is to be performed
   */
  public PrefixSearchPattern(String prefix, boolean caseSensitive) {
    this.prefix = prefix;
    this.caseSensitive = caseSensitive;
  }

  @Override
  public MatchQuality matches(Element element) {
    if (element == null) {
      return null;
    }
    String name = element.getDisplayName();
    if (name == null) {
      return null;
    }
    if (caseSensitive && startsWith(name, prefix)) {
      return MatchQuality.EXACT;
    }
    if (!caseSensitive && startsWithIgnoreCase(name, prefix)) {
      return MatchQuality.EXACT;
    }
    return null;
  }
}
