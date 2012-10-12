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
package com.google.dart.tools.ui.internal.update;

import com.google.common.collect.Lists;
import com.google.dart.tools.update.core.internal.jobs.INIRewriter;

import junit.framework.TestCase;

import static org.junit.Assert.assertArrayEquals;

import java.util.List;

/**
 * Smoke tests for verifying INI rewrite logic.
 */
public class INIRewriteTest extends TestCase {

  public void testInsert() throws Exception {

    List<String> list = Lists.newArrayList("-consoleLog", "-data", "workspace", "-vmargs",
        "-Dosgi.requiredJavaVersion=1.6");

    INIRewriter.insertBefore(list, "-vmargs", "-vm");
    INIRewriter.insertAfter(list, "-vm", "/usr/local/buildtools/java/jdk-64/bin/java");

    assertArrayEquals(new String[] {
        "-consoleLog", "-data", "workspace", "-vm", "/usr/local/buildtools/java/jdk-64/bin/java",
        "-vmargs", "-Dosgi.requiredJavaVersion=1.6"}, list.toArray());

  }

  public void testMergeAgentFlag() throws Exception {
    String[] orig = {
        "-consoleLog", "-data", "workspace", "-vmargs", "-Dosgi.requiredJavaVersion=1.6",
        "-XX:-GoogleAgent"};
    String[] latest = {
        "-consoleLog", "-data", "workspace", "-vmargs", "-Dosgi.requiredJavaVersion=1.6"};
    String[] merged = INIRewriter.merge(orig, latest);

    assertArrayEquals(orig, merged);
  }

  public void testMergeIdentity() throws Exception {
    String[] orig = {
        "-data", "@noDefault", "-consoleLog", "-vmargs", "-Dosgi.requiredJavaVersion=1.6",
        "-XX:MaxPermSize=256m", "-Xms40m", "-Xmx1024m", "-XstartOnFirstThread",
        "-Dorg.eclipse.swt.internal.carbon.smallFonts",
        "-Declipse.vm=/System/Library/Frameworks/JavaVM.framework",
        "-Xdock:icon=../Resources/dart.icns",
        "-Dcom.dart.tools.update.core.url=http://gsdview.appspot.com/dart-editor-archive-continuous/"};
    String[] merged = INIRewriter.merge(orig, orig);

    assertArrayEquals(orig, merged);
  }

  public void testMergeVM() throws Exception {
    String[] orig = {
        "-consoleLog", "-data", "workspace", "-vm", "/usr/local/buildtools/java/jdk-64/bin/java",
        "-vmargs", "-Dosgi.requiredJavaVersion=1.6"};
    String[] latest = {
        "-consoleLog", "-data", "workspace", "-vmargs", "-Dosgi.requiredJavaVersion=1.6"};
    String[] merged = INIRewriter.merge(orig, latest);

    assertArrayEquals(orig, merged);
  }

  public void testOverwriteUserVMargs() throws Exception {
    String[] orig = {
        "-data", "@noDefault", "-consoleLog", "-vmargs", "-Dosgi.requiredJavaVersion=1.6",
        "-XX:MaxPermSize=256m", "-Xms40m", "-Xmx1024m", "-XstartOnFirstThread",
        "-Dorg.eclipse.swt.internal.carbon.smallFonts",
        "-Declipse.vm=/System/Library/Frameworks/JavaVM.framework",
        "-Xdock:icon=../Resources/dart.icns",
        "-Dcom.dart.tools.update.core.url=http://gsdview.appspot.com/dart-editor-archive-continuous/"};
    String[] updated = {
        "-data", "@noDefault", "-consoleLog", "-vmargs", "-Dosgi.requiredJavaVersion=1.6",
        "-XX:MaxPermSize=512m", "-Xms64m", "-Xmx1024m", "-XstartOnFirstThread",
        "-Dorg.eclipse.swt.internal.carbon.smallFonts",
        "-Declipse.vm=/System/Library/Frameworks/JavaVM.framework",
        "-Xdock:icon=../Resources/dart.icns"};
    String[] merged = INIRewriter.merge(orig, updated);

    assertArrayEquals(updated, merged);
  }

}
