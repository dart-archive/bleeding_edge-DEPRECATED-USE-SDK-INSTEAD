// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// This code was auto-generated, is not intended to be edited, and is subject to
// significant change. Please see the README file for more information.

library engine.source.io;

import 'source.dart';
import 'java_core.dart';
import 'java_io.dart';
import 'utilities_general.dart';
import 'engine.dart';
export 'source.dart';

/**
 * Instances of interface `LocalSourcePredicate` are used to determine if the given
 * [Source] is "local" in some sense, so can be updated.
 */
abstract class LocalSourcePredicate {
  /**
   * Instance of [LocalSourcePredicate] that always returns `false`.
   */
  static final LocalSourcePredicate FALSE = new LocalSourcePredicate_FALSE();

  /**
   * Instance of [LocalSourcePredicate] that always returns `true`.
   */
  static final LocalSourcePredicate TRUE = new LocalSourcePredicate_TRUE();

  /**
   * Instance of [LocalSourcePredicate] that returns `true` for all [Source]s
   * except of SDK.
   */
  static final LocalSourcePredicate NOT_SDK = new LocalSourcePredicate_NOT_SDK();

  /**
   * Determines if the given [Source] is local.
   *
   * @param source the [Source] to analyze
   * @return `true` if the given [Source] is local
   */
  bool isLocal(Source source);
}

class LocalSourcePredicate_FALSE implements LocalSourcePredicate {
  @override
  bool isLocal(Source source) => false;
}

class LocalSourcePredicate_TRUE implements LocalSourcePredicate {
  @override
  bool isLocal(Source source) => true;
}

class LocalSourcePredicate_NOT_SDK implements LocalSourcePredicate {
  @override
  bool isLocal(Source source) => source.uriKind != UriKind.DART_URI;
}

/**
 * Instances of the class `FileBasedSource` implement a source that represents a file.
 */
class FileBasedSource implements Source {
  /**
   * The file represented by this source.
   */
  final JavaFile file;

  /**
   * The cached encoding for this source.
   */
  String _encoding;

  /**
   * The kind of URI from which this source was originally derived.
   */
  final UriKind uriKind;

  /**
   * Initialize a newly created source object. The source object is assumed to not be in a system
   * library.
   *
   * @param file the file represented by this source
   */
  FileBasedSource.con1(JavaFile file) : this.con2(file, UriKind.FILE_URI);

  /**
   * Initialize a newly created source object.
   *
   * @param file the file represented by this source
   * @param flags `true` if this source is in one of the system libraries
   */
  FileBasedSource.con2(this.file, this.uriKind);

  @override
  bool operator ==(Object object) => object != null && this.runtimeType == object.runtimeType && file == (object as FileBasedSource).file;

  @override
  bool exists() => file.isFile();

  @override
  TimestampedData<String> get contents {
    TimeCounter_TimeCounterHandle handle = PerformanceStatistics.io.start();
    try {
      return contentsFromFile;
    } finally {
      handle.stop();
    }
  }

  @override
  String get encoding {
    if (_encoding == null) {
      _encoding = "${uriKind.encoding}${file.toURI().toString()}";
    }
    return _encoding;
  }

  @override
  String get fullName => file.getAbsolutePath();

  @override
  int get modificationStamp => file.lastModified();

  @override
  String get shortName => file.getName();

  @override
  int get hashCode => file.hashCode;

  @override
  bool get isInSystemLibrary => uriKind == UriKind.DART_URI;

  @override
  Source resolveRelative(Uri containedUri) {
    try {
      Uri resolvedUri = file.toURI().resolveUri(containedUri);
      return new FileBasedSource.con2(new JavaFile.fromUri(resolvedUri), uriKind);
    } on JavaException catch (exception) {
    }
    return null;
  }

  @override
  String toString() {
    if (file == null) {
      return "<unknown source>";
    }
    return file.getAbsolutePath();
  }

  /**
   * Get the contents and timestamp of the underlying file.
   *
   * Clients should consider using the the method [AnalysisContext#getContents]
   * because contexts can have local overrides of the content of a source that the source is not
   * aware of.
   *
   * @return the contents of the source paired with the modification stamp of the source
   * @throws Exception if the contents of this source could not be accessed
   * @see #getContents()
   */
  TimestampedData<String> get contentsFromFile {
    return new TimestampedData<String>(file.lastModified(), file.readAsStringSync());
  }
}

