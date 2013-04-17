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
package com.google.dart.tools.core.internal.analysis.model;

import com.google.dart.engine.source.Source;
import com.google.dart.engine.source.SourceContainer;

/**
 * Instances of the class {@link InvertedSourceContainer} represent a source container that contains
 * all source containers not in a given directory.
 */
public class InvertedSourceContainer implements SourceContainer {

  private final CompositeSourceContainer container;

  public InvertedSourceContainer(CompositeSourceContainer container) {
    this.container = container;
  }

  @Override
  public boolean contains(Source source) {
    return !container.contains(source);
  }

}
