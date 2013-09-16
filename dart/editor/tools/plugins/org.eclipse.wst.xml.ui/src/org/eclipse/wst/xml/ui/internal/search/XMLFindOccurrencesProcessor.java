/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/

package org.eclipse.wst.xml.ui.internal.search;

import org.eclipse.wst.sse.ui.internal.search.FindOccurrencesProcessor;
import org.eclipse.wst.xml.core.internal.regions.DOMRegionContext;
import org.eclipse.wst.xml.core.text.IXMLPartitions;

/**
 * Configures a FindOccurrencesProcessor with XML partitions and regions
 */
public class XMLFindOccurrencesProcessor extends FindOccurrencesProcessor {

  protected String[] getPartitionTypes() {
    return new String[] {IXMLPartitions.XML_DEFAULT};
  }

  protected String[] getRegionTypes() {
    return new String[] {
        DOMRegionContext.XML_TAG_NAME, DOMRegionContext.XML_TAG_ATTRIBUTE_NAME,
        DOMRegionContext.XML_TAG_ATTRIBUTE_VALUE};
  }
}
