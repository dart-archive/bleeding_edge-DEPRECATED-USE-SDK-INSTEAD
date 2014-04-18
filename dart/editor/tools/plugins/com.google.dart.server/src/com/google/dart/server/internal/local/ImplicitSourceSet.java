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

import com.google.dart.engine.source.Source;
import com.google.dart.engine.utilities.translation.DartName;
import com.google.dart.server.SourceSet;
import com.google.dart.server.SourceSetKind;

/**
 * An implementation of {@link SourceSet} for some {@link SourceSetKind}.
 * 
 * @coverage dart.server.local
 */
@DartName("_ImplicitSourceSet")
public class ImplicitSourceSet implements SourceSet {
  private final SourceSetKind kind;

  public ImplicitSourceSet(SourceSetKind kind) {
    this.kind = kind;
  }

  @Override
  public SourceSetKind getKind() {
    return kind;
  }

  @Override
  public Source[] getSources() {
    return Source.EMPTY_ARRAY;
  }

  @Override
  public String toString() {
    return kind.toString();
  }
}
