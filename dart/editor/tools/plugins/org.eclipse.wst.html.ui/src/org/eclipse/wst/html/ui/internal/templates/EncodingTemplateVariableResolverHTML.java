/*******************************************************************************
 * Copyright (c) 2005 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.templates;

import org.eclipse.jface.text.templates.SimpleTemplateVariableResolver;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.wst.html.core.internal.HTMLCorePlugin;
import org.eclipse.wst.html.ui.internal.HTMLUIMessages;
import org.eclipse.wst.sse.core.internal.encoding.CommonEncodingPreferenceNames;

public class EncodingTemplateVariableResolverHTML extends SimpleTemplateVariableResolver {
  private static final String ENCODING_TYPE = getEncodingType();

  private static String getEncodingType() {
    return "encoding"; //$NON-NLS-1$
  }

  /**
   * Creates a new encoding variable
   */
  public EncodingTemplateVariableResolverHTML() {
    super(ENCODING_TYPE, HTMLUIMessages.Creating_files_encoding);
  }

  protected String resolve(TemplateContext context) {
    return HTMLCorePlugin.getDefault().getPluginPreferences().getString(
        CommonEncodingPreferenceNames.OUTPUT_CODESET);
  }
}
