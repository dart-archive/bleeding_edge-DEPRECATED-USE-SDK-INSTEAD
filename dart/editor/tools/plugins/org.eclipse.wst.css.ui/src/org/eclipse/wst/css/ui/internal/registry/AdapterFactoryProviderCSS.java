/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.registry;

import org.eclipse.wst.css.core.internal.modelhandler.ModelHandlerForCSS;
import org.eclipse.wst.css.ui.internal.contentoutline.JFaceNodeAdapterFactoryCSS;
import org.eclipse.wst.sse.core.internal.ltk.modelhandler.IDocumentTypeHandler;
import org.eclipse.wst.sse.core.internal.model.FactoryRegistry;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapter;
import org.eclipse.wst.sse.ui.internal.provisional.registry.AdapterFactoryProvider;
import org.eclipse.wst.sse.ui.internal.util.Assert;

public class AdapterFactoryProviderCSS implements AdapterFactoryProvider {
  public boolean isFor(IDocumentTypeHandler contentTypeDescription) {
    return (contentTypeDescription instanceof ModelHandlerForCSS);
  }

  public void addAdapterFactories(IStructuredModel structuredModel) {
    // add the normal content based factories to model's registry
    addContentBasedFactories(structuredModel);
  }

  protected void addContentBasedFactories(IStructuredModel structuredModel) {
    FactoryRegistry factoryRegistry = structuredModel.getFactoryRegistry();
    Assert.isNotNull(factoryRegistry,
        "Program Error: client caller must ensure model has factory registry"); //$NON-NLS-1$
    INodeAdapterFactory factory = null;

    factory = factoryRegistry.getFactoryFor(IJFaceNodeAdapter.class);
    if (factory == null) {
      factory = new JFaceNodeAdapterFactoryCSS(IJFaceNodeAdapter.class, true);
      factoryRegistry.addFactory(factory);
    }
  }

  public void reinitializeFactories(IStructuredModel structuredModel) {

  }

}
