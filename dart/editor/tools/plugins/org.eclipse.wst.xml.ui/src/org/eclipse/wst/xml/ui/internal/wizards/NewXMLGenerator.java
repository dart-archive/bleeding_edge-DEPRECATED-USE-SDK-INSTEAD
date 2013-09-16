/*******************************************************************************
 * Copyright (c) 2001, 2011 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.wizards;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.util.Assert;
import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolver;
import org.eclipse.wst.common.uriresolver.internal.provisional.URIResolverPlugin;
import org.eclipse.wst.common.uriresolver.internal.util.URIHelper;
import org.eclipse.wst.sse.core.internal.encoding.CommonEncodingPreferenceNames;
import org.eclipse.wst.sse.core.utils.StringUtils;
import org.eclipse.wst.xml.core.internal.XMLCorePlugin;
import org.eclipse.wst.xml.core.internal.catalog.provisional.ICatalogEntry;
import org.eclipse.wst.xml.core.internal.contentmodel.CMDocument;
import org.eclipse.wst.xml.core.internal.contentmodel.CMElementDeclaration;
import org.eclipse.wst.xml.core.internal.contentmodel.CMNamedNodeMap;
import org.eclipse.wst.xml.core.internal.contentmodel.ContentModelManager;
import org.eclipse.wst.xml.core.internal.contentmodel.util.ContentBuilder;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMContentBuilderImpl;
import org.eclipse.wst.xml.core.internal.contentmodel.util.DOMWriter;
import org.eclipse.wst.xml.core.internal.contentmodel.util.NamespaceInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class NewXMLGenerator {

  protected String grammarURI;
  protected CMDocument cmDocument;
  protected int buildPolicy;
  protected String rootElementName;

  protected ICatalogEntry xmlCatalogEntry;
  protected int optionalElementDepthLimit = -1;

  // info for dtd
  protected String publicId;
  protected String systemId;
  protected String defaultSystemId;

  // info for xsd
  public List namespaceInfoList;

  public NewXMLGenerator() {
    super();
  }

  public NewXMLGenerator(String grammarURI, CMDocument cmDocument) {
    this.grammarURI = grammarURI;
    this.cmDocument = cmDocument;
  }

  public static CMDocument createCMDocument(String uri, String[] errorInfo) {
    String title = null;
    String message = null;
    List errorList = new Vector();
    CMDocument cmDocument = null;

    if (URIHelper.isReadableURI(uri, true)) {
      // (cs) assume the uri has been provided in a normalized form
      cmDocument = ContentModelManager.getInstance().createCMDocument(uri, null);

      if (uri.endsWith(".dtd")) { //$NON-NLS-1$
        if (errorList.size() > 0) {
          title = XMLWizardsMessages._UI_INVALID_GRAMMAR_ERROR;
          message = XMLWizardsMessages._UI_LABEL_ERROR_DTD_INVALID_INFO;
        }
      } else // ".xsd"
      {
        // To be consistent with the schema editor validation
        XMLSchemaValidationChecker validator = new XMLSchemaValidationChecker();
        if (!validator.isValid(uri)) {
          title = XMLWizardsMessages._UI_INVALID_GRAMMAR_ERROR;
          message = XMLWizardsMessages._UI_LABEL_ERROR_SCHEMA_INVALID_INFO;
        } else if (cmDocument != null) {
          int globalElementCount = cmDocument.getElements().getLength();
          if (globalElementCount == 0) {
            title = XMLWizardsMessages._UI_WARNING_TITLE_NO_ROOT_ELEMENTS;
            message = XMLWizardsMessages._UI_WARNING_MSG_NO_ROOT_ELEMENTS;
          }
        }
      }
    } else {
      title = XMLWizardsMessages._UI_WARNING_TITLE_NO_ROOT_ELEMENTS;
      message = XMLWizardsMessages._UI_WARNING_URI_NOT_FOUND_COLON + " " + uri; //$NON-NLS-1$
    }
    errorInfo[0] = title;
    errorInfo[1] = message;

    return cmDocument;
  }

  private String applyLineDelimiter(IFile file, String text) {
    String systemLineSeparator = System.getProperty("line.separator");
    String lineDelimiter = Platform.getPreferencesService().getString(Platform.PI_RUNTIME,
        Platform.PREF_LINE_SEPARATOR, systemLineSeparator,
        new IScopeContext[] {new ProjectScope(file.getProject()), new InstanceScope()});//$NON-NLS-1$
    if (!systemLineSeparator.equals(lineDelimiter)) {
      String convertedText = StringUtils.replace(text, "\r\n", "\n");
      convertedText = StringUtils.replace(convertedText, "\r", "\n");
      convertedText = StringUtils.replace(convertedText, "\n", lineDelimiter);
      return convertedText;
    }
    return text;
  }

  /**
   * @deprecated use createTemplateXMLDocument(IFile, String) instead
   */
  public void createEmptyXMLDocument(IFile newFile) throws Exception {
    String charSet = getUserPreferredCharset();
    String contents = "<?xml version=\"1.0\" encoding=\"" + charSet + "\"?>"; //$NON-NLS-1$ //$NON-NLS-2$
    createTemplateXMLDocument(newFile, contents);
  }

  void createTemplateXMLDocument(IFile newFile, String contents) throws Exception {
    if (contents != null) {
      String charSet = getUserPreferredCharset();
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

      PrintWriter writer = new PrintWriter(new OutputStreamWriter(outputStream, charSet));
      contents = applyLineDelimiter(newFile, contents);
      writer.print(contents);
      writer.flush();
      outputStream.close();

      ByteArrayInputStream inputStream = new ByteArrayInputStream(outputStream.toByteArray());
      newFile.setContents(inputStream, true, true, null);
      inputStream.close();
    }
  }

  private String getUserPreferredCharset() {
    Preferences preference = XMLCorePlugin.getDefault().getPluginPreferences();
    String charSet = preference.getString(CommonEncodingPreferenceNames.OUTPUT_CODESET);
    return charSet;
  }

  public void createXMLDocument(String xmlFileName) throws Exception {
    IContentType contentType = Platform.getContentTypeManager().findContentTypeFor(xmlFileName);
    String charset = null;
    if (contentType != null) {
      charset = contentType.getDefaultCharset();
    }
    ByteArrayOutputStream outputStream = createXMLDocument(xmlFileName, charset);

    File file = new File(xmlFileName);
    FileOutputStream fos = new FileOutputStream(file);
    outputStream.writeTo(fos);
    fos.close();
  }

  public void createXMLDocument(IFile newFile, String xmlFileName) throws Exception {
    String charset = newFile.getCharset();
    ByteArrayOutputStream outputStream = createXMLDocument(xmlFileName, charset);

    String contents = outputStream.toString(charset);
    contents = applyLineDelimiter(newFile, contents);

    ByteArrayInputStream inputStream = new ByteArrayInputStream(contents.getBytes(charset));
    newFile.setContents(inputStream, true, true, null);
    inputStream.close();
  }

  public ByteArrayOutputStream createXMLDocument(String xmlFileName, String charset)
      throws Exception {
    if (charset == null) {
      charset = getUserPreferredCharset();
      if (charset == null) {
        charset = "UTF-8"; //$NON-NLS-1$
      }
    }
    CMDocument cmDocument = getCMDocument();

    Assert.isNotNull(cmDocument);
    Assert.isNotNull(getRootElementName());

    // create the xml model
    CMNamedNodeMap nameNodeMap = cmDocument.getElements();
    CMElementDeclaration cmElementDeclaration = (CMElementDeclaration) nameNodeMap.getNamedItem(getRootElementName());

    Document xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
    DOMContentBuilderImpl contentBuilder = new DOMContentBuilderImpl(xmlDocument);

    // this 'uglyTempHack' flag is required in order to supress the
    // creation a default encoding
    // we'll handle this later in the domWriter.print() method used below
    //
    contentBuilder.supressCreationOfDoctypeAndXMLDeclaration = true;
    contentBuilder.setBuildPolicy(buildPolicy);
    contentBuilder.setOptionalElementDepthLimit(optionalElementDepthLimit);
    contentBuilder.setExternalCMDocumentSupport(new MyExternalCMDocumentSupport(namespaceInfoList,
        xmlFileName));
    contentBuilder.createDefaultRootContent(cmDocument, cmElementDeclaration, namespaceInfoList);

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream, charset);

    DOMWriter domWriter = new DOMWriter(outputStreamWriter);

    // TODO... instead of relying on file extensions, we need to keep
    // track of the grammar type
    // better yet we should reate an SSE document so that we can format it
    // nicely before saving
    // then we won't need the DOMWriter at all
    //
    domWriter.print(xmlDocument, charset, cmDocument.getNodeName(),
        getNonWhitespaceString(getPublicId()), getNonWhitespaceString(getSystemId()));
    outputStream.flush();
    outputStream.close();

    return outputStream;
  }

  public void createNamespaceInfoList() {
    List result = new Vector();
    if (cmDocument != null) {
      result = (List) cmDocument.getProperty("http://org.eclipse.wst/cm/properties/completeNamespaceInfo"); //$NON-NLS-1$
      if (result != null) {
        int size = result.size();
        for (int i = 0; i < size; i++) {
          NamespaceInfo info = (NamespaceInfo) result.get(i);
          if (i == 0) {
            String locationInfo = null;
            if (xmlCatalogEntry != null) {
              if (xmlCatalogEntry.getEntryType() == ICatalogEntry.ENTRY_TYPE_PUBLIC) {
                locationInfo = xmlCatalogEntry.getAttributeValue(ICatalogEntry.ATTR_WEB_URL);
              } else {
                locationInfo = xmlCatalogEntry.getKey();
              }
            }
            if (locationInfo == null) {
              locationInfo = defaultSystemId;
            }
            info.locationHint = locationInfo;
            info.setProperty("locationHint-readOnly", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            info.setProperty("uri-readOnly", "true"); //$NON-NLS-1$ //$NON-NLS-2$
            info.setProperty("unremovable", "true"); //$NON-NLS-1$ //$NON-NLS-2$
          } else {
            info.locationHint = null;
          }
        }
      }

      NamespaceInfoContentBuilder builder = new NamespaceInfoContentBuilder();
      builder.setBuildPolicy(ContentBuilder.BUILD_ONLY_REQUIRED_CONTENT);
      builder.visitCMNode(cmDocument);
      result.addAll(builder.list);
    }
    namespaceInfoList = result;
  }

  public boolean isMissingNamespaceLocation() {
    boolean result = false;
    for (Iterator i = namespaceInfoList.iterator(); i.hasNext();) {
      NamespaceInfo info = (NamespaceInfo) i.next();
      if (info.locationHint == null) {
        result = true;
        break;
      }
    }
    return result;
  }

  public String[] getNamespaceInfoErrors() {
    String[] errorList = null;

// No warnings should be given when namespaces entries have missing location hints
// See https://bugs.eclipse.org/bugs/show_bug.cgi?id=105128		
//		if ((namespaceInfoList != null) && isMissingNamespaceLocation()) {
//			String title = XMLWizardsMessages._UI_LABEL_NO_LOCATION_HINT;
//			String message = XMLWizardsMessages._UI_WARNING_MSG_NO_LOCATION_HINT_1 + " " + XMLWizardsMessages._UI_WARNING_MSG_NO_LOCATION_HINT_2 + "\n\n" + XMLWizardsMessages._UI_WARNING_MSG_NO_LOCATION_HINT_3; //$NON-NLS-1$ //$NON-NLS-2$
//
//			errorList = new String[2];
//			errorList[0] = title;
//			errorList[1] = message;
//		}
    return errorList;
  }

  public void setXMLCatalogEntry(ICatalogEntry catalogEntry) {
    xmlCatalogEntry = catalogEntry;
  }

  public ICatalogEntry getXMLCatalogEntry() {
    return xmlCatalogEntry;
  }

  public void setBuildPolicy(int policy) {
    buildPolicy = policy;
  }

  public void setDefaultSystemId(String sysId) {
    defaultSystemId = sysId;
  }

  public String getDefaultSystemId() {
    return defaultSystemId;
  }

  public void setSystemId(String sysId) {
    systemId = sysId;
  }

  public String getSystemId() {
    return systemId;
  }

  public void setPublicId(String pubId) {
    publicId = pubId;
  }

  public String getPublicId() {
    return publicId;
  }

  public void setGrammarURI(String gramURI) {
    grammarURI = gramURI;
  }

  public String getGrammarURI() {
    return grammarURI;
  }

  public void setCMDocument(CMDocument cmDoc) {
    cmDocument = cmDoc;
  }

  public CMDocument getCMDocument() {
    return cmDocument;
  }

  public void setRootElementName(String rootName) {
    rootElementName = rootName;
  }

  public String getRootElementName() {
    return rootElementName;
  }

  protected class MyExternalCMDocumentSupport implements
      DOMContentBuilderImpl.ExternalCMDocumentSupport {
    protected List namespaceInfoList1;
    protected URIResolver idResolver;
    protected String resourceLocation;

    protected MyExternalCMDocumentSupport(List namespaceInfoListParam, String resourceLocation) {
      this.namespaceInfoList1 = namespaceInfoListParam;
      this.resourceLocation = resourceLocation;
      idResolver = URIResolverPlugin.createResolver();
    }

    public CMDocument getCMDocument(Element element, String namespaceURI) {
      CMDocument result = null;
      if ((namespaceURI != null) && (namespaceURI.trim().length() > 0)) {
        String locationHint = null;
        for (Iterator i = namespaceInfoList1.iterator(); i.hasNext();) {
          NamespaceInfo info = (NamespaceInfo) i.next();
          if (namespaceURI.equals(info.uri)) {
            locationHint = info.locationHint;
            break;
          }
        }
        if (locationHint != null) {
          grammarURI = idResolver.resolve(resourceLocation, locationHint, locationHint);
          result = ContentModelManager.getInstance().createCMDocument(getGrammarURI(), null);
        }
      } else {
        result = cmDocument;
      }
      return result;
    }
  }

  public static String getNonWhitespaceString(String string) {
    String result = null;
    if (string != null) {
      if (string.trim().length() > 0) {
        result = string;
      }
    }
    return result;
  }

  public void setOptionalElementDepthLimit(int optionalElementDepthLimit) {
    this.optionalElementDepthLimit = optionalElementDepthLimit;
  }
}
