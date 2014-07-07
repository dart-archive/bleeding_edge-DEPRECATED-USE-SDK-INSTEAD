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
package com.google.dart.server.internal;

import com.google.dart.server.HoverInformation;

/**
 * Concrete implementation of {@link HoverInformation}.
 * 
 * @coverage dart.server.remote
 */
public class HoverInformationImpl implements HoverInformation {
  private final int offset;
  private final int length;
  private final String containingLibraryName;
  private final String containingLibraryPath;
  private final String dartdoc;
  private final String elementDescription;
  private final String elementKind;
  private final String parameter;
  private final String propagatedType;
  private final String staticType;

  public HoverInformationImpl(int offset, int length, String containingLibraryName,
      String containingLibraryPath, String dartdoc, String elementDescription, String elementKind,
      String parameter, String propagatedType, String staticType) {
    this.offset = offset;
    this.length = length;
    this.containingLibraryName = containingLibraryName;
    this.containingLibraryPath = containingLibraryPath;
    this.dartdoc = dartdoc;
    this.elementDescription = elementDescription;
    this.elementKind = elementKind;
    this.parameter = parameter;
    this.propagatedType = propagatedType;
    this.staticType = staticType;
  }

  @Override
  public String getContainingLibraryName() {
    return containingLibraryName;
  }

  @Override
  public String getContainingLibraryPath() {
    return containingLibraryPath;
  }

  @Override
  public String getDartdoc() {
    return dartdoc;
  }

  @Override
  public String getElementDescription() {
    return elementDescription;
  }

  @Override
  public String getElementKind() {
    return elementKind;
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public String getParameter() {
    return parameter;
  }

  @Override
  public String getPropagatedType() {
    return propagatedType;
  }

  @Override
  public String getStaticType() {
    return staticType;
  }
}
