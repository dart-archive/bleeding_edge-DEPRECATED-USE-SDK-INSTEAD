/*
 * Copyright (c) 2011, the Dart project authors.
 *
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.internal.text.functions;

import com.google.dart.tools.ui.DartUIMessages;

import org.eclipse.jface.internal.text.html.HTMLPrinter;
import org.eclipse.jface.text.source.DefaultAnnotationHover;

import java.util.Iterator;
import java.util.List;

/**
 * Determines all markers for the given line and collects, concatenates, and formats returns their
 * messages in HTML.
 */
@SuppressWarnings("restriction")
public class HTMLAnnotationHover extends DefaultAnnotationHover {

  /*
   * Formats several message as HTML text.
   */
  @Override
  protected String formatMultipleMessages(List messages) {
    StringBuffer buffer = new StringBuffer();
    HTMLPrinter.addPageProlog(buffer);
    HTMLPrinter.addParagraph(
        buffer,
        HTMLPrinter.convertToHTMLContent(DartUIMessages.JavaAnnotationHover_multipleMarkersAtThisLine));

    HTMLPrinter.startBulletList(buffer);
    Iterator e = messages.iterator();
    while (e.hasNext()) {
      HTMLPrinter.addBullet(buffer, HTMLPrinter.convertToHTMLContent((String) e.next()));
    }
    HTMLPrinter.endBulletList(buffer);

    HTMLPrinter.addPageEpilog(buffer);
    return buffer.toString();
  }

  /*
   * Formats a message as HTML text.
   */
  @Override
  protected String formatSingleMessage(String message) {
    StringBuffer buffer = new StringBuffer();
    HTMLPrinter.addPageProlog(buffer);
    HTMLPrinter.addParagraph(buffer, HTMLPrinter.convertToHTMLContent(message));
    HTMLPrinter.addPageEpilog(buffer);
    return buffer.toString();
  }
}
