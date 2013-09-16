/*******************************************************************************
 * Copyright (c) 2001, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation Jens Lukowski/Innoopract - initial renaming/restructuring
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.contentoutline;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.wst.sse.core.internal.format.IStructuredFormatProcessor;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.xml.core.internal.contentmodel.modelquery.ModelQuery;
import org.eclipse.wst.xml.core.internal.modelquery.ModelQueryUtil;
import org.eclipse.wst.xml.core.internal.provisional.format.FormatProcessorXML;
import org.eclipse.wst.xml.ui.internal.actions.AbstractNodeActionManager;
import org.w3c.dom.Node;

public class XMLNodeActionManager extends AbstractNodeActionManager {
  public XMLNodeActionManager(IStructuredModel model, Viewer viewer) {
    super(model, ModelQueryUtil.getModelQuery(model), viewer);
  }

  public void reformat(Node newElement, boolean deep) {
    try {
      // tell the model that we are about to make a big model change
      fModel.aboutToChangeModel();

      // format selected node
      IStructuredFormatProcessor formatProcessor = new FormatProcessorXML();
      formatProcessor.formatNode(newElement);
    } finally {
      // tell the model that we are done with the big model change
      fModel.changedModel();
    }
  }

  public void setModel(IStructuredModel newModel) {
    fModel = newModel;
    setModelQuery(ModelQueryUtil.getModelQuery(newModel));
  }

  protected void setModelQuery(ModelQuery newModelQuery) {
    modelQuery = newModelQuery;
  }
}
