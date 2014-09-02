/*
 * Copyright (c) 2011, the Dart project authors.
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
package com.google.dart.tools.ui.internal.text.editor;

import com.google.dart.tools.ui.PreferenceConstants;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.dnd.ByteArrayTransfer;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IWorkbenchActionDefinitionIds;
import org.eclipse.ui.texteditor.TextEditorAction;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ResourceBundle;

/**
 * Action for cut/copy and paste with support for adding imports on paste.
 */
public final class ClipboardOperationAction extends TextEditorAction {

  public static class ClipboardData {
    private static String[] readArray(DataInputStream dataIn) throws IOException {
      int count = dataIn.readInt();

      String[] array = new String[count];
      for (int i = 0; i < count; i++) {
        array[i] = dataIn.readUTF();
      }
      return array;
    }

    private static void writeArray(DataOutputStream dataOut, String[] array) throws IOException {
      dataOut.writeInt(array.length);
      for (int i = 0; i < array.length; i++) {
        dataOut.writeUTF(array[i]);
      }
    }

    private String fOriginHandle;

    private String[] fTypeImports;

    private String[] fStaticImports;

    public ClipboardData(byte[] bytes) throws IOException {
      DataInputStream dataIn = new DataInputStream(new ByteArrayInputStream(bytes));
      try {
        fOriginHandle = dataIn.readUTF();
        fTypeImports = readArray(dataIn);
        fStaticImports = readArray(dataIn);
      } finally {
        dataIn.close();
      }
    }

    public String[] getStaticImports() {
      return fStaticImports;
    }

    public String[] getTypeImports() {
      return fTypeImports;
    }

    public byte[] serialize() throws IOException {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      DataOutputStream dataOut = new DataOutputStream(out);
      try {
        dataOut.writeUTF(fOriginHandle);
        writeArray(dataOut, fTypeImports);
        writeArray(dataOut, fStaticImports);
      } finally {
        dataOut.close();
        out.close();
      }

      return out.toByteArray();
    }
  }

  private static class ClipboardTransfer extends ByteArrayTransfer {

    private static final String TYPE_NAME = "source-with-imports-transfer-format" + System.currentTimeMillis(); //$NON-NLS-1$

    private static final int TYPEID = registerType(TYPE_NAME);

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.dnd.Transfer#getTypeIds()
     */
    @Override
    protected int[] getTypeIds() {
      return new int[] {TYPEID};
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.dnd.Transfer#getTypeNames()
     */
    @Override
    protected String[] getTypeNames() {
      return new String[] {TYPE_NAME};
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.swt.dnd.Transfer#javaToNative(java.lang.Object,
     * org.eclipse.swt.dnd.TransferData)
     */
    @Override
    protected void javaToNative(Object data, TransferData transferData) {
      if (data instanceof ClipboardData) {
        try {
          super.javaToNative(((ClipboardData) data).serialize(), transferData);
        } catch (IOException e) {
          // it's best to send nothing if there were problems
        }
      }
    }

    /*
     * (non-Javadoc) Method declared on Transfer.
     */
    @Override
    protected Object nativeToJava(TransferData transferData) {
      byte[] bytes = (byte[]) super.nativeToJava(transferData);
      if (bytes != null) {
        try {
          return new ClipboardData(bytes);
        } catch (IOException e) {
        }
      }
      return null;
    }

  }

  private static final ClipboardTransfer fgTransferInstance = new ClipboardTransfer();

  /** The text operation code */
  private int fOperationCode = -1;
  /** The text operation target */
  private ITextOperationTarget fOperationTarget;

  /**
   * Creates the action.
   */
  @SuppressWarnings("deprecation")
  public ClipboardOperationAction(ResourceBundle bundle, String prefix, ITextEditor editor,
      int operationCode) {
    super(bundle, prefix, editor);
    fOperationCode = operationCode;

    if (operationCode == ITextOperationTarget.CUT) {
      setHelpContextId(IAbstractTextEditorHelpContextIds.CUT_ACTION);
      setActionDefinitionId(IWorkbenchActionDefinitionIds.CUT);
    } else if (operationCode == ITextOperationTarget.COPY) {
      setHelpContextId(IAbstractTextEditorHelpContextIds.COPY_ACTION);
      setActionDefinitionId(IWorkbenchActionDefinitionIds.COPY);
    } else if (operationCode == ITextOperationTarget.PASTE) {
      setHelpContextId(IAbstractTextEditorHelpContextIds.PASTE_ACTION);
      setActionDefinitionId(IWorkbenchActionDefinitionIds.PASTE);
    } else {
      Assert.isTrue(false, "Invalid operation code"); //$NON-NLS-1$
    }
    update();
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.jface.action.IAction#run()
   */
  @Override
  public void run() {
    if (fOperationCode == -1 || fOperationTarget == null) {
      return;
    }

    ITextEditor editor = getTextEditor();
    if (editor == null) {
      return;
    }

    if (!isReadOnlyOperation() && !validateEditorInputState()) {
      return;
    }

    BusyIndicator.showWhile(getDisplay(), new Runnable() {
      @Override
      public void run() {
        internalDoOperation();
      }
    });
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.TextEditorAction#setEditor(org.eclipse.ui.texteditor
   * .ITextEditor)
   */
  @Override
  public void setEditor(ITextEditor editor) {
    super.setEditor(editor);
    fOperationTarget = null;
  }

  /*
   * (non-Javadoc)
   * 
   * @see org.eclipse.ui.texteditor.IUpdate#update()
   */
  @Override
  public void update() {
    super.update();

    if (!isReadOnlyOperation() && !canModifyEditor()) {
      setEnabled(false);
      return;
    }

    ITextEditor editor = getTextEditor();
    if (fOperationTarget == null && editor != null && fOperationCode != -1) {
      fOperationTarget = (ITextOperationTarget) editor.getAdapter(ITextOperationTarget.class);
    }

    boolean isEnabled = (fOperationTarget != null && fOperationTarget.canDoOperation(fOperationCode));
    setEnabled(isEnabled);
  }

  protected final void internalDoOperation() {
    if (PreferenceConstants.getPreferenceStore().getBoolean(
        PreferenceConstants.EDITOR_IMPORTS_ON_PASTE)) {
      if (fOperationCode == ITextOperationTarget.PASTE) {
        doPasteWithImportsOperation();
      } else {
        doCutCopyWithImportsOperation();
      }
    } else {
      fOperationTarget.doOperation(fOperationCode);
    }
  }

  private void doCutCopyWithImportsOperation() {
    fOperationTarget.doOperation(fOperationCode);
  }

  private void doPasteWithImportsOperation() {
    Clipboard clipboard = new Clipboard(getDisplay());
    ClipboardData importsData = (ClipboardData) clipboard.getContents(fgTransferInstance);
    if (importsData == null || importsData.fOriginHandle == null) {
      fOperationTarget.doOperation(fOperationCode);
    }
  }

  private Display getDisplay() {
    Shell shell = getShell();
    if (shell != null) {
      return shell.getDisplay();
    }
    return null;
  }

  private Shell getShell() {
    ITextEditor editor = getTextEditor();
    if (editor != null) {
      IWorkbenchPartSite site = editor.getSite();
      Shell shell = site.getShell();
      if (shell != null && !shell.isDisposed()) {
        return shell;
      }
    }
    return null;
  }

  private boolean isReadOnlyOperation() {
    return fOperationCode == ITextOperationTarget.COPY;
  }
}
