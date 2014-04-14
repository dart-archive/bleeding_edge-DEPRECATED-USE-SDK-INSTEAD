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
package com.google.dart.engine.internal.cache;

import com.google.dart.engine.source.Source;

/**
 * Instances of the class {@code DefaultRetentionPolicy} implement a retention policy that will keep
 * AST's in the cache if there is analysis information that needs to be computed for a source, where
 * the computation is dependent on having the AST.
 */
public class DefaultRetentionPolicy implements CacheRetentionPolicy {
  /**
   * An instance of this class that can be shared.
   */
  public static final DefaultRetentionPolicy POLICY = new DefaultRetentionPolicy();

  /**
   * Initialize a newly created retention policy.
   */
  public DefaultRetentionPolicy() {
    super();
  }

  @Override
  public RetentionPriority getAstPriority(Source source, SourceEntry sourceEntry) {
    if (sourceEntry instanceof DartEntry) {
      DartEntry dartEntry = (DartEntry) sourceEntry;
      if (astIsNeeded(dartEntry)) {
        return RetentionPriority.MEDIUM;
      }
    }
    return RetentionPriority.LOW;
  }

  /**
   * Return {@code true} if there is analysis information in the given entry that needs to be
   * computed, where the computation is dependent on having the AST.
   * 
   * @param dartEntry the entry being tested
   * @return {@code true} if there is analysis information that needs to be computed from the AST
   */
  protected boolean astIsNeeded(DartEntry dartEntry) {
    return dartEntry.hasInvalidData(DartEntry.HINTS)
        || dartEntry.hasInvalidData(DartEntry.VERIFICATION_ERRORS)
        || dartEntry.hasInvalidData(DartEntry.RESOLUTION_ERRORS);
  }
}
