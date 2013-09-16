/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.css.ui.internal.hyperlink;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.hyperlink.AbstractHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlink;
import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolverPlugin;
import org.eclipse.wst.css.core.internal.document.CSSRegionContainer;
import org.eclipse.wst.css.core.internal.provisional.document.ICSSNode;
import org.eclipse.wst.sse.core.StructuredModelManager;
import org.eclipse.wst.sse.core.internal.provisional.IStructuredModel;
import org.eclipse.wst.sse.core.internal.provisional.text.IStructuredDocument;
import org.w3c.dom.css.CSSImportRule;
import org.w3c.dom.css.CSSPrimitiveValue;

import java.io.File;
import java.net.URI;

/**
 * Detects hyperlink regions within CSS documents. This includes url() methods as well as the
 * resource referred to by @import
 */
public class CSSHyperlinkDetector extends AbstractHyperlinkDetector {

  /*
   * (non-Javadoc)
   * 
   * @see
   * org.eclipse.jface.text.hyperlink.IHyperlinkDetector#detectHyperlinks(org.eclipse.jface.text
   * .ITextViewer, org.eclipse.jface.text.IRegion, boolean)
   */
  public IHyperlink[] detectHyperlinks(ITextViewer textViewer, IRegion region,
      boolean canShowMultipleHyperlinks) {
    if (textViewer != null && region != null) {

      final IDocument document = textViewer.getDocument();
      final ICSSNode node = getNode(document, region);

      if (node == null) {
        return null;
      }
      String href = null;
      switch (node.getNodeType()) {
        case ICSSNode.PRIMITIVEVALUE_NODE:
          if (((CSSPrimitiveValue) node).getPrimitiveType() == CSSPrimitiveValue.CSS_URI) {
            href = ((CSSPrimitiveValue) node).getStringValue();
          }
          break;
        case ICSSNode.IMPORTRULE_NODE:
          href = ((CSSImportRule) node).getHref();
          break;
      }
      if (href != null) {
        final IHyperlink hyperlink = getHyperlink(node, href);
        if (hyperlink != null) {
          return new IHyperlink[] {hyperlink};
        }
      }

    }
    return null;
  }

  private ICSSNode getNode(IDocument document, IRegion region) {
    if (!(document instanceof IStructuredDocument))
      return null;

    IStructuredModel model = null;
    ICSSNode node = null;
    try {
      model = StructuredModelManager.getModelManager().getModelForRead(
          (IStructuredDocument) document);
      node = (ICSSNode) model.getIndexedRegion(region.getOffset());
    } finally {
      if (model != null) {
        model.releaseFromRead();
      }
    }
    return node;
  }

  private IHyperlink getHyperlink(ICSSNode node, String href) {
    IHyperlink hyperlink = null;
    final String baseLocation = getBaseLocation(node.getOwnerDocument().getModel());
    if (baseLocation != null) {
      final String resolvedHref = URIResolverPlugin.createResolver().resolve(baseLocation, null,
          href);
      if (resolvedHref != null && isValidURI(resolvedHref)) {
        final IRegion hyperlinkRegion = getHyperlinkRegion(node, href);
        hyperlink = createHyperlink(resolvedHref, hyperlinkRegion);
      }
    }

    return hyperlink;
  }

  private IRegion getHyperlinkRegion(ICSSNode node, String href) {
    CSSRegionContainer uriRegion = null;
    switch (node.getNodeType()) {
      case ICSSNode.PRIMITIVEVALUE_NODE:
        uriRegion = (CSSRegionContainer) node;
        break;
      case ICSSNode.IMPORTRULE_NODE:
        ICSSNode attribute = node.getAttributes().getNamedItem("href"); //$NON-NLS-1$
        if (attribute instanceof CSSRegionContainer) {
          uriRegion = (CSSRegionContainer) attribute;
        }
        break;
    }
    if (uriRegion != null) {
      final int start = uriRegion.getStartOffset();
      final int end = uriRegion.getEndOffset();
      if (end > start)
        return new Region(start, end - start);
    }
    return null;
  }

  private IHyperlink createHyperlink(String href, IRegion region) {
    IHyperlink link = null;
    // try to locate the file in the workspace
    File systemFile = getFileFromUriString(href);
    if (systemFile != null) {
      String systemPath = systemFile.getPath();
      IFile file = getFile(systemPath);
      if (file != null) {
        // this is a WorkspaceFileHyperlink since file exists in
        // workspace
        link = new WorkspaceFileHyperlink(region, file);
      }
    }
    return link;
  }

  /**
   * Returns an IFile from the given uri if possible, null if cannot find file from uri.
   * 
   * @param fileString file system path
   * @return returns IFile if fileString exists in the workspace
   */
  private IFile getFile(String fileString) {
    IFile file = null;

    if (fileString != null) {
      Path filePath = new Path(fileString);
      if (filePath.segmentCount() > 1
          && ResourcesPlugin.getWorkspace().getRoot().getFile(filePath).exists()) {
        return ResourcesPlugin.getWorkspace().getRoot().getFile(filePath);
      }
      IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocation(filePath);
      for (int i = 0; (i < files.length) && (file == null); i++) {
        if (files[i].exists()) {
          file = files[i];
        }
      }
    }

    return file;
  }

  /**
   * Checks whether the given uriString is really pointing to a file
   * 
   * @param uriString
   * @return boolean
   */
  private boolean isValidURI(String uriString) {
    boolean isValid = false;
    File file = getFileFromUriString(uriString);

    if (file != null) {
      isValid = file.isFile();
    }

    return isValid;
  }

  /**
   * Create a file from the given uri string
   * 
   * @param uriString - assumes uriString is not http://
   * @return File created from uriString if possible, null otherwise
   */
  private File getFileFromUriString(String uriString) {
    File file = null;
    try {
      // first just try to create a file directly from uriString as
      // default in case create file from uri does not work
      file = new File(uriString);

      // try to create file from uri
      URI uri = new URI(uriString);
      file = new File(uri);
    } catch (Exception e) {
      // if exception is thrown while trying to create File just ignore
      // and file will be null
    }
    return file;
  }

  /**
   * Get the base location from the current model (local file system)
   */
  private String getBaseLocation(IStructuredModel model) {
    String result = null;

    // get the base location from the current model
    if (model != null) {
      result = model.getBaseLocation();

      IPath path = new Path(result);
      if (path.segmentCount() > 1) {
        IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
        if (file.exists()) {
          String baseLocation = null;
          if (file.getLocation() != null) {
            baseLocation = file.getLocation().toString();
          }
          if (baseLocation == null && file.getLocationURI() != null) {
            baseLocation = file.getLocationURI().toString();
          }
          if (baseLocation == null) {
            baseLocation = file.getFullPath().toString();
          }
          result = baseLocation;
        }
      }
    }
    return result;
  }
}
