/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.style;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.TextAttribute;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.ui.internal.provisional.style.LineStyleProvider;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.ui.internal.style.IStyleConstantsXML;
import org.eclipse.wst.xml.ui.internal.style.LineStyleProviderForXML;

public class LineStyleProviderForHTML extends LineStyleProviderForXML implements LineStyleProvider {

  public LineStyleProviderForHTML() {
    super();
  }

  /**
   * a method to centralize all the "format rules" for regions specifically associated for how to
   * "open" the region.
   */
  // NOTE: this method was just copied down form LineStyleProviderForXML
  public TextAttribute getAttributeFor(ITextRegion region) {
    // not sure why this is coming through null, but just to catch it
    if (region == null) {
      return (TextAttribute) getTextAttributes().get(IStyleConstantsXML.XML_CONTENT);
    }
    String type = region.getType();
    if (type == DOMRegionContext.BLOCK_TEXT) {
      return (TextAttribute) getTextAttributes().get(IStyleConstantsXML.XML_CONTENT);
    }
    // workaround: make PI edges the same color as tag edges
    else if ((type == DOMRegionContext.XML_PI_OPEN) || (type == DOMRegionContext.XML_PI_CLOSE)) {
      return (TextAttribute) getTextAttributes().get(IStyleConstantsXML.TAG_BORDER);
    }
    // first try "standard" tag attributes from super class
    return super.getAttributeFor(region);
  }

  protected void loadColors() {
    super.loadColors();

    addTextAttribute(IStyleConstantsHTML.SCRIPT_AREA_BORDER);
  }

  protected void handlePropertyChange(PropertyChangeEvent event) {
    if (event != null) {
      String prefKey = event.getProperty();
      // check if preference changed is a style preference
      if (IStyleConstantsHTML.SCRIPT_AREA_BORDER.equals(prefKey)) {
        addTextAttribute(IStyleConstantsHTML.SCRIPT_AREA_BORDER);

        // this is what AbstractLineStyleProvider.propertyChange() does
        getHighlighter().refreshDisplay();
      } else {
        super.handlePropertyChange(event);
      }
    } else {
      super.handlePropertyChange(event);
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.wst.sse.ui.style.AbstractLineStyleProvider#getColorPreferences()
   */
  protected IPreferenceStore getColorPreferences() {
    return HTMLUIPlugin.getDefault().getPreferenceStore();
  }
}
