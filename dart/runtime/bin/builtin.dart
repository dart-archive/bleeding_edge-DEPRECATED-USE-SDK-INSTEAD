// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library builtin;
import 'dart:async';
import 'dart:convert';
import 'dart:isolate';


//////////////////////
/* Support for loading within the isolate via dart:io */
import 'dart:io';

// Enable by setting the #define LOAD_VIA_SERVICE_ISOLATE (see dartutils.cc)
bool _load_via_service_isolate = false;

var _httpClient;
void _httpGet(int tag,
              Uri uri,
              String libraryUri,
              loadCallback(List<int> data)) {
  if (_httpClient == null) {
    _httpClient = new HttpClient()..maxConnectionsPerHost = 6;
  }
  _httpClient.getUrl(uri)
    .then((HttpClientRequest request) => request.close())
    .then((HttpClientResponse response) {
      var builder = new BytesBuilder(copy: false);
      response.listen(
          builder.add,
          onDone: () {
            if (response.statusCode != 200) {
              var msg = 'Failure getting $uri: '
                        '${response.statusCode} ${response.reasonPhrase}';
              _asyncLoadError(tag, uri.toString(), libraryUri, msg);
            }
            loadCallback(builder.takeBytes());
          },
          onError: (error) {
            _asyncLoadError(tag, uri.toString(), libraryUri, error);
          });
    })
    .catchError((error) {
      _asyncLoadError(tag, uri.toString(), libraryUri, error);
    });
  // TODO(floitsch): remove this line. It's just here to push an event on the
  // event loop so that we invoke the scheduled microtasks. Also remove the
  // import of dart:async when this line is not needed anymore.
  Timer.run(() {});
}


void _cleanup() {
  if (_httpClient != null) {
    _httpClient.close();
    _httpClient = null;
 }
}

_loadDataAsyncDartIO(int tag,
                       String uri,
                       String libraryUri,
                       Uri resourceUri) {
  _startingOneLoadRequest(uri);
  if ((resourceUri.scheme == 'http') || (resourceUri.scheme == 'https')) {
    _httpGet(tag, resourceUri, libraryUri, (data) {
      _loadScript(tag, uri, libraryUri, data);
    });
  } else {
    var sourceFile = new File(resourceUri.toFilePath());
    sourceFile.readAsBytes().then((data) {
      _loadScript(tag, uri, libraryUri, data);
    },
    onError: (e) {
      _asyncLoadError(tag, uri, libraryUri, e);
    });
  }
}

//////////////////////

/* See Dart_LibraryTag in dart_api.h */
const Dart_kScriptTag = null;
const Dart_kImportTag = 0;
const Dart_kSourceTag = 1;
const Dart_kCanonicalizeUrl = 2;

// Dart native extension scheme.
const _DART_EXT = 'dart-ext:';

// import 'root_library'; happens here from C Code

// The root library (aka the script) is imported into this library. The
// standalone embedder uses this to lookup the main entrypoint in the
// root library's namespace.
Function _getMainClosure() => main;

// A port for communicating with the service isolate for I/O.
SendPort _loadPort;

const _logBuiltin = false;

// Corelib 'print' implementation.
void _print(arg) {
  _Logger._printString(arg.toString());
}

class _Logger {
  static void _printString(String s) native "Logger_PrintString";
}

_getPrintClosure() => _print;

_getCurrentDirectoryPath() native "Directory_Current";

// Corelib 'Uri.base' implementation.
Uri _uriBase() {
  // We are not using Dircetory.current here to limit the dependency
  // on dart:io. This code is the same as:
  //   return new Uri.file(Directory.current.path + "/");
  var result = _getCurrentDirectoryPath();
  if (result is OSError) {
    throw new FileSystemException(
        "Getting current working directory failed", "", result);
  }
  return new Uri.file(result + "/");
}

_getUriBaseClosure() => _uriBase;


// Are we running on Windows?
var _isWindows;
var _workingWindowsDrivePrefix;
// The current working directory
var _workingDirectoryUri;
// The URI that the entry point script was loaded from. Remembered so that
// package imports can be resolved relative to it.
var _entryPointScript;
// The directory to look in to resolve "package:" scheme URIs.
var _packageRoot;

_sanitizeWindowsPath(path) {
  // For Windows we need to massage the paths a bit according to
  // http://blogs.msdn.com/b/ie/archive/2006/12/06/file-uris-in-windows.aspx
  //
  // Convert
  // C:\one\two\three
  // to
  // /C:/one/two/three

  if (_isWindows == false) {
    // Do nothing when not running Windows.
    return path;
  }

  var fixedPath = "${path.replaceAll('\\', '/')}";

  if ((path.length > 2) && (path[1] == ':')) {
    // Path begins with a drive letter.
    return '/$fixedPath';
  }

  return fixedPath;
}

