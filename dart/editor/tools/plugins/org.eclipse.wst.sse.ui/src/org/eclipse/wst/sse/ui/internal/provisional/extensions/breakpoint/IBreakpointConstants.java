/*******************************************************************************
 * Copyright (c) 2001, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.extensions.breakpoint;

/**
 * @author pavery
 */
public interface IBreakpointConstants {
  String ATTR_HIDDEN = "hidden"; //$NON-NLS-1$
  /**
   * Setters of this attribute should use '/'for segment separators when representing paths.
   */
  String RESOURCE_PATH = "org.eclipse.wst.sse.ui.extensions.breakpoint.path"; //$NON-NLS-1$
}
