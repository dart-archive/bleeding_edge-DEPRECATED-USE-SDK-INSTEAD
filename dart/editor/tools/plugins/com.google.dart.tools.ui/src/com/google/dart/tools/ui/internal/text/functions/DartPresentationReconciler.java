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

import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.presentation.PresentationReconciler;

/**
 * Presentation reconciler, adding functionality for operation without a viewer.
 */
public class DartPresentationReconciler extends PresentationReconciler {

  /** Last used document */
  private IDocument fLastDocument;

  /**
   * Constructs a "repair description" for the given damage and returns this description as a text
   * presentation.
   * <p>
   * NOTE: Should not be used if this reconciler is installed on a viewer.
   * </p>
   * 
   * @param damage the damage to be repaired
   * @param document the document whose presentation must be repaired
   * @return the presentation repair description as text presentation
   */
  public TextPresentation createRepairDescription(IRegion damage, IDocument document) {
    if (document != fLastDocument) {
      setDocumentToDamagers(document);
      setDocumentToRepairers(document);
      fLastDocument = document;
    }
    return createPresentation(damage, document);
  }
}
