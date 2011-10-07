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
package com.google.dart.tools.core.internal.builder;

import com.google.dart.compiler.DartArtifactProvider;
import com.google.dart.compiler.Source;

import java.io.IOException;
import java.io.Reader;

/**
 * A read-through caching implementation of {@link DartArtifactProvider} that pulls existing content
 * from a parent artifact provider but caches new content in memory.
 */
public class LocalArtifactProvider extends CachingArtifactProvider {

  /**
   * The parent artifact provider (not <code>null</code>)
   */
  private final DartArtifactProvider parent;

  /**
   * Construct a new instance with the specified parent
   * 
   * @param parent the parent artifact provider (not <code>null</code>)
   */
  public LocalArtifactProvider(DartArtifactProvider parent) {
    if (parent == null) {
      throw new IllegalArgumentException();
    }
    this.parent = parent;
  }

  /**
   * If content is cached locally, then return a reader for that content, otherwise defer to the
   * parent artifact provider.
   */
  @Override
  public Reader getArtifactReader(Source source, String part, String extension) throws IOException {
    Reader reader = super.getArtifactReader(source, part, extension);
    if (reader != null) {
      return reader;
    }
    return parent.getArtifactReader(source, part, extension);
  }

  /**
   * If content for this extension has been cached locally, then assume that content is up to date.
   * Otherwise defer to the parent artifact provider.
   */
  @Override
  public boolean isOutOfDate(Source source, Source base, String extension) {
    if (!super.isOutOfDate(source, base, extension)) {
      return false;
    }
    return isOutOfDateInParent(source, base, extension);
  }

  /**
   * Call the parent artifact provider to determine if the content is up to date
   */
  protected boolean isOutOfDateInParent(Source source, Source base, String extension) {
    return parent.isOutOfDate(source, base, extension);
  }
}
