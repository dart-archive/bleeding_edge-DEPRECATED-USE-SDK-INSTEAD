/*
 * Copyright (c) 2013, the Dart project authors.
 * 
 * Licensed under the Eclipse Public License v1.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.dart.tools.ui.actions;

import com.google.dart.engine.element.ClassElement;
import com.google.dart.engine.element.CompilationUnitElement;
import com.google.dart.engine.element.Element;
import com.google.dart.engine.element.LibraryElement;
import com.google.dart.engine.source.Source;
import com.google.dart.tools.ui.instrumentation.UIInstrumentationBuilder;
import com.google.dart.tools.ui.internal.text.editor.DartEditor;
import com.google.dart.tools.ui.internal.text.editor.DartSelection;
import com.google.dart.tools.ui.internal.util.ExternalBrowserUtil;

import org.eclipse.swt.widgets.Event;

/**
 * This action opens an external DartDoc link.
 */
public class OpenExternalDartdocAction extends AbstractDartSelectionAction {
  /**
   * @return {@code true} if given {@link DartSelection} looks valid and we can try to open Dart Doc
   *         for it.
   */
  private static boolean isValidSelection(DartSelection selection) {
    Element element = getSelectionElement(selection);
    // no element
    if (element == null) {
      return false;
    }
    // top-level or class member
    {
      Element enclosing = element.getEnclosingElement();
      if (enclosing instanceof CompilationUnitElement || enclosing instanceof ClassElement) {
      } else {
        return false;
      }
    }
    // prepare LibraryElement
    LibraryElement libraryElement = element.getLibrary();
    if (libraryElement == null) {
      return false;
    }
    // not external, so cannot be on http://api.dartlang.org/
    Source librarySource = libraryElement.getSource();
    if (librarySource == null || !librarySource.isInSystemLibrary()) {
      return false;
    }
    // OK
    return true;
  }

  public OpenExternalDartdocAction(DartEditor editor) {
    super(editor);
  }

  @Override
  public void selectionChanged(DartSelection selection) {
    setEnabled(isValidSelection(selection));
  }

  @Override
  protected void doRun(DartSelection selection, Event event,
      UIInstrumentationBuilder instrumentation) {
    // prepare Element
    Element element = getSelectionElement(selection);
    if (element == null) {
      instrumentation.metric("Problem", "element was null");
      return;
    }
    // prepare LibraryElement
    LibraryElement libraryElement = element.getLibrary();
    if (libraryElement == null) {
      instrumentation.metric("Problem", "library was null");
      return;
    }
    String libraryName = libraryElement.getName();
    // prepare names
    String className;
    String elementName;
    ClassElement enclosingClassElement = element.getAncestor(ClassElement.class);
    if (element instanceof ClassElement) {
      className = element.getName();
      elementName = null;
    } else if (enclosingClassElement != null) {
      className = enclosingClassElement.getName();
      elementName = element.getName();
    } else {
      className = null;
      elementName = element.getName();
    }
    // do open
    browseDartDoc(libraryName, className, elementName, instrumentation);
  }

  @Override
  protected void init() {
    setText(ActionMessages.OpenExternalDartdocAction_label);
    setDescription(ActionMessages.OpenExternalDartdocAction_description);
    setToolTipText(ActionMessages.OpenExternalDartdocAction_tooltip);
  }

  /**
   * Open an external browser on the Dart doc for the specified library and class.
   * 
   * @param libraryName the library name (not {@code null})
   * @param className the class name or {@code null} to display library Dart doc
   * @param elementName the name of the element in library or class, may be {@code null}
   */
  private void browseDartDoc(String libraryName, String className, String elementName,
      UIInstrumentationBuilder instrumentation) {
    String url = "http://api.dartlang.org/";
    if (libraryName != null) {
      libraryName = libraryName.replace(':', '_');
      libraryName = libraryName.replace('.', '_');
      url += libraryName;
      // class
      if (className != null) {
        url += '/' + className;
      }
      url += ".html";
      // specific element
      if (elementName != null) {
        url += "#" + elementName;
      }
    }

    instrumentation.metric("ClickTarget", className != null ? "type" : "library");
    instrumentation.data("LibraryName", libraryName).data("ClassNameHtml", className);
    instrumentation.data("Url", url);

    ExternalBrowserUtil.openInExternalBrowser(url);
  }
}
