/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.correction;

import org.eclipse.jface.text.source.Annotation;
import org.eclipse.wst.sse.ui.internal.reconcile.TemporaryAnnotation;

import java.util.Map;

/*
 * Copied from org.eclipse.ui.internal.ide.registry.MarkerQuery
 */
class AnnotationQuery {
  /**
   * The annotation type targetted by this query. May be <code>null</code>.
   */
  private String type;

  /**
   * A sorted list of the attributes targetted by this query. The list is sorted from least to
   * greatest according to <code>Sting.compare</code>
   */
  private String[] attributes;

  /**
   * Cached hash code value
   */
  private int hashCode;

  /**
   * Creates a new annotation query with the given type and attributes.
   * <p>
   * The type may be <code>null</code>. The attributes may be empty, but not <code>null</code>.
   * </p>
   * 
   * @param problemType the targeted annotation type
   * @param markerAttributes the targeted annotation attributes
   */
  public AnnotationQuery(String annotationType, String[] annotationAttributes) {
    if (annotationAttributes == null) {
      throw new IllegalArgumentException();
    }

    type = annotationType;
    attributes = annotationAttributes;
    computeHashCode();
  }

  /**
   * Performs a query against the given annotation.
   * <p>
   * Returns a <code>AnnotationQueryResult</code> if the marker is appropriate for this query
   * (correct type and has all of the query attributes), otherwise <code>null</code> is returned.
   * 
   * @param annotation the annotation to perform the query against
   * @return a annotation query result or <code>null</code>
   */
  public AnnotationQueryResult performQuery(Annotation anno) {
    if (!(anno instanceof TemporaryAnnotation))
      return null;

    Map annoAttributes = ((TemporaryAnnotation) anno).getAttributes();
    /*
     * https://bugs.eclipse.org/bugs/show_bug.cgi?id=243660
     * 
     * May be null if the original validation IMessage contained no attributes or this annotation
     * was not based on a validation IMessage
     */
    if (annoAttributes == null) {
      return null;
    }

    // Check type
    if (type != null) {
      String problemType = (String) annoAttributes.get("problemType"); //$NON-NLS-1$;
      if (!type.equals(problemType))
        return null;
    }

    String[] values = new String[attributes.length];
    for (int i = 0; i < attributes.length; i++) {
      Object value = annoAttributes.get(attributes[i]);
      if (value == null) {
        return null;
      }
      values[i] = value.toString();
    }
    // Create and return the result
    return new AnnotationQueryResult(values);
  }

  /*
   * (non-Javadoc) Method declared on Object.
   */
  public boolean equals(Object o) {
    if (!(o instanceof AnnotationQuery)) {
      return false;
    }

    if (o == this) {
      return true;
    }

    AnnotationQuery mq = (AnnotationQuery) o;
    if (!(type == null ? mq.type == null : type.equals(mq.type))) {
      return false;
    }

    if (attributes.length != mq.attributes.length) {
      return false;
    }

    for (int i = 0; i < attributes.length; i++) {
      if (!(attributes[i].equals(mq.attributes[i]))) {
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

    if (type != null) {
      hashCode = hashCode * 37 + type.hashCode();
    }

    for (int i = 0; i < attributes.length; i++) {
      hashCode = hashCode * 37 + attributes[i].hashCode();
    }
  }

  /**
   * Returns the targetted marker type. May be <code>null</code>
   * 
   * @return the targetted marker type
   */
  public String getType() {
    return type;
  }

  /**
   * Returns the targetted attributes. The array may be empty.
   * 
   * @return the targetted attributes
   */
  public String[] getAttributes() {
    return attributes;
  }
}
