/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.registry;

import org.eclipse.wst.html.core.internal.modelhandler.ModelHandlerForHTML;
import org.eclipse.wst.html.ui.internal.contentoutline.JFaceNodeAdapterFactoryForHTML;
import org.eclipse.wst.sse.core.internal.ltk.modelhandler.IDocumentTypeHandler;
import org.eclipse.wst.sse.core.internal.model.FactoryRegistry;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapter;
import org.eclipse.wst.sse.ui.internal.provisional.registry.AdapterFactoryProvider;
import org.eclipse.wst.sse.ui.internal.util.Assert;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;

public class AdapterFactoryProviderForHTML implements AdapterFactoryProvider {

  public void addAdapterFactories(IStructuredModel structuredModel) {

    // these are the normal edit side content based factories
    addContentBasedFactories(structuredModel);
    // Must update/add to propagating adapter here too
    if (structuredModel instanceof IDOMModel) {
      addPropagatingAdapters(structuredModel);
    }
  }

  protected void addContentBasedFactories(IStructuredModel structuredModel) {

    FactoryRegistry factoryRegistry = structuredModel.getFactoryRegistry();
    Assert.isNotNull(factoryRegistry,
        "Program Error: client caller must ensure model has factory registry"); //$NON-NLS-1$
    INodeAdapterFactory factory = null;

    factory = factoryRegistry.getFactoryFor(IJFaceNodeAdapter.class);
    if (factory == null) {
      factory = new JFaceNodeAdapterFactoryForHTML();
      factoryRegistry.addFactory(factory);
    }
  }

  protected void addPropagatingAdapters(IStructuredModel structuredModel) {
    // no propagating to add
  }

  /*
   * @see AdapterFactoryProvider#isFor(ContentTypeDescription)
   */
  public boolean isFor(IDocumentTypeHandler contentTypeDescription) {
    return (contentTypeDescription instanceof ModelHandlerForHTML);
  }

  public void reinitializeFactories(IStructuredModel structuredModel) {
    // nothing to do, since no embedded type
  }

}