_trimWindowsPath(path) {
  // Convert /X:/ to X:/.
  if (_isWindows == false) {
    // Do nothing when not running Windows.
    return path;
  }
  if (!path.startsWith('/') || (path.length < 3)) {
    return path;
  }
  // Match '/?:'.
  if ((path[0] == '/') && (path[2] == ':')) {
    // Remove leading '/'.
    return path.substring(1);
  }
  return path;
}

_enforceTrailingSlash(uri) {
  // Ensure we have a trailing slash character.
  if (!uri.endsWith('/')) {
    return '$uri/';
  }
  return uri;
}


_extractDriveLetterPrefix(cwd) {
  if (!_isWindows) {
    return null;
  }
  if (cwd.length > 1 && cwd[1] == ':') {
    return '/${cwd[0]}:';
  }
  return null;
}


void _setWorkingDirectory(cwd) {
  _workingWindowsDrivePrefix = _extractDriveLetterPrefix(cwd);
  cwd = _sanitizeWindowsPath(cwd);
  cwd = _enforceTrailingSlash(cwd);
  _workingDirectoryUri = new Uri(scheme: 'file', path: cwd);
  if (_logBuiltin) {
    _print('# Working Directory: $cwd');
  }
}


_setPackageRoot(String packageRoot) {
  packageRoot = _enforceTrailingSlash(packageRoot);
  if (packageRoot.startsWith('file:') ||
      packageRoot.startsWith('http:') ||
      packageRoot.startsWith('https:')) {
    _packageRoot = _workingDirectoryUri.resolve(packageRoot);
  } else {
    packageRoot = _sanitizeWindowsPath(packageRoot);
    packageRoot = _trimWindowsPath(packageRoot);
    _packageRoot = _workingDirectoryUri.resolveUri(new Uri.file(packageRoot));
  }
  if (_logBuiltin) {
    _print('# Package root: $packageRoot -> $_packageRoot');
  }
}


// Given a uri with a 'package' scheme, return a Uri that is prefixed with
// the package root.
Uri _resolvePackageUri(Uri uri) {
  if (!uri.host.isEmpty) {
    var path = '${uri.host}${uri.path}';
    var right = 'package:$path';
    var wrong = 'package://$path';

    throw "URIs using the 'package:' scheme should look like "
          "'$right', not '$wrong'.";
  }

  var packageRoot = _packageRoot == null ?
                    _entryPointScript.resolve('packages/') :
                    _packageRoot;
  return packageRoot.resolve(uri.path);
}



String _resolveScriptUri(String scriptName) {
  if (_workingDirectoryUri == null) {
    throw 'No current working directory set.';
  }
  scriptName = _sanitizeWindowsPath(scriptName);

  var scriptUri = Uri.parse(scriptName);
  if (scriptUri.scheme != '') {
    // Script has a scheme, assume that it is fully formed.
    _entryPointScript = scriptUri;
  } else {
    // Script does not have a scheme, assume that it is a path,
    // resolve it against the working directory.
    _entryPointScript = _workingDirectoryUri.resolve(scriptName);
  }
  if (_logBuiltin) {
    _print('# Resolved entry point to: $_entryPointScript');
  }
  return _entryPointScript.toString();
}


// Function called by standalone embedder to resolve uris.
String _resolveUri(String base, String userString) {
  if (_logBuiltin) {
    _print('# Resolving: $userString from $base');
  }
  var baseUri = Uri.parse(base);
  if (userString.startsWith(_DART_EXT)) {
    var uri = userString.substring(_DART_EXT.length);
    return '$_DART_EXT${baseUri.resolve(uri)}';
  } else {
    return baseUri.resolve(userString).toString();
  }
}

Uri _createUri(String userUri) {
  var uri = Uri.parse(userUri);
  switch (uri.scheme) {
    case '':
    case 'file':
    case 'http':
    case 'https':
      return uri;
    case 'package':
      return _resolvePackageUri(uri);
    default:
      // Only handling file, http[s], and package URIs
      // in standalone binary.
      if (_logBuiltin) {
        _print('# Unknown scheme (${uri.scheme}) in $uri.');
      }
      throw 'Not a known scheme: $uri';
  }
}

int _numOutstandingLoadRequests = 0;
void _finishedOneLoadRequest(String uri) {
  assert(_numOutstandingLoadRequests > 0);
  _numOutstandingLoadRequests--;
  if (_logBuiltin) {
    _print("Loading of $uri finished, "
           "${_numOutstandingLoadRequests} requests remaining");
  }
  if (_numOutstandingLoadRequests == 0) {
    _signalDoneLoading();
    _cleanup();
  }
}

void _startingOneLoadRequest(String uri) {
  assert(_numOutstandingLoadRequests >= 0);
  _numOutstandingLoadRequests++;
  if (_logBuiltin) {
    _print("Loading of $uri started, "
           "${_numOutstandingLoadRequests} requests outstanding");
  }
}

