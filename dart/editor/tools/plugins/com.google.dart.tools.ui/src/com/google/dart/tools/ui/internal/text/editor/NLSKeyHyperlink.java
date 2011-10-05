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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.core.dom.ITypeBinding;
import com.google.dart.tools.core.model.DartModelException;
import com.google.dart.tools.ui.AccessorClassReference;
import com.google.dart.tools.ui.Messages;
import com.google.dart.tools.ui.NLSHintHelper;

import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.texteditor.IEditorStatusLine;

/**
 * NLS key hyperlink.
 */
public class NLSKeyHyperlink implements IHyperlink {

  private IRegion fRegion;
  private AccessorClassReference fAccessorClassReference;
  private IEditorPart fEditor;
  private final String fKeyName;

  /**
   * Creates a new NLS key hyperlink.
   * 
   * @param region
   * @param keyName
   * @param ref
   * @param editor the editor which contains the hyperlink
   */
  public NLSKeyHyperlink(IRegion region, String keyName, AccessorClassReference ref,
      IEditorPart editor) {
    Assert.isNotNull(region);
    Assert.isNotNull(keyName);
    Assert.isNotNull(ref);
    Assert.isNotNull(editor);

    fRegion = region;
    fKeyName = keyName;
    fAccessorClassReference = ref;
    fEditor = editor;
  }

  /*
   * @see com.google.dart.tools.ui.editor.IHyperlink#getHyperlinkRegion()
   */
  @Override
  public IRegion getHyperlinkRegion() {
    return fRegion;
  }

  /*
   * @see com.google.dart.tools.ui.editor.IHyperlink#getHyperlinkText()
   */
  @Override
  public String getHyperlinkText() {
    return null;
  }

  /*
   * @see com.google.dart.tools.ui.editor.IHyperlink#getTypeLabel()
   */
  @Override
  public String getTypeLabel() {
    return null;
  }

  /*
   * @see com.google.dart.tools.ui.editor.IHyperlink#open()
   */
  @Override
  public void open() {
    IStorage propertiesFile = null;
    try {
      ITypeBinding typeBinding = fAccessorClassReference.getBinding();
      propertiesFile = NLSHintHelper.getResourceBundle(typeBinding.getElement().getDartProject(),
          fAccessorClassReference);
    } catch (DartModelException e) {
      // Don't open the file
    }
    if (propertiesFile == null) {
      showErrorInStatusLine(fEditor,
          DartEditorMessages.Editor_OpenPropertiesFile_error_fileNotFound_dialogMessage);
      return;
    }

    IEditorPart editor;
    try {
      editor = EditorUtility.openInEditor(propertiesFile, true);
    } catch (PartInitException e) {
      handleOpenPropertiesFileFailed(propertiesFile);
      return;
    } catch (DartModelException e) {
      handleOpenPropertiesFileFailed(propertiesFile);
      return;
    }

//		// Reveal the key in the properties file
//		if (editor instanceof ITextEditor) {
//			IRegion region= null;
//			boolean found= false;
//
//			// Find key in document
//			IEditorInput editorInput= editor.getEditorInput();
//			IDocument document= ((ITextEditor)editor).getDocumentProvider().getDocument(editorInput);
//			if (document != null) {
//				FindReplaceDocumentAdapter finder= new FindReplaceDocumentAdapter(document);
//				PropertyKeyHyperlinkDetector detector= new PropertyKeyHyperlinkDetector();
//				detector.setContext(editor);
//				String key= PropertyFileDocumentModel.unwindEscapeChars(fKeyName);
//				int offset= document.getLength() - 1;
//				try {
//					while (!found && offset >= 0) {
//						region= finder.find(offset, key, false, true, false, false);
//						if (region == null)
//							offset= -1;
//						else {
//							// test whether it's the key
//							IHyperlink[] hyperlinks= detector.detectHyperlinks(null, region, false);
//							if (hyperlinks != null) {
//								for (int i= 0; i < hyperlinks.length; i++) {
//									IRegion hyperlinkRegion= hyperlinks[i].getHyperlinkRegion();
//									found= key.equals(document.get(hyperlinkRegion.getOffset(), hyperlinkRegion.getLength()));
//								}
//							} else if (document instanceof IDocumentExtension3) {
//								// Fall back: test using properties file partitioning
//								ITypedRegion partition= null;
//								partition= ((IDocumentExtension3)document).getPartition(IPropertiesFilePartitions.PROPERTIES_FILE_PARTITIONING, region.getOffset(), false);
//								found= IDocument.DEFAULT_CONTENT_TYPE.equals(partition.getType())
//										&& key.equals(document.get(partition.getOffset(), partition.getLength()).trim());
//							}
//							// Prevent endless loop (panic code, shouldn't be needed)
//							if (offset == region.getOffset())
//								offset= -1;
//							else
//								offset= region.getOffset();
//						}
//					}
//				} catch (BadLocationException ex) {
//					found= false;
//				} catch (BadPartitioningException e1) {
//					found= false;
//				}
//			}
//			if (found)
//				EditorUtility.revealInEditor(editor, region);
//			else {
//				EditorUtility.revealInEditor(editor, 0, 0);
//				showErrorInStatusLine(editor, Messages.format(DartEditorMessages.Editor_OpenPropertiesFile_error_keyNotFound, fKeyName));
//			}
//		}
  }

  private void handleOpenPropertiesFileFailed(IStorage propertiesFile) {
    showErrorInStatusLine(fEditor, Messages.format(
        DartEditorMessages.Editor_OpenPropertiesFile_error_openEditor_dialogMessage,
        propertiesFile.getFullPath().toOSString()));
  }

  private void showErrorInStatusLine(IEditorPart editor, final String message) {
    final Display display = fEditor.getSite().getShell().getDisplay();
    display.beep();
    final IEditorStatusLine statusLine = (IEditorStatusLine) editor.getAdapter(IEditorStatusLine.class);
    if (statusLine != null) {
      display.asyncExec(new Runnable() {
        /*
         * @see java.lang.Runnable#run()
         */
        @Override
        public void run() {
          statusLine.setMessage(true, message, null);
        }
      });
    }
  }
}
