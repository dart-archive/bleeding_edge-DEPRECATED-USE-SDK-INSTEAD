/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.core.internal.model;

import com.google.common.base.Objects;
import com.google.dart.tools.core.internal.util.MementoTokenizer;
import com.google.dart.tools.core.model.CompilationUnit;
import com.google.dart.tools.core.model.DartElement;
import com.google.dart.tools.core.model.DartImport;
import com.google.dart.tools.core.model.DartLibrary;
import com.google.dart.tools.core.model.SourceRange;
import com.google.dart.tools.core.workingcopy.WorkingCopyOwner;

/**
 * Information about imported {@link DartLibrary}.
 */
public class DartImportImpl extends SourceReferenceImpl implements DartImport {
  private final SourceRange sourceRange;
  private final SourceRange uriRange;
  private final DartLibrary library;
  private final String prefix;
  private final SourceRange nameRange;

  public DartImportImpl(CompilationUnitImpl parent, SourceRange sourceRange, SourceRange uriRange,
      DartLibrary library, String prefix, SourceRange nameRange) {
    super(parent);
    this.sourceRange = sourceRange;
    this.uriRange = uriRange;
    this.library = library;
    this.prefix = prefix;
    this.nameRange = nameRange;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof DartImportImpl) {
      DartImportImpl other = (DartImportImpl) obj;
      return Objects.equal(other.library, library) && Objects.equal(other.prefix, prefix);
    }
    return false;
  }

  @Override
  public boolean exists() {
    return true;
  }

  @Override
  public CompilationUnit getCompilationUnit() {
    return (CompilationUnit) getParent();
  }

  @Override
  public String getElementName() {
    return prefix;
  }

  @Override
  public int getElementType() {
    return IMPORT;
  }

  @Override
  public DartLibrary getLibrary() {
    return library;
  }

  @Override
  public SourceRange getNameRange() {
    return nameRange;
  }

  @Override
  public String getPrefix() {
    return prefix;
  }

  @Override
  public SourceRange getSourceRange() {
    return sourceRange;
  }

  @Override
  public SourceRange getUriRange() {
    return uriRange;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(library, prefix);
  }

  @Override
  protected DartElement getHandleFromMemento(String token, MementoTokenizer tokenizer,
      WorkingCopyOwner owner) {
    throw new RuntimeException("Not implemented");
  }

  @Override
  protected char getHandleMementoDelimiter() {
    throw new RuntimeException("Not implemented");
  }

}