class LoadError extends Error {
  final String message;
  LoadError(this.message);

  String toString() => 'Load Error: $message';
}

void _signalDoneLoading() native "Builtin_DoneLoading";
void _loadScriptCallback(int tag, String uri, String libraryUri, List<int> data)
    native "Builtin_LoadScript";
void _asyncLoadErrorCallback(uri, libraryUri, error)
    native "Builtin_AsyncLoadError";

void _loadScript(int tag, String uri, String libraryUri, List<int> data) {
  // TODO: Currently a compilation error while loading the script is
  // fatal for the isolate. _loadScriptCallback() does not return and
  // the _numOutstandingLoadRequests counter remains out of sync.
  _loadScriptCallback(tag, uri, libraryUri, data);
  _finishedOneLoadRequest(uri);
}

void _asyncLoadError(tag, uri, libraryUri, error) {
  if (_logBuiltin) {
    _print("_asyncLoadError($uri), error: $error");
  }
  if (tag == Dart_kImportTag) {
    // When importing a library, the libraryUri is the imported
    // uri.
    libraryUri = uri;
  }
  _asyncLoadErrorCallback(uri, libraryUri, new LoadError(error.toString()));
  _finishedOneLoadRequest(uri);
}


_loadDataAsyncLoadPort(int tag,
                       String uri,
                       String libraryUri,
                       Uri resourceUri) {
  var receivePort = new ReceivePort();
  receivePort.first.then((dataOrError) {
    if (dataOrError is List<int>) {
      _loadScript(tag, uri, libraryUri, dataOrError);
    } else {
      _asyncLoadError(tag, uri, libraryUri, dataOrError);
    }
  }).catchError((e) {
    _asyncLoadError(tag, uri, libraryUri, e.toString());
  });

  try {
    var msg = [receivePort.sendPort, resourceUri.toString()];
    _loadPort.send(msg);
    _startingOneLoadRequest(uri);
  } catch (e) {
    if (_logBuiltin) {
      _print("Exception when communicating with service isolate: $e");
    }
    _asyncLoadError(tag, uri, libraryUri, e.toString());
    receivePort.close();
  }
}

// Asynchronously loads script data through a http[s] or file uri.
_loadDataAsync(int tag, String uri, String libraryUri) {
  if (tag == Dart_kScriptTag) {
    uri = _resolveScriptUri(uri);
  }

  Uri resourceUri = _createUri(uri);

  if (_load_via_service_isolate) {
    _loadDataAsyncLoadPort(tag, uri, libraryUri, resourceUri);
  } else {
    _loadDataAsyncDartIO(tag, uri, libraryUri, resourceUri);
  }

}

// Returns either a file path or a URI starting with http[s]:, as a String.
String _filePathFromUri(String userUri) {
  var uri = Uri.parse(userUri);
  if (_logBuiltin) {
    _print('# Getting file path from: $uri');
  }

  var path;
  switch (uri.scheme) {
    case '':
    case 'file':
      return uri.toFilePath();
    case 'package':
      return _filePathFromUri(_resolvePackageUri(uri).toString());
    case 'http':
    case 'https':
      return uri.toString();
    default:
      // Only handling file, http, and package URIs
      // in standalone binary.
      if (_logBuiltin) {
        _print('# Unknown scheme (${uri.scheme}) in $uri.');
      }
      throw 'Not a known scheme: $uri';
  }
}

String _nativeLibraryExtension() native "Builtin_NativeLibraryExtension";

String _platformExtensionFileName(String name) {
  var extension = _nativeLibraryExtension();

  if (_isWindows) {
    return '$name.$extension';
  } else {
    return 'lib$name.$extension';
  }
}

// Returns the directory part, the filename part, and the name
// of a native extension URL as a list [directory, filename, name].
// The directory part is either a file system path or an HTTP(S) URL.
// The filename part is the extension name, with the platform-dependent
// prefixes and extensions added.
_extensionPathFromUri(String userUri) {
  if (!userUri.startsWith(_DART_EXT)) {
    throw 'Unexpected internal error: Extension URI $userUri missing dart-ext:';
  }
  userUri = userUri.substring(_DART_EXT.length);

  if (userUri.contains('\\')) {
    throw 'Unexpected internal error: Extension URI $userUri contains \\';
  }


  String name;
  String path;  // Will end in '/'.
  int index = userUri.lastIndexOf('/');
  if (index == -1) {
    name = userUri;
    path = './';
  } else if (index == userUri.length - 1) {
    throw 'Extension name missing in $extensionUri';
  } else {
    name = userUri.substring(index + 1);
    path = userUri.substring(0, index + 1);
  }

  path = _filePathFromUri(path);
  var filename = _platformExtensionFileName(name);

  return [path, filename, name];
}
