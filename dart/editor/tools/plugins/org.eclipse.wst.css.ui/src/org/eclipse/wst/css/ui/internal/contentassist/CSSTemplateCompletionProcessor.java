/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.contentassist;

import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.templates.ContextTypeRegistry;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateCompletionProcessor;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.persistence.TemplateStore;
import org.eclipse.swt.graphics.Image;
import org.eclipse.wst.css.ui.internal.CSSUIPlugin;
import org.eclipse.wst.css.ui.internal.editor.CSSEditorPluginImages;
import org.eclipse.wst.css.ui.internal.image.CSSImageHelper;

/**
 * Completion processor for CSS Templates. Most of the work is already done by the CSS Content
 * Assist processor, so by the time the CSSTemplateCompletionProcessor is asked for content assist
 * proposals, the CSS content assist processor has already set the context type for templates.
 */
class CSSTemplateCompletionProcessor extends TemplateCompletionProcessor {
  private String fContextTypeId = null;

  protected TemplateContextType getContextType(ITextViewer viewer, IRegion region) {
    TemplateContextType type = null;

    ContextTypeRegistry registry = getTemplateContextRegistry();
    if (registry != null)
      type = registry.getContextType(fContextTypeId);

    return type;
  }

  protected Image getImage(Template template) {
    // just return the same image for now
    return CSSImageHelper.getInstance().getImage(CSSEditorPluginImages.IMG_OBJ_TEMPLATE);
  }

  private ContextTypeRegistry getTemplateContextRegistry() {
    return CSSUIPlugin.getDefault().getTemplateContextRegistry();
  }

  protected Template[] getTemplates(String contextTypeId) {
    Template templates[] = null;

    TemplateStore store = getTemplateStore();
    if (store != null)
      templates = store.getTemplates(contextTypeId);

    return templates;
  }

  private TemplateStore getTemplateStore() {
    return CSSUIPlugin.getDefault().getTemplateStore();
  }

  void setContextType(String contextTypeId) {
    fContextTypeId = contextTypeId;
  }
}
