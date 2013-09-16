/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.templates;

import org.eclipse.jface.text.templates.GlobalTemplateVariables;
import org.eclipse.jface.text.templates.TemplateContextType;

/**
 * Base class for HTML template context types. Templates of this context type apply to any place
 * within HTML content type.
 */
public class TemplateContextTypeHTML extends TemplateContextType {

  public TemplateContextTypeHTML() {
    super();
    addResolver(new GlobalTemplateVariables.Cursor());
    addResolver(new GlobalTemplateVariables.Date());
    addResolver(new GlobalTemplateVariables.Dollar());
    addResolver(new GlobalTemplateVariables.LineSelection());
    addResolver(new GlobalTemplateVariables.Time());
    addResolver(new GlobalTemplateVariables.User());
    addResolver(new GlobalTemplateVariables.WordSelection());
    addResolver(new GlobalTemplateVariables.Year());
    addResolver(new EncodingTemplateVariableResolverHTML());
  }
}
