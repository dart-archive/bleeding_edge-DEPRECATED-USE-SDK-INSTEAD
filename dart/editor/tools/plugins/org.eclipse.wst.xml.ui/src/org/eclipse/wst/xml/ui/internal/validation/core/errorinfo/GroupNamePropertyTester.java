/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.validation.core.errorinfo;

import org.eclipse.core.expressions.PropertyTester;
import org.eclipse.core.resources.IMarker;

/**
 * Custom property tester used to determine if the groupName attribute is present on a marker and
 * that its value starts with a given expected value.
 */
public class GroupNamePropertyTester extends PropertyTester {
  /**
   * The group name prefix property name.
   */
  private static final String GROUP_NAME_PREFIX = "groupNamePrefix"; //$NON-NLS-1$  

  /**
   * The group name marker attribute.
   */
  private static final String GROUP_NAME = "groupName"; //$NON-NLS-1$

  public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
    if (GROUP_NAME_PREFIX.equals(property)) {
      if (receiver instanceof IMarker) {
        IMarker marker = (IMarker) receiver;

        String groupName = marker.getAttribute(GROUP_NAME, null);

        boolean testValue = groupName != null && expectedValue instanceof String
            && groupName.startsWith((String) expectedValue);
        return testValue;
      }
    }

    return false;
  }
}
