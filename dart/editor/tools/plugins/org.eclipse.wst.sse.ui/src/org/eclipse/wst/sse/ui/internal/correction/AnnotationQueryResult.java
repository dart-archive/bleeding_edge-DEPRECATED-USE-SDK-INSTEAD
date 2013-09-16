/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.correction;

/*
 * Copied from rg.eclipse.ui.internal.ide.registr.MarkerQueryResult
 */
class AnnotationQueryResult {
  /**
   * An ordered collection of annotation attribute values.
   */
  private String[] values;

  /**
   * Cached hash code value
   */
  private int hashCode;

  /**
   * Creates a new annotation query result with the given values.
   * <p>
   * The values may not be empty.
   * </p>
   * 
   * @param annotationAttributeValues the target annotation's attribute values
   */
  public AnnotationQueryResult(String[] annotationAttributeValues) {
    if (annotationAttributeValues == null) {
      throw new IllegalArgumentException();
    }
    values = annotationAttributeValues;
    computeHashCode();
  }

  /*
   * (non-Javadoc) Method declared on Object.
   */
  public boolean equals(Object o) {
    if (!(o instanceof AnnotationQueryResult)) {
      return false;
    }

    if (o == this) {
      return true;
    }

    AnnotationQueryResult mqr = (AnnotationQueryResult) o;
    if (values.length != mqr.values.length) {
      return false;
    }

    for (int i = 0; i < values.length; i++) {
      if (!(values[i].equals(mqr.values[i]))) {
        return false;
      }
    }

    return true;
  }

  /*
   * (non-Javadoc) Method declared on Object.
   */
  public int hashCode() {
    return hashCode;
  }

  /**
   * Computes the hash code for this instance.
   */
  public void computeHashCode() {
    hashCode = 19;

    for (int i = 0; i < values.length; i++) {
      hashCode = hashCode * 37 + values[i].hashCode();
    }
  }
}
