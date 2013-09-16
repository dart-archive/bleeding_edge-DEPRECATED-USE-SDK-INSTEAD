/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.common.ui.internal.search.dialogs;

import org.eclipse.core.resources.IFile;
import org.eclipse.wst.common.core.search.pattern.QualifiedName;

// TODO.. use QualifiedName consistently for name and metaName
//
public class ComponentSpecification {
  String qualifier;
  String name;
  IFile file;
  Object object;
  boolean isNew;
  QualifiedName metaName;

  public ComponentSpecification() {
  }

  public ComponentSpecification(String qualifier, String name, IFile file) {
    super();
    this.qualifier = qualifier;
    this.name = name;
    this.file = file;
  }

  public IFile getFile() {
    return file;
  }

  public void setFile(IFile file) {
    this.file = file;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getQualifier() {
    return qualifier;
  }

  public void setQualifier(String qualifier) {
    this.qualifier = qualifier;
  }

  public Object getObject() {
    return object;
  }

  public void setObject(Object object) {
    this.object = object;
  }

  public boolean isNew() {
    return isNew;
  }

  public void setNew(boolean isNew) {
    this.isNew = isNew;
  }

  public QualifiedName getMetaName() {
    return metaName;
  }

  public void setMetaName(QualifiedName metaName) {
    this.metaName = metaName;
  }
}
