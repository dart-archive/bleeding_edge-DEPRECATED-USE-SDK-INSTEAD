/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.html.ui.internal.hyperlink;

import com.ibm.icu.util.StringTokenizer;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.wst.html.core.internal.provisional.HTML40Namespace;
import org.eclipse.wst.html.core.internal.validate.ModuleCoreSupport;
import org.eclipse.wst.html.ui.internal.HTMLUIMessages;
import org.eclipse.wst.html.ui.internal.HTMLUIPlugin;
import org.eclipse.wst.html.ui.internal.Logger;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.IndexedRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocumentRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegion;
import org.eclipse.wst.sse.core.internal.provisional.text.ITextRegionList;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMModel;
import org.eclipse.wst.xml.core.internal.provisional.document.IDOMNode;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class AnchorHyperlinkDetector extends AbstractHyperlinkDetector {
  static class ExternalElementHyperlink implements IHyperlink {
    private String fAnchorName = null;
    private Element fBaseElement = null;
    private Display fDisplay = null;
    private IRegion fHyperlinkRegion = null;

    /**
     * @param hyperlinkRegion
     * @param anchorName
     */
    public ExternalElementHyperlink(Display display, IRegion hyperlinkRegion, String anchorName,
        Element baseElement) {
      super();
      fDisplay = display;
      fHyperlinkRegion = hyperlinkRegion;
      fAnchorName = anchorName;
      fBaseElement = baseElement;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
     */
    IStatus _open() {
      if (fBaseElement instanceof IDOMNode) {
        StringTokenizer tokenizer = new StringTokenizer(fAnchorName, "#"); //$NON-NLS-1$
        String filename = null;
        String anchorName = null;
        if (tokenizer.hasMoreTokens()) {
          try {
            filename = tokenizer.nextToken();
            anchorName = tokenizer.nextToken();
          } catch (NoSuchElementException e) {
            // poorly formed value
          }
        }
        if (filename != null && anchorName != null) {
          // System.out.println(filename + ":" + anchorName + "-" +
          // fBaseElement);

          IPath basePath = new Path(((IDOMNode) fBaseElement).getModel().getBaseLocation());
          if (basePath.segmentCount() > 1) {
            IPath resolved = ModuleCoreSupport.resolve(basePath, filename);
            IFile targetFile = ResourcesPlugin.getWorkspace().getRoot().getFile(resolved);
            if (targetFile.isAccessible()) {
              IStructuredModel model = null;
              int start = -1;
              int end = -1;
              try {
                model = StructuredModelManager.getModelManager().getModelForRead(targetFile);
                if (model instanceof IDOMModel) {
                  NodeList anchors = ((IDOMModel) model).getDocument().getElementsByTagNameNS(
                      "*", HTML40Namespace.ElementName.A); //$NON-NLS-1$
                  for (int i = 0; i < anchors.getLength() && start < 0; i++) {
                    Node item = anchors.item(i);
                    Node nameNode = item.getAttributes().getNamedItem(
                        HTML40Namespace.ATTR_NAME_NAME);
                    if (nameNode == null)
                      nameNode = item.getAttributes().getNamedItem(HTML40Namespace.ATTR_NAME_ID);
                    if (nameNode != null) {
                      String name = nameNode.getNodeValue();
                      if (anchorName.equals(name) && nameNode instanceof IndexedRegion) {
                        start = ((IndexedRegion) nameNode).getStartOffset();
                        end = ((IndexedRegion) nameNode).getEndOffset();
                      }
                    }
                  }
                  anchors = ((IDOMModel) model).getDocument().getElementsByTagName(
                      HTML40Namespace.ElementName.A);
                  for (int i = 0; i < anchors.getLength() && start < 0; i++) {
                    Node item = anchors.item(i);
                    Node nameNode = item.getAttributes().getNamedItem(
                        HTML40Namespace.ATTR_NAME_NAME);
                    if (nameNode == null)
                      nameNode = item.getAttributes().getNamedItem(HTML40Namespace.ATTR_NAME_ID);
                    if (nameNode != null) {
                      String name = nameNode.getNodeValue();
                      if (anchorName.equals(name) && nameNode instanceof IndexedRegion) {
                        start = ((IndexedRegion) nameNode).getStartOffset();
                        end = ((IndexedRegion) nameNode).getEndOffset();
                      }
                    }
                  }

                  anchors = ((IDOMModel) model).getDocument().getElementsByTagName("*"); //$NON-NLS-1$
                  for (int i = 0; i < anchors.getLength() && start < 0; i++) {
                    Node item = anchors.item(i);
                    Node nameNode = item.getAttributes().getNamedItem(
                        HTML40Namespace.ATTR_NAME_NAME);
                    if (nameNode == null)
                      nameNode = item.getAttributes().getNamedItem(HTML40Namespace.ATTR_NAME_ID);
                    if (nameNode != null) {
                      String name = nameNode.getNodeValue();
                      if (anchorName.equals(name) && nameNode instanceof IndexedRegion) {
                        start = ((IndexedRegion) nameNode).getStartOffset();
                        end = ((IndexedRegion) nameNode).getEndOffset();
                      }
                    }
                  }

                }
                return open(basePath.toString(), targetFile, start, end);
              } catch (Exception e) {
                Logger.logException(e);
                return new Status(IStatus.ERROR, HTMLUIPlugin.ID, e.getMessage());

              } finally {
                if (model != null)
                  model.releaseFromRead();
              }
            }
          }
        }
      }
      return Status.OK_STATUS;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkRegion()
     */
    public IRegion getHyperlinkRegion() {
      return fHyperlinkRegion;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkText()
     */
    public String getHyperlinkText() {
      return NLS.bind(HTMLUIMessages.Open, fAnchorName);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
     */
    public String getTypeLabel() {
      return null;
    }

    public void open() {
      scheduleOpen();
    }

    /**
     * @param targetFile
     * @param start
     * @param end
     */
    private IStatus open(String base, IFile targetFile, int start, int end) throws CoreException,
        PartInitException {
      IMarker temporaryMarker = null;
      try {
        IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

        IEditorPart editor = IDE.openEditor(page, targetFile);

        if (0 <= start && start <= end) {
          temporaryMarker = targetFile.createMarker(IMarker.BOOKMARK);
          temporaryMarker.setAttribute(IMarker.MESSAGE, base);
          temporaryMarker.setAttribute(IMarker.CHAR_START, start);
          temporaryMarker.setAttribute(IMarker.CHAR_END, end);
          IDE.gotoMarker(editor, temporaryMarker);
        }
        return Status.OK_STATUS;
      } finally {
        if (temporaryMarker != null)
          try {
            temporaryMarker.delete();
          } catch (CoreException e) {
            Logger.logException(e);
          }
      }
    }

    void scheduleOpen() {
      Job opener = new UIJob(fDisplay, fAnchorName) {
        public IStatus runInUIThread(IProgressMonitor monitor) {
          return _open();
        }

      };
      opener.setSystem(true);
      opener.setUser(false);
      opener.schedule();
    }
  }

  /**
   * Links to the given target node within the text viewer. The target node is expected to implement
   * IndexedNode and appear in that text viewer (i.e. same editor).
   */
  static class InternalElementHyperlink implements IHyperlink {
    private IRegion fHyperlinkRegion;
    private Node fTarget = null;
    private ITextViewer fViewer = null;

    /**
		 * 
		 */
    public InternalElementHyperlink(ITextViewer textViewer, IRegion hyperlinkRegion, Node targetNode) {
      fHyperlinkRegion = hyperlinkRegion;
      fTarget = targetNode;
      fViewer = textViewer;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkRegion()
     */
    public IRegion getHyperlinkRegion() {
      return fHyperlinkRegion;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#getHyperlinkText()
     */
    public String getHyperlinkText() {
      if (fTarget instanceof IndexedRegion) {
        try {
          int line = fViewer.getDocument().getLineOfOffset(
              ((IndexedRegion) fTarget).getStartOffset()) + 1;
          return NLS.bind(HTMLUIMessages.Hyperlink_line, new String[] {
              fTarget.getNodeName(), fTarget.getNodeValue(), String.valueOf(line)});
        } catch (BadLocationException e) {
          Logger.logException(e);
        }
      }
      return NLS.bind(HTMLUIMessages.Open, fTarget.getNodeName());
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#getTypeLabel()
     */
    public String getTypeLabel() {
      return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.eclipse.jface.text.hyperlink.IHyperlink#open()
     */
    public void open() {
      if (fTarget instanceof IndexedRegion) {
        int offset = ((IndexedRegion) fTarget).getStartOffset();
        if (fViewer instanceof ITextViewerExtension5) {
          offset = ((ITextViewerExtension5) fViewer).modelOffset2WidgetOffset(offset);
        }
        fViewer.getSelectionProvider().setSelection(new TextSelection(offset, 0));
        fViewer.revealRange(offset, 0);
      }
    }
  }

  public AnchorHyperlinkDetector() {
    super();
  }

  private void addHyperLinkForHref(ITextViewer textViewer, IRegion linkRegion, Element element,
      String hrefValue, List links, Node anchor) {
    Node nameNode = anchor.getAttributes().getNamedItem(HTML40Namespace.ATTR_NAME_HREF);
    if (nameNode != null) {
      String name = nameNode.getNodeValue();
      if (hrefValue.equals(name) && nameNode instanceof IndexedRegion) {
        links.add(new InternalElementHyperlink(textViewer, linkRegion, nameNode));
      }
    }
  }

  private void addHyperLinkForName(ITextViewer textViewer, IRegion linkRegion, Element element,
      String anchorName, List links, Node anchor) {
    Node nameNode = anchor.getAttributes().getNamedItem(HTML40Namespace.ATTR_NAME_NAME);
    if (nameNode != null) {
      String name = nameNode.getNodeValue();
      if (anchorName.equals(name) && nameNode instanceof IndexedRegion) {
        links.add(new InternalElementHyperlink(textViewer, linkRegion, nameNode));
      }
    }
    nameNode = anchor.getAttributes().getNamedItem(HTML40Namespace.ATTR_NAME_ID);
    if (nameNode != null) {
      String name = nameNode.getNodeValue();
      if (anchorName.equals(name) && nameNode instanceof IndexedRegion) {
        links.add(new InternalElementHyperlink(textViewer, linkRegion, nameNode));
      }
    }
  }

  /**
   * @param documentRegion
   * @param valueRegion
   * @return
   */
  private IRegion createHyperlinkRegion(IStructuredDocumentRegion documentRegion,
      ITextRegion valueRegion) {
    return new Region(documentRegion.getStartOffset(valueRegion), valueRegion.getTextLength());
  }

  // link to anchors with the given name (value includes the '#')
  IHyperlink[] createHyperlinksToAnchorNamed(ITextViewer textViewer, IRegion hyperlinkRegion,
      Element element, String anchorName, boolean canShowMultipleHyperlinks) {
    List links = new ArrayList(1);
    // >1 guards the substring-ing
    if (anchorName.length() > 1 && anchorName.startsWith("#")) { //$NON-NLS-1$
      // an anchor in this document
      NodeList anchors = null;//element.getOwnerDocument().getElementsByTagNameNS("*", HTML40Namespace.ElementName.A); //$NON-NLS-1$
      String internalAnchorName = anchorName.substring(1);
//			for (int i = 0; i < anchors.getLength(); i++) {
//				addHyperLinkForName(textViewer, hyperlinkRegion, element, internalAnchorName, links, anchors.item(i));
//			}
//			anchors = element.getOwnerDocument().getElementsByTagName(HTML40Namespace.ElementName.A);
//			for (int i = 0; i < anchors.getLength(); i++) {
//				addHyperLinkForName(textViewer, hyperlinkRegion, element, internalAnchorName, links, anchors.item(i));
//			}
      anchors = element.getOwnerDocument().getElementsByTagName("*"); //$NON-NLS-1$
      for (int i = 0; i < anchors.getLength(); i++) {
        addHyperLinkForName(textViewer, hyperlinkRegion, element, internalAnchorName, links,
            anchors.item(i));
      }
    } else {
      // another file, possibly very slow to compute ahead of time
      links.add(new ExternalElementHyperlink(textViewer.getTextWidget().getDisplay(),
          hyperlinkRegion, anchorName, element));
    }
    if (!links.isEmpty()) {
      return (IHyperlink[]) links.toArray(new IHyperlink[links.size()]);
    }
    return null;
  }

  // link to anchors that link to this target
  IHyperlink[] createReferrerHyperlinks(ITextViewer textViewer, IRegion hyperlinkRegion,
      Element element, String nameValue, boolean canShowMultipleHyperlinks) {
    List links = new ArrayList(1);
    if (nameValue.length() > 0) {
      String target = "#" + nameValue; //$NON-NLS-1$
      NodeList anchors = null;//element.getOwnerDocument().getElementsByTagNameNS("*", HTML40Namespace.ElementName.A); //$NON-NLS-1$
//			for (int i = 0; i < anchors.getLength(); i++) {
//				addHyperLinkForHref(textViewer, hyperlinkRegion, element, target, links, anchors.item(i));
//			}
//			anchors = element.getOwnerDocument().getElementsByTagName(HTML40Namespace.ElementName.A);
//			for (int i = 0; i < anchors.getLength(); i++) {
//				addHyperLinkForHref(textViewer, hyperlinkRegion, element, target, links, anchors.item(i));
//			}
      anchors = element.getOwnerDocument().getElementsByTagName("*"); //$NON-NLS-1$
      for (int i = 0; i < anchors.getLength(); i++) {
        addHyperLinkForHref(textViewer, hyperlinkRegion, element, target, links, anchors.item(i));
      }
    }
    if (!links.isEmpty()) {
      return (IHyperlink[]) links.toArray(new IHyperlink[links.size()]);
    }
    return null;
  }

  public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
      boolean canShowMultipleHyperlinks) {
    if (textViewer != null && region != null) {
      IDocument document = textViewer.getDocument();
      if (document != null) {
        Node currentNode = getCurrentNode(document, region.getOffset());
        if (currentNode != null && currentNode.getNodeType() == Node.ELEMENT_NODE) {
          Element element = (Element) currentNode;
          IStructuredDocumentRegion documentRegion = ((IStructuredDocument) document).getRegionAtCharacterOffset(region.getOffset());
          ITextRegion textRegion = documentRegion.getRegionAtCharacterOffset(region.getOffset());
          ITextRegion nameRegion = null;
          ITextRegion valueRegion = null;
          String name = null;
          String value = null;
          if (DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE.equals(textRegion.getType())) {
            ITextRegionList regions = documentRegion.getRegions();
            /*
             * Could use 2, but there needs to be the tag open and name regions
             */
            int index = regions.indexOf(textRegion);
            if (index >= 4) {
              nameRegion = regions.get(index - 2);
              valueRegion = textRegion;
              name = documentRegion.getText(nameRegion);
              value = StringUtils.strip(documentRegion.getText(valueRegion));
            }
          } else if (DOMRegionContext.XML_TAG_ATTRIBUTE_NAME.equals(textRegion.getType())) {
            ITextRegionList regions = documentRegion.getRegions();
            int index = regions.indexOf(textRegion);
            // minus 3 to leave equal and value regions
            if (index <= regions.size() - 3) {
              nameRegion = textRegion;
              valueRegion = regions.get(index + 2);
              name = documentRegion.getText(nameRegion);
              value = StringUtils.strip(documentRegion.getText(valueRegion));
            }
          }
          if (name != null && value != null) {
            int idx = -1;
            if (HTML40Namespace.ATTR_NAME_HREF.equalsIgnoreCase(name)
                && (idx = value.indexOf("#")) >= 0) { //$NON-NLS-1$
              String filename = value.substring(0, idx);
              final String anchorName = idx < value.length() - 1 ? value.substring(idx + 1) : null;
              if (anchorName != null) {
                final IPath basePath = new Path(((IDOMNode) element).getModel().getBaseLocation());
                if (basePath.segmentCount() > 1) {
                  if (filename.length() == 0) {
                    filename = basePath.lastSegment();
                  }
                  final IPath resolved = ModuleCoreSupport.resolve(basePath, filename);
                  final IFile targetFile = ResourcesPlugin.getWorkspace().getRoot().getFile(
                      resolved);
                  if (targetFile.isAccessible())
                    return createHyperlinksToAnchorNamed(textViewer,
                        createHyperlinkRegion(documentRegion, valueRegion), element, value,
                        canShowMultipleHyperlinks);
                }
              }
            }
            if (HTML40Namespace.ATTR_NAME_NAME.equalsIgnoreCase(name)
                || HTML40Namespace.ATTR_NAME_ID.equalsIgnoreCase(name)) {
              return createReferrerHyperlinks(textViewer,
                  createHyperlinkRegion(documentRegion, valueRegion), element, value,
                  canShowMultipleHyperlinks);
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Returns the node the cursor is currently on in the document. null if no node is selected
   * 
   * @param offset
   * @return Node either element, doctype, text, or null
   */
  private Node getCurrentNode(IDocument document, int offset) {
    // get the current node at the offset (returns either: element,
    // doctype, text)
    IndexedRegion inode = null;
    IStructuredModel sModel = null;
    try {
      sModel = StructuredModelManager.getModelManager().getExistingModelForRead(document);
      if (sModel != null) {
        inode = sModel.getIndexedRegion(offset);
        if (inode == null) {
          inode = sModel.getIndexedRegion(offset - 1);
        }
      }
    } finally {
      if (sModel != null)
        sModel.releaseFromRead();
    }

    if (inode instanceof Node) {
      return (Node) inode;
    }
    return null;
  }
}
