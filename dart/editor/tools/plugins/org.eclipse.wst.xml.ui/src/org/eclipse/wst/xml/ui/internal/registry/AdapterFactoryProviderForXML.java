/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.registry;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.wst.sse.core.internal.ltk.modelhandler.IDocumentTypeHandler;
import org.eclipse.wst.sse.core.internal.model.FactoryRegistry;
import org.eclipse.wst.sse.core.internal.provisional.INodeAdapterFactory;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.util.Assert;
import org.eclipse.wst.sse.ui.internal.contentoutline.IJFaceNodeAdapter;
import org.eclipse.wst.sse.ui.internal.provisional.registry.AdapterFactoryProvider;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.CMDocumentManager;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelhandler.ModelHandlerForXML;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.ui.internal.DOMObserver;
import org.eclipse.wst.xml.ui.internal.XMLUIPlugin;
import org.eclipse.wst.xml.ui.internal.contentoutline.JFaceNodeAdapterFactory;
import org.eclipse.wst.xml.ui.internal.preferences.XMLUIPreferenceNames;

/**
 * 
 */
public class AdapterFactoryProviderForXML implements AdapterFactoryProvider {

  /*
   * @see AdapterFactoryProvider#addAdapterFactories(IStructuredModel)
   */
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
      factory = new JFaceNodeAdapterFactory();
      factoryRegistry.addFactory(factory);
    }

    // cs... added for inferred grammar support
    //
    if (structuredModel != null) {
      ModelQuery modelQuery = ModelQueryUtil.getModelQuery(structuredModel);
      if (modelQuery != null) {
        CMDocumentManager documentManager = modelQuery.getCMDocumentManager();
        if (documentManager != null) {
          IPreferenceStore store = XMLUIPlugin.getDefault().getPreferenceStore();
          boolean useInferredGrammar = (store != null)
              ? store.getBoolean(XMLUIPreferenceNames.USE_INFERRED_GRAMMAR) : true;

          documentManager.setPropertyEnabled(CMDocumentManager.PROPERTY_ASYNC_LOAD, true);
          documentManager.setPropertyEnabled(CMDocumentManager.PROPERTY_AUTO_LOAD, false);
          documentManager.setPropertyEnabled(CMDocumentManager.PROPERTY_USE_CACHED_RESOLVED_URI,
              true);
          DOMObserver domObserver = new DOMObserver(structuredModel);
          domObserver.setGrammarInferenceEnabled(useInferredGrammar);
          domObserver.init();
        }
      }
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.wst.sse.ui.internal.provisional.registry.AdapterFactoryProvider#isFor(org.eclipse
   * .wst.sse.core.internal.ltk.modelhandler.IDocumentTypeHandler)
   */
  public boolean isFor(IDocumentTypeHandler contentTypeDescription) {
    return (contentTypeDescription instanceof ModelHandlerForXML);
  }

  public void reinitializeFactories(IStructuredModel structuredModel) {
    // nothing to do, since no embedded type
  }
}
