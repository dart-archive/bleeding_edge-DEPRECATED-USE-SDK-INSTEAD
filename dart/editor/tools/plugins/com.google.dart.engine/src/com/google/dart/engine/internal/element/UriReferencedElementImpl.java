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
package com.google.dart.engine.internal.element;

import com.google.dart.engine.element.UriReferencedElement;

/**
 * Instances of the class {@code UriReferencedElementImpl} implement an {@link UriReferencedElement}
 * .
 * 
 * @coverage dart.engine.element
 */
public abstract class UriReferencedElementImpl extends ElementImpl implements UriReferencedElement {
  /**
   * The offset of the URI in the file, may be {@code -1} if synthetic.
   */
  private int uriOffset = -1;

  /**
   * The offset of the character immediately following the last character of this node's URI, may be
   * {@code -1} if synthetic.
   */
  private int uriEnd = -1;

  /**
   * The URI that is specified by this directive.
   */
  private String uri;

  /**
   * Initialize a newly created import element.
   * 
   * @param name the name of this element
   * @param offset the directive offset, may be {@code -1} if synthetic.
   */
  public UriReferencedElementImpl(String name, int offset) {
    super(name, offset);
  }

  @Override
  public String getUri() {
    return uri;
  }

  @Override
  public int getUriEnd() {
    return uriEnd;
  }

  @Override
  public int getUriOffset() {
    return uriOffset;
  }

  /**
   * Set the URI that is specified by this directive.
   * 
   * @param uri the URI that is specified by this directive.
   */
  public void setUri(String uri) {
    this.uri = uri;
  }

  /**
   * Set the the offset of the character immediately following the last character of this node's
   * URI. {@code -1} for synthetic import.
   */
  public void setUriEnd(int uriEnd) {
    this.uriEnd = uriEnd;
  }

  /**
   * Sets the offset of the URI in the file.
   */
  public void setUriOffset(int uriOffset) {
    this.uriOffset = uriOffset;
  }
}
