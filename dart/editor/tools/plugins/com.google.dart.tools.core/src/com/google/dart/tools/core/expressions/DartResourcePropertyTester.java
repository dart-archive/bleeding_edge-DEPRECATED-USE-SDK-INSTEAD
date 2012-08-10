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
package com.google.dart.tools.core.expressions;

import com.google.dart.tools.core.DartCore;
import com.google.dart.tools.core.internal.model.DartProjectNature;
import com.google.dart.tools.core.utilities.general.AdapterUtilities;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IResource;

/**
 * Tests files to see if they are being ignored by the analyzer.
 */
public class DartResourcePropertyTester extends PropertyTester {

  public static final String PROPERTY_NAMESPACE = "com.google.dart.tools.core.expressions"; //$NON-NLS-1$
  public static final String PROPERTY_IS_ANALYZED = "isAnalyzed"; //$NON-NLS-1$
  public static final String PROPERTY_IS_IGNORED = "isIgnored"; //$NON-NLS-1$
  public static final String PROPERTY_IS_ANALYZABLE = "isAnalyzable"; //$NON-NLS-1$

  @Override
  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {

    if (PROPERTY_IS_ANALYZED.equals(property)) {
      return testIsAnalyzed(receiver);
    } else if (PROPERTY_IS_IGNORED.equals(property)) {
      return testIsIgnored(receiver);
    } else if (PROPERTY_IS_ANALYZABLE.equals(property)) {
      return testIsAnalyzable(receiver);
    }

    return false;
  }

  private boolean isAnalyzableDartResource(Object elem) {

    IResource resource = AdapterUtilities.getAdapter(elem, IResource.class);

    if (resource == null) {
      return false;
    }

    return DartCore.isAnalyzed(resource);
  }

  private boolean isIgnoredDartResource(Object elem) {

    if (!testIsAnalyzable(elem)) {
      return false;
    }

    IResource resource = AdapterUtilities.getAdapter(elem, IResource.class);

    if (resource == null) {
      return false;
    }

    return !DartCore.isAnalyzed(resource);
  }

  private boolean testIsAnalyzable(Object receiver) {

    IResource resource = AdapterUtilities.getAdapter(receiver, IResource.class);

    if (resource == null) {
      return false;
    }

    if (!DartProjectNature.hasDartNature(resource)) {
      return false;
    }

    return DartCore.isDartLikeFileName(resource.getName());
  }

  private boolean testIsAnalyzed(Object receiver) {
    if (receiver instanceof Iterable<?>) {
      for (Object elem : (Iterable<?>) receiver) {
        if (!isAnalyzableDartResource(elem)) {
          return false;
        }
      }
    } else {
      return isAnalyzableDartResource(receiver);
    }

    return true;
  }

  private boolean testIsIgnored(Object receiver) {
    if (receiver instanceof Iterable<?>) {
      for (Object elem : (Iterable<?>) receiver) {
        if (!isIgnoredDartResource(elem)) {
          return false;
        }
      }
    } else {
      return isIgnoredDartResource(receiver);
    }

    return true;
  }
}
