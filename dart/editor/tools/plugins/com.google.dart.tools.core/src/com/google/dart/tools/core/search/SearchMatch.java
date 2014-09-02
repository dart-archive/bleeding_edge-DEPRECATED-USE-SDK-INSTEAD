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
package com.google.dart.tools.core.search;

import com.google.dart.engine.utilities.source.SourceRange;

/**
 * Instances of the class <code>SearchMatch</code> represent a match found by a search engine.
 * 
 * @coverage dart.tools.core.search
 */
public class SearchMatch {
  /**
   * The quality of the match.
   */
  private MatchQuality quality;

  /**
   * The kind of the match.
   */
  private MatchKind kind;

  /**
   * The source range that was matched.
   */
  private SourceRange sourceRange;

  /**
   * Is <code>true</code> if field or method access is done using qualifier.
   */
  private boolean qualified;

  /**
   * Is not <code>null</code> if matched element was imported with this prefix.
   */
  private String importPrefix;

  /**
   * @return the import prefix used to import this match of element, may be <code>null</code>.
   */
  public String getImportPrefix() {
    return importPrefix;
  }

  /**
   * Return the kind of the match. The kind is only used with reference matches and is an indication
   * of the kind of reference that was found.
   * 
   * @return the kind of the match
   */
  public MatchKind getKind() {
    return kind;
  }

  /**
   * Return the quality of the match. The quality is an indication of how closely the match matches
   * the original search criteria.
   * 
   * @return the quality of the match
   */
  public MatchQuality getQuality() {
    return quality;
  }

  /**
   * Return the source range that was matched.
   * 
   * @return the source range that was matched
   */
  public SourceRange getSourceRange() {
    return sourceRange;
  }

  /**
   * Return the quality of the match. The quality is an indication of how closely the match matches
   * the original search criteria.
   * 
   * @return the quality of the match
   * @deprecated use getQuality()
   */
  @Deprecated
  public MatchQuality getType() {
    return quality;
  }

  /**
   * @return the <code>true</code> if field or method access is done using qualifier.
   */
  public boolean isQualified() {
    return qualified;
  }

  /**
   * @see #getImportPrefix()
   */
  public void setImportPrefix(String importPrefix) {
    this.importPrefix = importPrefix;
  }

  /**
   * Specifies if field or method access is done using qualifier.
   */
  public void setQualified(boolean qualified) {
    this.qualified = qualified;
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("SearchMatch(kind="); //$NON-NLS-1$
    builder.append(kind);
    builder.append(", quality="); //$NON-NLS-1$
    builder.append(quality);
    builder.append(", range="); //$NON-NLS-1$
    builder.append(sourceRange);
    builder.append(", qualified="); //$NON-NLS-1$
    builder.append(qualified);
    builder.append(")"); //$NON-NLS-1$
    return builder.toString();
  }
}