/**
 * Instances of the class `PackageUriResolver` resolve `package` URI's in the context of
 * an application.
 *
 * For the purposes of sharing analysis, the path to each package under the "packages" directory
 * should be canonicalized, but to preserve relative links within a package, the remainder of the
 * path from the package directory to the leaf should not.
 */
class PackageUriResolver extends UriResolver {
  /**
   * The package directories that `package` URI's are assumed to be relative to.
   */
  final List<JavaFile> _packagesDirectories;

  /**
   * The name of the `package` scheme.
   */
  static String PACKAGE_SCHEME = "package";

  /**
   * Log exceptions thrown with the message "Required key not available" only once.
   */
  static bool _CanLogRequiredKeyIoException = true;

  /**
   * Return `true` if the given URI is a `package` URI.
   *
   * @param uri the URI being tested
   * @return `true` if the given URI is a `package` URI
   */
  static bool isPackageUri(Uri uri) => PACKAGE_SCHEME == uri.scheme;

  /**
   * Initialize a newly created resolver to resolve `package` URI's relative to the given
   * package directories.
   *
   * @param packagesDirectories the package directories that `package` URI's are assumed to be
   *          relative to
   */
  PackageUriResolver(this._packagesDirectories) {
    if (_packagesDirectories.length < 1) {
      throw new IllegalArgumentException("At least one package directory must be provided");
    }
  }

  @override
  Source fromEncoding(UriKind kind, Uri uri) {
    if (kind == UriKind.PACKAGE_SELF_URI || kind == UriKind.PACKAGE_URI) {
      return new FileBasedSource.con2(new JavaFile.fromUri(uri), kind);
    }
    return null;
  }

  @override
  Source resolveAbsolute(Uri uri) {
    if (!isPackageUri(uri)) {
      return null;
    }
    String path = uri.path;
    if (path == null) {
      path = uri.path;
      if (path == null) {
        return null;
      }
    }
    String pkgName;
    String relPath;
    int index = path.indexOf('/');
    if (index == -1) {
      // No slash
      pkgName = path;
      relPath = "";
    } else if (index == 0) {
      // Leading slash is invalid
      return null;
    } else {
      // <pkgName>/<relPath>
      pkgName = path.substring(0, index);
      relPath = path.substring(index + 1);
    }
    for (JavaFile packagesDirectory in _packagesDirectories) {
      JavaFile resolvedFile = new JavaFile.relative(packagesDirectory, path);
      if (resolvedFile.exists()) {
        JavaFile canonicalFile = getCanonicalFile(packagesDirectory, pkgName, relPath);
        UriKind uriKind = _isSelfReference(packagesDirectory, canonicalFile) ? UriKind.PACKAGE_SELF_URI : UriKind.PACKAGE_URI;
        return new FileBasedSource.con2(canonicalFile, uriKind);
      }
    }
    return new FileBasedSource.con2(getCanonicalFile(_packagesDirectories[0], pkgName, relPath), UriKind.PACKAGE_URI);
  }

  @override
  Uri restoreAbsolute(Source source) {
    if (source is FileBasedSource) {
      String sourcePath = source.file.getPath();
      for (JavaFile packagesDirectory in _packagesDirectories) {
        List<JavaFile> pkgFolders = packagesDirectory.listFiles();
        if (pkgFolders != null) {
          for (JavaFile pkgFolder in pkgFolders) {
            try {
              String pkgCanonicalPath = pkgFolder.getCanonicalPath();
              if (sourcePath.startsWith(pkgCanonicalPath)) {
                String relPath = sourcePath.substring(pkgCanonicalPath.length);
                return parseUriWithException("${PACKAGE_SCHEME}:${pkgFolder.getName()}${relPath}");
              }
            } on JavaException catch (e) {
            }
          }
        }
      }
    }
    return null;
  }

