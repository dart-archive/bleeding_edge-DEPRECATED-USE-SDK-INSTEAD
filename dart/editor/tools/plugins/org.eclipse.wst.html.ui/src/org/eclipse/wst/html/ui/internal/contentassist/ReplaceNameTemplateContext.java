/*******************************************************************************
 * Copyright (c) 2006, 2010 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.html.ui.internal.contentassist;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.templates.DocumentTemplateContext;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContextType;
import org.eclipse.jface.text.templates.TemplateException;
import org.eclipse.jface.text.templates.TemplateVariable;

/**
 * Just like DocumentTemplateContext except if an insert offset is passed in, during evaluation, the
 * "prefix" before the template will be checked to see if it matches the template name. If so,
 * overwrite the template name. Otherwise, just insert the template at the insert offset location
 * (by not overwriting the prefix text)
 * 
 * @deprecated No longer used
 */
public class ReplaceNameTemplateContext extends DocumentTemplateContext {
  private int fInsertOffset = -1;

  /**
   * Creates a document template context.
   * 
   * @param type the context type
   * @param document the document this context applies to
   * @param offset the offset of the document region
   * @param length the length of the document region
   */
  public ReplaceNameTemplateContext(TemplateContextType type, IDocument document, int offset,
      int length) {
    this(type, document, new Position(offset, length));
  }

  /**
   * Creates a document template context. The supplied <code>Position</code> will be queried to
   * compute the <code>getStart</code> and <code>getEnd</code> methods, which will therefore answer
   * updated position data if it is registered with the document.
   * 
   * @param type the context type
   * @param document the document this context applies to
   * @param position the position describing the area of the document which forms the template
   *          context
   * @since 3.1
   */
  public ReplaceNameTemplateContext(TemplateContextType type, IDocument document, Position position) {
    super(type, document, position);
  }

  /**
   * Creates a document template context.
   * 
   * @param type the context type
   * @param document the document this context applies to
   * @param offset the offset of the document region
   * @param length the length of the document region
   * @param insertOffset the offset of the document region where insert was originally requested
   */
  public ReplaceNameTemplateContext(TemplateContextType type, IDocument document, int offset,
      int length, int insertOffset) {
    this(type, document, new Position(offset, length));
    fInsertOffset = insertOffset;
  }

  /*
   * @see
   * org.eclipse.jface.text.templates.TemplateContext#evaluate(org.eclipse.jface.text.templates.
   * Template)
   */
  public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException {
    TemplateBuffer buffer = super.evaluate(template);
    if (buffer != null) {
      if (fInsertOffset > -1 && fInsertOffset > getStart()) {
        String prefix = getDocument().get(getStart(), fInsertOffset - getStart());
        if (!template.getName().startsWith(prefix)) {
          // generate a new buffer that actually contains the
          // text that was going to be overwritten
          int prefixSize = prefix.length();
          TemplateVariable[] newTemplateVar = buffer.getVariables();
          for (int i = 0; i < newTemplateVar.length; i++) {
            int[] offsets = newTemplateVar[i].getOffsets();
            for (int j = 0; j < offsets.length; j++) {
              offsets[j] += prefixSize;
            }
          }
          buffer = new TemplateBuffer(prefix + buffer.getString(), newTemplateVar);
        }
      }
    }
    return buffer;
  }
}
