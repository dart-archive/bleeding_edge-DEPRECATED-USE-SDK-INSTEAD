/*******************************************************************************
 * Copyright (c) 2001, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.sse.ui.internal.provisional.registry;

import java.util.Iterator;

/**
 * This is basically a "factory for factories". It is to used to associate "edit time"
 * AdapterFactories with a StructuredModel, based on the IStructuredModel's ContentTypeDescription.
 * In plugin.xml files, there should be an AdapterFactoryProvider defined for every definition of
 * ContentTypeDescription.
 */
public interface AdapterFactoryRegistry {

  /**
   * Returns an Iterator over a Collection of AdapterFactoryProviders
   * 
   * @return
   */
  public Iterator getAdapterFactories();
}
