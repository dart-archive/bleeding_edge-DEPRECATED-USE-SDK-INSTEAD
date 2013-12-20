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

package com.google.dart.engine.internal.html.angular;

import com.google.dart.engine.html.ast.XmlTagNode;

/**
 * {@link NgAnnotation} describes any <code>NgAnnotation</code> annotation instance.
 */
abstract class NgAnnotation {
  private final InjectSelector selector;

  public NgAnnotation(InjectSelector selector) {
    this.selector = selector;
  }

  /**
   * Applies this {@link NgAnnotation} to the resolver.
   * 
   * @param resolver the {@link AngularHtmlUnitResolver} to apply to, not {@code null}
   * @param node the {@link XmlTagNode} to apply within, not {@code null}
   */
  public abstract void apply(AngularHtmlUnitResolver resolver, XmlTagNode node);

  /**
   * @return the {@link InjectSelector} of this annotation.
   */
  public InjectSelector getSelector() {
    return selector;
  }
}
