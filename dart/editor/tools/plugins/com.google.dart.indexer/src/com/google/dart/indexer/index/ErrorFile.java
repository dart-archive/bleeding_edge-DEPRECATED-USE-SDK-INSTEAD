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
package com.google.dart.indexer.index;

// Copyright 2009-2010 Google Inc. All Rights Reserved.

// package com.instantiations.eclipse.analysis.indexer.index;
//
// import java.io.DataInputStream;
// import java.io.DataOutputStream;
// import java.io.File;
// import java.io.FileInputStream;
// import java.io.FileOutputStream;
// import java.io.IOException;
//
// import org.eclipse.core.resources.IFile;
// import org.eclipse.core.runtime.IPath;
// import org.eclipse.core.runtime.Path;
//
// import
// com.instantiations.eclipse.analysis.indexer.exceptions.IndexRequiresFullRebuild;
// import
// com.instantiations.eclipse.analysis.indexer.exceptions.IndexTemporarilyNonOperational;
// import
// com.instantiations.eclipse.analysis.indexer.index.configuration.IndexConfigurationInstance;
// import
// com.instantiations.eclipse.analysis.indexer.index.diskstorage.IndexFolder;
//
// /**
// * Provides utility method to read and write a file that describes the index
// * configuration that the index uses.
// */
public class ErrorFile {
  //
  // private static final String ENCODING = "utf-8";
  //
  // /**
  // * Checks that the index configuration file exists in the given folder and
  // * represents exactly the given index configuration.
  // */
  // public static IPath[] read(IndexFolder folder,
  // IndexConfigurationInstance configuration)
  // throws IndexRequiresFullRebuild {
  // File versionFile = getFile(folder);
  // try {
  // FileInputStream in = new FileInputStream(versionFile);
  // DataInputStream ds=new DataInputStream(in);
  // try {
  // int count=ds.readInt();
  // IPath[] result=new IPath[count];
  // for (int a=0;a<count;a++){
  // result[a]=new Path(ds.readUTF());
  // }
  // return result;
  // } finally {
  // in.close();
  // }
  // } catch (IOException e) {
  // throw new IndexRequiresFullRebuild("Error reading index version", e);
  // }
  // }
  //
  // /**
  // * Creates or overwrites an index configuration file in the given folder, so
  // * that it will correspond to the given index configuration.
  // */
  // public static void write(IndexFolder folder,
  // IndexConfigurationInstance configuration,IFile[] files)
  // throws IndexTemporarilyNonOperational {
  // folder.getFolder().mkdirs();
  // File versionFile = getFile(folder);
  // try {
  // FileOutputStream out = new FileOutputStream(versionFile);
  //
  // try {
  // DataOutputStream ds=new DataOutputStream(out);
  // ds.writeInt(files.length);
  // for (int a=0;a<files.length;a++){
  // ds.writeUTF(files[a].getFullPath().toString());
  // }
  // } finally {
  // out.close();
  // }
  // } catch (IOException e) {
  // throw new IndexTemporarilyNonOperational(
  // "Error writing index version", e);
  // }
  // }
  //
  // private static File getFile(IndexFolder folder) {
  // File versionFile = folder.byLocalPath("errors-info");
  // return versionFile;
  // }
  //
}
