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
package com.google.dart.engine.internal.index;

import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.index.UniverseElement;
import com.google.dart.engine.internal.element.ElementImpl;

/**
 * Implementation of {@link UniverseElement}.
 * 
 * @coverage dart.engine.index
 */
public class UniverseElementImpl extends ElementImpl implements UniverseElement {
  public static final UniverseElementImpl INSTANCE = new UniverseElementImpl();

  private UniverseElementImpl() {
    super("--universe--", -1);
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    // Visitors currently can't visit this kind of node. To 'fix' this, we would need to add an
    // interface for this element kind.
    return null;
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.UNIVERSE;
  }
}
