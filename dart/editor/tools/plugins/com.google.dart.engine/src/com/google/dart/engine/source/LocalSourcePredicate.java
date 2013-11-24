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

package com.google.dart.engine.source;

/**
 * Instances of interface {@code LocalSourcePredicate} are used to determine if the given
 * {@link Source} is "local" in some sense, so can be updated.
 * 
 * @coverage dart.engine.source
 */
public interface LocalSourcePredicate {
  /**
   * Instance of {@link LocalSourcePredicate} that always returns {@code false}.
   */
  LocalSourcePredicate FALSE = new LocalSourcePredicate() {
    @Override
    public boolean isLocal(Source source) {
      return false;
    }
  };

  /**
   * Instance of {@link LocalSourcePredicate} that always returns {@code true}.
   */
  LocalSourcePredicate TRUE = new LocalSourcePredicate() {
    @Override
    public boolean isLocal(Source source) {
      return true;
    }
  };

  /**
   * Instance of {@link LocalSourcePredicate} that returns {@code true} for all {@link Source}s
   * except of SDK.
   */
  LocalSourcePredicate NOT_SDK = new LocalSourcePredicate() {
    @Override
    public boolean isLocal(Source source) {
      return source.getUriKind() != UriKind.DART_URI;
    }
  };

  /**
   * Determines if the given {@link Source} is local.
   * 
   * @param source the {@link Source} to analyze
   * @return {@code true} if the given {@link Source} is local
   */
  boolean isLocal(Source source);
}
