package com.google.dart.engine.services.internal.correction;

import java.net.URI;

public class URIUtils {
  /**
   * Computes relative relative path to reference "target" from "base". Uses ".." if needed, in
   * contrast to {@link URI#relativize(URI)}.
   */
  public static String computeRelativePath(String base, String target) {
    // convert to URI separator
    base = base.replaceAll("\\\\", "/");
    target = target.replaceAll("\\\\", "/");
    if (base.startsWith("/") && target.startsWith("/")) {
      base = base.substring(1);
      target = target.substring(1);
    }
    // equal paths - no relative
    if (base.equals(target) == true) {
      return null;
    }
    // split paths
    String[] baseParts = base.split("/");
    String[] targetParts = target.split("/");
    // prepare maximum possible common root length
    int length = baseParts.length < targetParts.length ? baseParts.length : targetParts.length;
    // find common root
    int lastCommonRoot = -1;
    for (int i = 0; i < length; i++) {
      if (baseParts[i].equals(targetParts[i])) {
        lastCommonRoot = i;
      } else {
        break;
      }
    }
    // append ..
    StringBuilder relativePath = new StringBuilder();
    for (int i = lastCommonRoot + 1; i < baseParts.length; i++) {
      if (baseParts[i].length() > 0) {
        relativePath.append("../");
      }
    }
    // append target folder names
    for (int i = lastCommonRoot + 1; i < targetParts.length - 1; i++) {
      String p = targetParts[i];
      relativePath.append(p);
      relativePath.append("/");
    }
    // append target file name
    relativePath.append(targetParts[targetParts.length - 1]);
    // done
    return relativePath.toString();
  }
}
