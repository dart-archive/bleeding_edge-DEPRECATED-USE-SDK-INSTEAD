/*******************************************************************************
 * Copyright (c) 2005, 2007 IBM Corporation and others. All rights reserved. This program and the
 * accompanying materials are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html Contributors: IBM Corporation - initial API and
 * implementation
 *******************************************************************************/
package org.eclipse.wst.xml.ui.internal.catalog;

import org.eclipse.wst.common.uriresolver.internal.util.URIHelper;

public class URIUtils {

  private static final String PROTOCOL_PATTERN = ":";
  private static final String FILE_PROTOCOL = "file:";
  private static final String PLATFORM_RESOURCE_PROTOCOL = "platform:/resource/";
  private static final String LOCAL_FILE_PROTOCOL_FORWARD_SLASH = "\\\\\\";
  private static final String LOCAL_FILE_PROTOCOL_BACK_SLASH = "///";
  private static final char PATH_SEPARATOR_FORWARD_SLASH = '/';
  private static final char PATH_SEPARATOR_BACK_SLASH = '\\';

  public static String convertURIToLocation(String uri) {
    String location = uri;
    if (uri != null) {
      if (uri.startsWith(FILE_PROTOCOL)) {
        location = org.eclipse.wst.common.uriresolver.internal.URI.createURI(uri).toFileString();
        if (location != null
            && (location.startsWith(LOCAL_FILE_PROTOCOL_BACK_SLASH) || location.startsWith(LOCAL_FILE_PROTOCOL_FORWARD_SLASH))) {
          location = location.substring(LOCAL_FILE_PROTOCOL_BACK_SLASH.length());
        }
      } else if (uri.startsWith(PLATFORM_RESOURCE_PROTOCOL)) {
        location = uri.substring(PLATFORM_RESOURCE_PROTOCOL.length());
      }
    }
    return location;
  }

  public static String convertLocationToURI(String location) {
    String uri = location;
    if (!URIHelper.hasProtocol(location)) {
      uri = URIHelper.isAbsolute(location)
          ? org.eclipse.wst.common.uriresolver.internal.URI.createFileURI(location).toString()
          : URIHelper.prependPlatformResourceProtocol(location);
    }
    if (uri.startsWith(FILE_PROTOCOL)
        && uri.indexOf(PROTOCOL_PATTERN, FILE_PROTOCOL.length()) != -1) {
      uri = URIHelper.ensureFileURIProtocolFormat(uri);
    }
    uri = uri.replace(PATH_SEPARATOR_BACK_SLASH, PATH_SEPARATOR_FORWARD_SLASH);
    return uri;
  }

}
