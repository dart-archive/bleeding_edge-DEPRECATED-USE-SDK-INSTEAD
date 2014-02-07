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

package com.google.dart.engine.internal.element.angular;

import com.google.dart.engine.element.ElementKind;
import com.google.dart.engine.element.ElementVisitor;
import com.google.dart.engine.element.angular.AngularViewElement;
import com.google.dart.engine.source.Source;

/**
 * Implementation of {@code AngularViewElement}.
 * 
 * @coverage dart.engine.element
 */
public class AngularViewElementImpl extends AngularElementImpl implements AngularViewElement {
  /**
   * The HTML template URI.
   */
  private final String templateUri;

  /**
   * The offset of the {@link #templateUri} in the {@link #getSource()}.
   */
  private final int templateUriOffset;

  /**
   * The HTML template source.
   */
  private Source templateSource;

  /**
   * Initialize a newly created Angular view.
   */
  public AngularViewElementImpl(String templateUri, int templateUriOffset) {
    super(null, -1);
    this.templateUri = templateUri;
    this.templateUriOffset = templateUriOffset;
  }

  @Override
  public <R> R accept(ElementVisitor<R> visitor) {
    return visitor.visitAngularViewElement(this);
  }

  @Override
  public ElementKind getKind() {
    return ElementKind.ANGULAR_VIEW;
  }

  @Override
  public Source getTemplateSource() {
    return templateSource;
  }

  @Override
  public String getTemplateUri() {
    return templateUri;
  }

  @Override
  public int getTemplateUriOffset() {
    return templateUriOffset;
  }

  /**
   * Set the HTML template source.
   * 
   * @param templateSource the template source to set
   */
  public void setTemplateSource(Source templateSource) {
    this.templateSource = templateSource;
  }

  @Override
  protected String getIdentifier() {
    return "AngularView@" + templateUriOffset;
  }
}
