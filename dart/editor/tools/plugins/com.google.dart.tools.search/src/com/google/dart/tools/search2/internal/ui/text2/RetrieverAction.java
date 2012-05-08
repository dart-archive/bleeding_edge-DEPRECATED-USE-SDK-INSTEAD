/*
 * Copyright (c) 2012, the Dart project authors.
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
package com.google.dart.tools.search2.internal.ui.text2;

import com.google.dart.tools.search.internal.ui.SearchPlugin;
import com.google.dart.tools.search.ui.ISearchQuery;
import com.google.dart.tools.search.ui.NewSearchUI;
import com.google.dart.tools.search.ui.text.TextSearchQueryProvider;
import com.google.dart.tools.search2.internal.ui.SearchMessages;

import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.OperationCanceledException;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;

import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.forms.editor.FormEditor;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.ui.texteditor.ITextEditor;

abstract public class RetrieverAction extends Action {
  public RetrieverAction() {
  }

  public void run() {
    IWorkbenchPage page = getWorkbenchPage();
    if (page == null) {
      return;
    }
    TextSearchQueryProvider provider = TextSearchQueryProvider.getPreferred();
    String searchForString = getSearchForString(page);
    if (searchForString.length() == 0) {
      MessageDialog.openInformation(
          getShell(),
          SearchMessages.RetrieverAction_dialog_title,
          SearchMessages.RetrieverAction_empty_selection);
      return;
    }
    try {
      ISearchQuery query = createQuery(provider, searchForString);
      if (query != null) {
        NewSearchUI.runQueryInBackground(query);
      }
    } catch (OperationCanceledException ex) {
      // action cancelled
    } catch (CoreException e) {
      ErrorDialog.openError(
          getShell(),
          SearchMessages.RetrieverAction_error_title,
          SearchMessages.RetrieverAction_error_message,
          e.getStatus());
    }
  }

  private IWorkbenchPart getActivePart() {
    IWorkbenchPage page = getWorkbenchPage();
    if (page != null) {
      return page.getActivePart();
    }
    return null;
  }

  private Shell getShell() {
    IWorkbenchPart part = getActivePart();
    if (part != null) {
      return part.getSite().getShell();
    }
    return SearchPlugin.getActiveWorkbenchShell();
  }

  abstract protected IWorkbenchPage getWorkbenchPage();

  abstract protected ISearchQuery createQuery(TextSearchQueryProvider provider,
      String searchForString) throws CoreException;

  final protected String extractSearchTextFromEditor(IEditorPart editor) {
    if (editor != null) {
      ITextSelection selection = null;
      ISelectionProvider provider = editor.getEditorSite().getSelectionProvider();
      if (provider != null) {
        ISelection s = provider.getSelection();
        if (s instanceof ITextSelection) {
          selection = (ITextSelection) s;
        }
      }

      if (selection != null) {
        if (selection.getLength() == 0) {
          ITextEditor txtEditor = getTextEditor(editor);
          if (txtEditor != null) {
            IDocument document = txtEditor.getDocumentProvider().getDocument(
                txtEditor.getEditorInput());
            selection = expandSelection(selection, document, null);
          }
        }

        if (selection.getLength() > 0 && selection.getText() != null) {
          return trimSearchString(selection.getText());
        }
      }
    }
    return null;
  }

  final protected String extractSearchTextFromSelection(ISelection sel) {
    if (sel instanceof ITextSelection) {
      String text = ((ITextSelection) sel).getText();
      if (text != null) {
        return trimSearchString(text);
      }
    } else if (sel instanceof IStructuredSelection) {
      Object firstElement = ((IStructuredSelection) sel).getFirstElement();
      if (firstElement instanceof IAdaptable) {
        IWorkbenchAdapter wbAdapter = (IWorkbenchAdapter) ((IAdaptable) firstElement).getAdapter(IWorkbenchAdapter.class);
        if (wbAdapter != null) {
          return wbAdapter.getLabel(firstElement);
        }
      }
    }
    return null;
  }

  final protected String extractSearchTextFromWidget(Control control) {
    String sel = null;
    if (control instanceof Combo) {
      Combo combo = (Combo) control;
      sel = combo.getText();
      Point selection = combo.getSelection();
      sel = sel.substring(selection.x, selection.y);
    }
    if (control instanceof CCombo) {
      CCombo combo = (CCombo) control;
      sel = combo.getText();
      Point selection = combo.getSelection();
      sel = sel.substring(selection.x, selection.y);
    } else if (control instanceof Text) {
      Text text = (Text) control;
      sel = text.getSelectionText();
    } else if (control instanceof FormText) {
      FormText text = (FormText) control;
      sel = text.getSelectionText();
    } else if (control instanceof StyledText) {
      StyledText text = (StyledText) control;
      sel = text.getSelectionText();
    } else if (control instanceof Tree) {
      Tree tree = (Tree) control;
      TreeItem[] s = tree.getSelection();
      if (s.length > 0) {
        sel = s[0].getText();
      }
    } else if (control instanceof Table) {
      Table tree = (Table) control;
      TableItem[] s = tree.getSelection();
      if (s.length > 0) {
        sel = s[0].getText();
      }
    } else if (control instanceof List) {
      List list = (List) control;
      String[] s = list.getSelection();
      if (s.length > 0) {
        sel = s[0];
      }
    }

    if (sel != null) {
      sel = trimSearchString(sel);
    }
    return sel;
  }

  private String trimSearchString(String text) {
    text = text.trim();
    int idx = text.indexOf('\n');
    int idx2 = text.indexOf('\r');
    if (idx2 >= 0 && idx2 < idx) {
      idx = idx2;
    }
    if (idx >= 0) {
      text = text.substring(0, idx);
    }
    return text;
  }

  private ITextEditor getTextEditor(IEditorPart editor) {
    if (editor instanceof ITextEditor) {
      return (ITextEditor) editor;
    } else if (editor instanceof FormEditor) {
      FormEditor me = (FormEditor) editor;
      editor = me.getActiveEditor();
      if (editor instanceof ITextEditor) {
        return (ITextEditor) editor;
      }
    }
    return null;
  }

  private ITextSelection expandSelection(ITextSelection sel, IDocument document, String stopChars) {
    int offset = sel.getOffset();
    int length = sel.getLength();

    // in case the length is zero we have to decide whether to go
    // left or right.
    if (length == 0) {
      // try right
      char chr = 0;
      char chl = 0;
      try {
        chr = document.getChar(offset);
      } catch (BadLocationException e2) {
      }
      try {
        chl = document.getChar(offset - 1);
      } catch (BadLocationException e2) {
      }

      if (isPartOfIdentifier(chr)) {
        length = 1;
      } else if (isPartOfIdentifier(chl)) {
        offset--;
        length = 1;
      } else if (stopChars != null && stopChars.indexOf(chr) == -1) {
        length = 1;
      } else if (stopChars != null && stopChars.indexOf(chl) == -1) {
        offset--;
        length = 1;
      } else {
        return sel;
      }
    }

    int a = offset + length - 1;
    int z = a;

    // move z one behind last character.
    try {
      char ch = document.getChar(z);
      while (isValidChar(stopChars, ch)) {
        ch = document.getChar(++z);
      }
    } catch (BadLocationException e2) {
    }
    // move a one before the first character
    try {
      char ch = document.getChar(a);
      while (isValidChar(stopChars, ch)) {
        ch = document.getChar(--a);
      }
    } catch (BadLocationException e2) {
    }

    if (a == z) {
      offset = a;
      length = 0;
    } else {
      offset = a + 1;
      length = z - a - 1;
    }
    return new TextSelection(document, offset, length);
  }

  private boolean isValidChar(String stopChars, char ch) {
    return stopChars == null ? isPartOfIdentifier(ch) : stopChars.indexOf(ch) == -1;
  }

  private boolean isPartOfIdentifier(char ch) {
    return Character.isLetterOrDigit(ch) || ch == '_';
  }

  protected String getSearchForString(IWorkbenchPage page) {
    String searchFor = extractSearchTextFromSelection(page.getSelection());
    if (searchFor == null || searchFor.length() == 0) {
      IWorkbenchPart activePart = page.getActivePart();
      if (activePart instanceof IEditorPart) {
        searchFor = extractSearchTextFromEditor((IEditorPart) activePart);
      }
      if (searchFor == null) {
        Control focus = page.getWorkbenchWindow().getShell().getDisplay().getFocusControl();
        if (focus != null)
          searchFor = extractSearchTextFromWidget(focus);
      }
    }
    return searchFor == null ? "" : searchFor; //$NON-NLS-1$
  }

}
