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

package com.google.dart.server.internal.local;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.dart.engine.source.Source;
import com.google.dart.server.SourceSet;
import com.google.dart.server.SourceSetKind;

import java.util.Set;

/**
 * A {@link Source} provider and predicate based on {@link SourceSet}.
 * 
 * @coverage dart.server.local
 */
public class SourceSetBaseProvider {
  private final SourceSet set;
  private final Set<Source> listedSources;
  private final Set<Source> knownSources;
  private final Set<Source> addedSources;

  /**
   * Initialize a newly created {@link SourceSetBaseProvider}.
   * 
   * @param set the {@link SourceSet} on which this provider is based
   * @param knownSources the <em>mutable</em> set of all known {@link Source}s
   * @param addedSources the <em>mutable</em> set of all the {@link Source}s which were added
   *          directly
   */
  public SourceSetBaseProvider(SourceSet set, Set<Source> knownSources, Set<Source> addedSources) {
    this.set = set;
    this.listedSources = set.getKind() == SourceSetKind.LIST
        ? ImmutableSet.copyOf(set.getSources()) : ImmutableSet.<Source> of();
    this.knownSources = knownSources;
    this.addedSources = addedSources;
  }

  /**
   * Checks if the given {@link Source} satisfies this predicate.
   */
  public boolean apply(Source source) {
    switch (set.getKind()) {
      case ALL:
        return true;
      case EXPLICITLY_ADDED:
        return addedSources.contains(source);
      case LIST:
        return listedSources.contains(source);
      case NON_SDK:
      default:
        return !source.isInSystemLibrary();
    }
  }

  /**
   * Returns {@link Source}s which have not been allowed by the given old provider, but are allowed
   * by this one.
   */
  public Set<Source> computeNewSources(SourceSetBaseProvider oldProvider) {
    Set<Source> result = Sets.newHashSet();
    for (Source source : knownSources) {
      if (apply(source) && (oldProvider == null || !oldProvider.apply(source))) {
        result.add(source);
      }
    }
    return result;
  }
}