  /**
   * Answer the canonical file for the specified package.
   *
   * @param packagesDirectory the "packages" directory (not `null`)
   * @param pkgName the package name (not `null`, not empty)
   * @param relPath the path relative to the package directory (not `null`, no leading slash,
   *          but may be empty string)
   * @return the file (not `null`)
   */
  JavaFile getCanonicalFile(JavaFile packagesDirectory, String pkgName, String relPath) {
    JavaFile pkgDir = new JavaFile.relative(packagesDirectory, pkgName);
    try {
      pkgDir = pkgDir.getCanonicalFile();
    } on JavaIOException catch (e) {
      if (!e.toString().contains("Required key not available")) {
        AnalysisEngine.instance.logger.logError2("Canonical failed: ${pkgDir}", e);
      } else if (_CanLogRequiredKeyIoException) {
        _CanLogRequiredKeyIoException = false;
        AnalysisEngine.instance.logger.logError2("Canonical failed: ${pkgDir}", e);
      }
    }
    return new JavaFile.relative(pkgDir, relPath.replaceAll('/', new String.fromCharCode(JavaFile.separatorChar)));
  }

  /**
   * @return `true` if "file" was found in "packagesDir", and it is part of the "lib" folder
   *         of the application that contains in this "packagesDir".
   */
  bool _isSelfReference(JavaFile packagesDir, JavaFile file) {
    JavaFile rootDir = packagesDir.getParentFile();
    if (rootDir == null) {
      return false;
    }
    String rootPath = rootDir.getAbsolutePath();
    String filePath = file.getAbsolutePath();
    return filePath.startsWith("${rootPath}/lib");
  }
}

/**
 * Instances of the class [DirectoryBasedSourceContainer] represent a source container that
 * contains all sources within a given directory.
 */
class DirectoryBasedSourceContainer implements SourceContainer {
  /**
   * Append the system file separator to the given path unless the path already ends with a
   * separator.
   *
   * @param path the path to which the file separator is to be added
   * @return a path that ends with the system file separator
   */
  static String _appendFileSeparator(String path) {
    if (path == null || path.length <= 0 || path.codeUnitAt(path.length - 1) == JavaFile.separatorChar) {
      return path;
    }
    return "${path}${JavaFile.separator}";
  }

  /**
   * The container's path (not `null`).
   */
  String _path;

  /**
   * Construct a container representing the specified directory and containing any sources whose
   * [Source#getFullName] starts with the directory's path. This is a convenience method,
   * fully equivalent to [DirectoryBasedSourceContainer#DirectoryBasedSourceContainer]
   * .
   *
   * @param directory the directory (not `null`)
   */
  DirectoryBasedSourceContainer.con1(JavaFile directory) : this.con2(directory.getPath());

  /**
   * Construct a container representing the specified path and containing any sources whose
   * [Source#getFullName] starts with the specified path.
   *
   * @param path the path (not `null` and not empty)
   */
  DirectoryBasedSourceContainer.con2(String path) {
    this._path = _appendFileSeparator(path);
  }

  @override
  bool contains(Source source) => source.fullName.startsWith(_path);

  @override
  bool operator ==(Object obj) => (obj is DirectoryBasedSourceContainer) && obj.path == path;

  /**
   * Answer the receiver's path, used to determine if a source is contained in the receiver.
   *
   * @return the path (not `null`, not empty)
   */
  String get path => _path;

  @override
  int get hashCode => _path.hashCode;

  @override
  String toString() => "SourceContainer[${_path}]";
}

/**
 * Instances of the class `FileUriResolver` resolve `file` URI's.
 */
class FileUriResolver extends UriResolver {
  /**
   * The name of the `file` scheme.
   */
  static String FILE_SCHEME = "file";

  /**
   * Return `true` if the given URI is a `file` URI.
   *
   * @param uri the URI being tested
   * @return `true` if the given URI is a `file` URI
   */
  static bool isFileUri(Uri uri) => uri.scheme == FILE_SCHEME;

  @override
  Source fromEncoding(UriKind kind, Uri uri) {
    if (kind == UriKind.FILE_URI) {
      return new FileBasedSource.con2(new JavaFile.fromUri(uri), kind);
    }
    return null;
  }

  @override
  Source resolveAbsolute(Uri uri) {
    if (!isFileUri(uri)) {
      return null;
    }
    return new FileBasedSource.con1(new JavaFile.fromUri(uri));
  }
}