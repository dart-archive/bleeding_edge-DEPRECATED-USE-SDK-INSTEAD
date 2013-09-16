/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.sse.ui.internal.rules;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.rules.DefaultDamagerRepairer;
import org.eclipse.jface.text.rules.RuleBasedScanner;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.ui.internal.provisional.style.AbstractLineStyleProvider;
import org.eclipse.wst.sse.ui.internal.provisional.style.LineStyleProvider;

/**
 * An implementation of a presentation damager and presentation repairer. It uses a
 * LineStyleProvider to retrieve the style ranges associated with the calculated damaged region.
 * 
 * @see LineStyleProvider
 */
public class StructuredDocumentDamagerRepairer extends DefaultDamagerRepairer {

  private LineStyleProvider fProvider = null;

  public StructuredDocumentDamagerRepairer(LineStyleProvider provider) {
    super(new RuleBasedScanner());
    Assert.isNotNull(provider);
    fProvider = provider;
  }

  public void createPresentation(TextPresentation presentation, ITypedRegion region) {
    PresentationCollector collector = new PresentationCollector(presentation);
    fProvider.prepareRegions(region, region.getOffset(), region.getLength(), collector);
  }

  public void setProvider(LineStyleProvider provider) {
    fProvider = provider;
  }

  public void setDocument(IDocument document) {
    super.setDocument(document);
    if (fProvider instanceof AbstractLineStyleProvider)
      ((AbstractLineStyleProvider) fProvider).setDocument((IStructuredDocument) document);
  }

}
