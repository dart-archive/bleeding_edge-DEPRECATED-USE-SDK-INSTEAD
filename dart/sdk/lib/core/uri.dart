// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

part of dart.core;

/**
 * A parsed URI, such as a URL.
 *
 * **See also:**
 *
 * * [URIs][uris] in the [library tour][libtour]
 * * [RFC-3986](http://tools.ietf.org/html/rfc3986)
 *
 * [uris]: http://www.dartlang.org/docs/dart-up-and-running/contents/ch03.html#ch03-uri
 * [libtour]: http://www.dartlang.org/docs/dart-up-and-running/contents/ch03.html
 */
class Uri {
  final String _host;
  int _port;
  String _path;

  /**
   * Returns the scheme component.
   *
   * Returns the empty string if there is no scheme component.
   */
  final String scheme;

  /**
   * Returns the authority component.
   *
   * The authority is formatted from the [userInfo], [host] and [port]
   * parts.
   *
   * Returns the empty string if there is no authority component.
   */
  String get authority {
    if (!hasAuthority) return "";
    var sb = new StringBuffer();
    _writeAuthority(sb);
    return sb.toString();
  }

  /**
   * Returns the user info part of the authority component.
   *
   * Returns the empty string if there is no user info in the
   * authority component.
   */
  final String userInfo;

  /**
   * Returns the host part of the authority component.
   *
   * Returns the empty string if there is no authority component and
   * hence no host.
   *
   * If the host is an IP version 6 address, the surrounding `[` and `]` is
   * removed.
   */
  String get host {
    if (_host != null && _host.startsWith('[')) {
      return _host.substring(1, _host.length - 1);
    }
    return _host;
  }

  /**
   * Returns the port part of the authority component.
   *
   * Returns 0 if there is no port in the authority component.
   */
  int get port {
    if (_port == 0) {
      if (scheme == "http") return 80;
      if (scheme == "https") return 443;
    }
    return _port;
  }

  /**
   * Returns the path component.
   *
   * The returned path is encoded. To get direct access to the decoded
   * path use [pathSegments].
   *
   * Returns the empty string if there is no path component.
   */
  String get path => _path;

  /**
   * Returns the query component. The returned query is encoded. To get
   * direct access to the decoded query use [queryParameters].
   *
   * Returns the empty string if there is no query component.
   */
  final String query;

  /**
   * Returns the fragment identifier component.
   *
   * Returns the empty string if there is no fragment identifier
   * component.
   */
  final String fragment;

  /**
   * Cache the computed return value of [pathSegements].
   */
  List<String> _pathSegments;

  /**
   * Cache the computed return value of [queryParameters].
   */
  Map<String, String> _queryParameters;

  /**
   * Creates a new URI object by parsing a URI string.
   */
  static Uri parse(String uri) {
    // This parsing will not validate percent-encoding, IPv6, etc. When done
    // it will call `new Uri(...)` which will perform these validations.
    // This is purely splitting up the URI string into components.
    //
    // Important parts of the RFC 3986 used here:
    // URI           = scheme ":" hier-part [ "?" query ] [ "#" fragment ]
    //
    // hier-part     = "//" authority path-abempty
    //               / path-absolute
    //               / path-rootless
    //               / path-empty
    //
    // URI-reference = URI / relative-ref
    //
    // absolute-URI  = scheme ":" hier-part [ "?" query ]
    //
    // relative-ref  = relative-part [ "?" query ] [ "#" fragment ]
    //
    // relative-part = "//" authority path-abempty
    //               / path-absolute
    //               / path-noscheme
    //               / path-empty
    //
    // scheme        = ALPHA *( ALPHA / DIGIT / "+" / "-" / "." )
    //
    // authority     = [ userinfo "@" ] host [ ":" port ]
    // userinfo      = *( unreserved / pct-encoded / sub-delims / ":" )
    // host          = IP-literal / IPv4address / reg-name
    // port          = *DIGIT
    // reg-name      = *( unreserved / pct-encoded / sub-delims )
    //
    // path          = path-abempty    ; begins with "/" or is empty
    //               / path-absolute   ; begins with "/" but not "//"
    //               / path-noscheme   ; begins with a non-colon segment
    //               / path-rootless   ; begins with a segment
    //               / path-empty      ; zero characters
    //
    // path-abempty  = *( "/" segment )
    // path-absolute = "/" [ segment-nz *( "/" segment ) ]
    // path-noscheme = segment-nz-nc *( "/" segment )
    // path-rootless = segment-nz *( "/" segment )
    // path-empty    = 0<pchar>
    //
    // segment       = *pchar
    // segment-nz    = 1*pchar
    // segment-nz-nc = 1*( unreserved / pct-encoded / sub-delims / "@" )
    //               ; non-zero-length segment without any colon ":"
    //
    // pchar         = unreserved / pct-encoded / sub-delims / ":" / "@"
    //
    // query         = *( pchar / "/" / "?" )
    //
    // fragment      = *( pchar / "/" / "?" )
    bool isRegName(int ch) {
      return ch < 128 && ((_regNameTable[ch >> 4] & (1 << (ch & 0x0f))) != 0);
    }

    int ipV6Address(int index) {
      // IPv6. Skip to ']'.
      index = uri.indexOf(']', index);
      if (index == -1) {
        throw new FormatException("Bad end of IPv6 host");
      }
      return index + 1;
    }

    int length = uri.length;
    int index = 0;

    int schemeEndIndex = 0;

    if (length == 0) {
      return new Uri();
    }

    if (uri.codeUnitAt(0) != _SLASH) {
      // Can be scheme.
      while (index < length) {
        // Look for ':'. If found, continue from the post of ':'. If not (end
        // reached or invalid scheme char found) back up one char, and continue
        // to path.
        // Note that scheme-chars is contained in path-chars.
        int codeUnit = uri.codeUnitAt(index++);
        if (!_isSchemeCharacter(codeUnit)) {
          if (codeUnit == _COLON) {
            schemeEndIndex = index;
          } else {
            // Back up one char, since we met an invalid scheme char.
            index--;
          }
          break;
        }
      }
    }

    int userInfoEndIndex = -1;
    int portIndex = -1;
    int authorityEndIndex = schemeEndIndex;
    // If we see '//', there must be an authority.
    if (authorityEndIndex == index &&
        authorityEndIndex + 1 < length &&
        uri.codeUnitAt(authorityEndIndex) == _SLASH &&
        uri.codeUnitAt(authorityEndIndex + 1) == _SLASH) {
      // Skip '//'.
      authorityEndIndex += 2;
      // It can both be host and userInfo.
      while (authorityEndIndex < length) {
        int codeUnit = uri.codeUnitAt(authorityEndIndex++);
        if (!isRegName(codeUnit)) {
          if (codeUnit == _LEFT_BRACKET) {
            authorityEndIndex = ipV6Address(authorityEndIndex);
          } else if (portIndex == -1 && codeUnit == _COLON) {
            // First time ':'.
            portIndex = authorityEndIndex;
          } else if (codeUnit == _AT_SIGN || codeUnit == _COLON) {
            // Second time ':' or first '@'. Must be userInfo.
            userInfoEndIndex = uri.indexOf('@', authorityEndIndex - 1);
            // Not found. Must be path then.
            if (userInfoEndIndex == -1) {
              authorityEndIndex = index;
              break;
            }
            portIndex = -1;
            authorityEndIndex = userInfoEndIndex + 1;
            // Now it can only be host:port.
            while (authorityEndIndex < length) {
              int codeUnit = uri.codeUnitAt(authorityEndIndex++);
              if (!isRegName(codeUnit)) {
                if (codeUnit == _LEFT_BRACKET) {
                  authorityEndIndex = ipV6Address(authorityEndIndex);
                } else if (codeUnit == _COLON) {
                  if (portIndex != -1) {
                    throw new FormatException("Double port in host");
                  }
                  portIndex = authorityEndIndex;
                } else {
                  authorityEndIndex--;
                  break;
                }
              }
            }
            break;
          } else {
            authorityEndIndex--;
            break;
          }
        }
      }
    } else {
      authorityEndIndex = schemeEndIndex;
    }

    // At path now.
    int pathEndIndex = authorityEndIndex;
    while (pathEndIndex < length) {
      int codeUnit = uri.codeUnitAt(pathEndIndex++);
      if (codeUnit == _QUESTION || codeUnit == _NUMBER_SIGN) {
        pathEndIndex--;
        break;
      }
    }

    // Maybe query.
    int queryEndIndex = pathEndIndex;
    if (queryEndIndex < length && uri.codeUnitAt(queryEndIndex) == _QUESTION) {
      while (queryEndIndex < length) {
        int codeUnit = uri.codeUnitAt(queryEndIndex++);
        if (codeUnit == _NUMBER_SIGN) {
          queryEndIndex--;
          break;
        }
      }
    }

    var scheme = null;
    if (schemeEndIndex > 0) {
      scheme = uri.substring(0, schemeEndIndex - 1);
    }

    var host = "";
    var userInfo = "";
    var port = 0;
    if (schemeEndIndex != authorityEndIndex) {
      int startIndex = schemeEndIndex + 2;
      if (userInfoEndIndex > 0) {
        userInfo = uri.substring(startIndex, userInfoEndIndex);
        startIndex = userInfoEndIndex + 1;
      }
      if (portIndex > 0) {
        var portStr = uri.substring(portIndex, authorityEndIndex);
        try {
          port = int.parse(portStr);
        } catch (_) {
          throw new FormatException("Invalid port: '$portStr'");
        }
        host = uri.substring(startIndex, portIndex - 1);
      } else {
        host = uri.substring(startIndex, authorityEndIndex);
      }
    }

    var path = uri.substring(authorityEndIndex, pathEndIndex);
    var query = "";
    if (pathEndIndex < queryEndIndex) {
      query = uri.substring(pathEndIndex + 1, queryEndIndex);
    }
    var fragment = "";
    // If queryEndIndex is not at end (length), there is a fragment.
    if (queryEndIndex < length) {
      fragment = uri.substring(queryEndIndex + 1, length);
    }

    return new Uri(scheme: scheme,
                   userInfo: userInfo,
                   host: host,
                   port: port,
                   path: path,
                   query: query,
                   fragment: fragment);
  }

  /**
   * Creates a new URI from its components.
   *
   * Each component is set through a named argument. Any number of
   * components can be provided. The default value for the components
   * not provided is the empry string, except for [port] which has a
   * default value of 0. The [path] and [query] components can be set
   * using two different named arguments.
   *
   * The scheme component is set through [scheme]. The scheme is
   * normalized to all lowercase letters.
   *
   * The user info part of the authority component is set through
   * [userInfo].
   *
   * The host part of the authority component is set through
   * [host]. The host can either be a hostname, an IPv4 address or an
   * IPv6 address, contained in '[' and ']'. If the host contains a
   * ':' character, the '[' and ']' are added if not already provided.
   *
   * The port part of the authority component is set through
   * [port]. The port is normalized for scheme http and https where
   * port 80 and port 443 respectively is set.
   *
   * The path component is set through either [path] or
   * [pathSegments]. When [path] is used, the provided string is
   * expected to be fully percent-encoded, and is used in its literal
   * form. When [pathSegments] is used, each of the provided segments
   * is percent-encoded and joined using the forward slash
   * separator. The percent-encoding of the path segments encodes all
   * characters except for the unreserved characters and the following
   * list of characters: `!$&'()*+,;=:@`. If the other components
   * calls for an absolute path a leading slash `/` is prepended if
   * not already there.
   *
   * The query component is set through either [query] or
   * [queryParameters]. When [query] is used the provided string is
   * expected to be fully percent-encoded and is used in its literal
   * form. When [queryParameters] is used the query is built from the
   * provided map. Each key and value in the map is percent-encoded
   * and joined using equal and ampersand characters. The
   * percent-encoding of the keys and values encodes all characters
   * except for the unreserved characters.
   *
   * The fragment component is set through [fragment].
   */
  Uri({String scheme,
       this.userInfo: "",
       String host: "",
       port: 0,
       String path,
       Iterable<String> pathSegments,
       String query,
       Map<String, String> queryParameters,
       fragment: ""}) :
      scheme = _makeScheme(scheme),
      _host = _makeHost(host),
      query = _makeQuery(query, queryParameters),
      fragment = _makeFragment(fragment) {
    // Perform scheme specific normalization.
    if (scheme == "http" && port == 80) {
      _port = 0;
    } else if (scheme == "https" && port == 443) {
      _port = 0;
    } else {
      _port = port;
    }
    // Fill the path.
    _path = _makePath(path, pathSegments);
  }

  /**
   * Creates a new `http` URI from authority, path and query.
   *
   * Examples:
   *
   *     // Create the URI http://example.org/path?q=abc.
   *     new Uri.http("google.com", "/search", { "q" : "dart" });http://example.org/path?q=abc.
   *     new Uri.http("user:pass@localhost:8080, "");  // http://user:pass@localhost:8080/
   *     new Uri.http("example.org, "a b");  // http://example.org/a%20b
   *     new Uri.http("example.org, "/a%2F");  // http://example.org/a%25%2F
   *
   * The `scheme` is always set to `http`.
   *
   * The `userInfo`, `host` and `port` components are set from the
   * [authority] argument.
   *
   * The `path` component is set from the [unencodedPath]
   * argument. The path passed must not be encoded as this constructor
   * encodes the path.
   *
   * The `query` component is set from the optional [queryParameters]
   * argument.
   */
  factory Uri.http(String authority,
                   String unencodedPath,
                   [Map<String, String> queryParameters]) {
    return _makeHttpUri("http", authority, unencodedPath, queryParameters);
  }

  /**
   * Creates a new `https` URI from authority, path and query.
   *
   * This constructor is the same as [Uri.http] except for the scheme
   * which is set to `https`.
   */
  factory Uri.https(String authority,
                    String unencodedPath,
                    [Map<String, String> queryParameters]) {
    return _makeHttpUri("https", authority, unencodedPath, queryParameters);
  }

  static Uri _makeHttpUri(String scheme,
                          String authority,
                          String unencodedPath,
                          Map<String, String> queryParameters) {
    var userInfo = "";
    var host = "";
    var port = 0;

    var hostStart = 0;
    // Split off the user info.
    bool hasUserInfo = false;
    for (int i = 0; i < authority.length; i++) {
      if (authority.codeUnitAt(i) == _AT_SIGN) {
        hasUserInfo = true;
        userInfo = authority.substring(0, i);
        hostStart = i + 1;
        break;
      }
    }
    var hostEnd = hostStart;
    if (hostStart < authority.length &&
        authority.codeUnitAt(hostStart) == _LEFT_BRACKET) {
      // IPv6 host.
      for (; hostEnd < authority.length; hostEnd++) {
        if (authority.codeUnitAt(hostEnd) == _RIGHT_BRACKET) break;
      }
      if (hostEnd == authority.length) {
        throw new FormatException("Invalid IPv6 host entry.");
      }
      parseIPv6Address(authority.substring(hostStart + 1, hostEnd));
      hostEnd++;  // Skip the closing bracket.
      if (hostEnd != authority.length &&
          authority.codeUnitAt(hostEnd) != _COLON) {
        throw new FormatException("Invalid end of authority");
      }
    }
    // Split host and port.
    bool hasPort = false;
    for (; hostEnd < authority.length; hostEnd++) {
      if (authority.codeUnitAt(hostEnd) == _COLON) {
        var portString = authority.substring(hostEnd + 1);
        // We allow the empty port - falling back to initial value.
        if (portString.isNotEmpty) port = int.parse(portString);
        break;
      }
    }
    host = authority.substring(hostStart, hostEnd);

    return new Uri(scheme: scheme,
                   userInfo: userInfo,
                   host: host,
                   port: port,
                   pathSegments: unencodedPath.split("/"),
                   queryParameters: queryParameters);
  }

  /**
   * Creates a new file URI from an absolute or relative file path.
   *
   * The file path is passed in [path].
   *
   * This path is interpreted using either Windows or non-Windows
   * semantics.
   *
   * With non-Windows semantics the slash ("/") is used to separate
   * path segments.
   *
   * With Windows semantics, backslash ("\") and forward-slash ("/")
   * are used to separate path segments, except if the path starts
   * with "\\?\" in which case, only backslash ("\") separates path
   * segments.
   *
   * If the path starts with a path separator an absolute URI is
   * created. Otherwise a relative URI is created. One exception from
   * this rule is that when Windows semantics is used and the path
   * starts with a drive letter followed by a colon (":") and a
   * path separator then an absolute URI is created.
   *
   * The default for whether to use Windows or non-Windows semantics
   * determined from the platform Dart is running on. When running in
   * the standalone VM this is detected by the VM based on the
   * operating system. When running in a browser non-Windows semantics
   * is always used.
   *
   * To override the automatic detection of which semantics to use pass
   * a value for [windows]. Passing `true` will use Windows
   * semantics and passing `false` will use non-Windows semantics.
   *
   * Examples using non-Windows semantics (resulting URI in comment):
   *
   *     new Uri.file("xxx/yyy");  // xxx/yyy
   *     new Uri.file("xxx/yyy/");  // xxx/yyy/
   *     new Uri.file("/xxx/yyy");  // file:///xxx/yyy
   *     new Uri.file("/xxx/yyy/");  // file:///xxx/yyy/
   *     new Uri.file("C:");  // C:
   *
   * Examples using Windows semantics (resulting URI in comment):
   *
   *     new Uri.file(r"xxx\yyy");  // xxx/yyy
   *     new Uri.file(r"xxx\yyy\");  // xxx/yyy/
   *     new Uri.file(r"\xxx\yyy");  // file:///xxx/yyy
   *     new Uri.file(r"\xxx\yyy/");  // file:///xxx/yyy/
   *     new Uri.file(r"C:\xxx\yyy");  // file:///C:/xxx/yyy
   *     new Uri.file(r"C:xxx\yyy");  // Throws as path with drive letter
   *                                  // is not absolute.
   *     new Uri.file(r"\\server\share\file");  // file://server/share/file
   *     new Uri.file(r"C:");  // Throws as path with drive letter
   *                           // is not absolute.
   *
   * If the path passed is not a legal file path [ArgumentError] is thrown.
   */
  factory Uri.file(String path, {bool windows}) {
    windows = windows == null ? Uri._isWindows : windows;
    return windows ? _makeWindowsFileUrl(path) : _makeFileUri(path);
  }

  /**
   * Returns the natural base URI for the current platform.
   *
   * When running in a browser this is the current URL (from
   * `window.location.href`).
   *
   * When not running in a browser this is the file URI referencing
   * the current working directory.
   */
  external static Uri get base;

  external static bool get _isWindows;

  static _checkNonWindowsPathReservedCharacters(List<String> segments,
                                                bool argumentError) {
    segments.forEach((segment) {
      if (segment.contains("/")) {
        if (argumentError) {
          throw new ArgumentError("Illegal path character $segment");
        } else {
          throw new UnsupportedError("Illegal path character $segment");
        }
      }
    });
  }

  static _checkWindowsPathReservedCharacters(List<String> segments,
                                             bool argumentError,
                                             [int firstSegment = 0]) {
    segments.skip(firstSegment).forEach((segment) {
      if (segment.contains(new RegExp(r'["*/:<>?\\|]'))) {
        if (argumentError) {
          throw new ArgumentError("Illegal character in path");
        } else {
          throw new UnsupportedError("Illegal character in path");
        }
      }
    });
  }

  static _checkWindowsDriveLetter(int charCode, bool argumentError) {
    if ((_UPPER_CASE_A <= charCode && charCode <= _UPPER_CASE_Z) ||
        (_LOWER_CASE_A <= charCode && charCode <= _LOWER_CASE_Z)) {
      return;
    }
    if (argumentError) {
      throw new ArgumentError("Illegal drive letter " +
                              new String.fromCharCode(charCode));
    } else {
      throw new UnsupportedError("Illegal drive letter " +
                              new String.fromCharCode(charCode));
    }
  }

  static _makeFileUri(String path) {
    String sep = "/";
    if (path.length > 0 && path[0] == sep) {
      // Absolute file:// URI.
      return new Uri(scheme: "file", pathSegments: path.split(sep));
    } else {
      // Relative URI.
      return new Uri(pathSegments: path.split(sep));
    }
  }

  static _makeWindowsFileUrl(String path) {
    if (path.startsWith("\\\\?\\")) {
      if (path.startsWith("\\\\?\\UNC\\")) {
        path = "\\${path.substring(7)}";
      } else {
        path = path.substring(4);
        if (path.length < 3 ||
            path.codeUnitAt(1) != _COLON ||
            path.codeUnitAt(2) != _BACKSLASH) {
          throw new ArgumentError(
              "Windows paths with \\\\?\\ prefix must be absolute");
        }
      }
    } else {
      path = path.replaceAll("/", "\\");
    }
    String sep = "\\";
    if (path.length > 1 && path[1] == ":") {
      _checkWindowsDriveLetter(path.codeUnitAt(0), true);
      if (path.length == 2 || path.codeUnitAt(2) != _BACKSLASH) {
        throw new ArgumentError(
            "Windows paths with drive letter must be absolute");
      }
      // Absolute file://C:/ URI.
      var pathSegments = path.split(sep);
      _checkWindowsPathReservedCharacters(pathSegments, true, 1);
      return new Uri(scheme: "file", pathSegments: pathSegments);
    }

    if (path.length > 0 && path[0] == sep) {
      if (path.length > 1 && path[1] == sep) {
        // Absolute file:// URI with host.
        int pathStart = path.indexOf("\\", 2);
        String hostPart =
            pathStart == -1 ? path.substring(2) : path.substring(2, pathStart);
        String pathPart =
            pathStart == -1 ? "" : path.substring(pathStart + 1);
        var pathSegments = pathPart.split(sep);
        _checkWindowsPathReservedCharacters(pathSegments, true);
        return new Uri(
            scheme: "file", host: hostPart, pathSegments: pathSegments);
      } else {
        // Absolute file:// URI.
        var pathSegments = path.split(sep);
        _checkWindowsPathReservedCharacters(pathSegments, true);
        return new Uri(scheme: "file", pathSegments: pathSegments);
      }
    } else {
      // Relative URI.
      var pathSegments = path.split(sep);
      _checkWindowsPathReservedCharacters(pathSegments, true);
      return new Uri(pathSegments: pathSegments);
    }
  }

  /**
   * Returns the URI path split into its segments. Each of the
   * segments in the returned list have been decoded. If the path is
   * empty the empty list will be returned. A leading slash `/` does
   * not affect the segments returned.
   *
   * The returned list is unmodifiable and will throw [UnsupportedError] on any
   * calls that would mutate it.
   */
  List<String> get pathSegments {
    if (_pathSegments == null) {
      var pathToSplit = !path.isEmpty && path.codeUnitAt(0) == _SLASH
                        ? path.substring(1)
                        : path;
      _pathSegments = new UnmodifiableListView(
        pathToSplit == "" ? const<String>[]
                          : pathToSplit.split("/")
                                       .map(Uri.decodeComponent)
                                       .toList(growable: false));
    }
    return _pathSegments;
  }

  /**
   * Returns the URI query split into a map according to the rules
   * specified for FORM post in the [HTML 4.01 specification section 17.13.4]
   * (http://www.w3.org/TR/REC-html40/interact/forms.html#h-17.13.4
   * "HTML 4.01 section 17.13.4"). Each key and value in the returned map
   * has been decoded. If there is no query the empty map is returned.
   *
   * Keys in the query string that have no value are mapped to the
   * empty string.
   *
   * The returned map is unmodifiable and will throw [UnsupportedError] on any
   * calls that would mutate it.
   */
  Map<String, String> get queryParameters {
    if (_queryParameters == null) {
      _queryParameters = new _UnmodifiableMap(splitQueryString(query));
    }
    return _queryParameters;
  }

  static String _makeHost(String host) {
    if (host == null || host.isEmpty) return host;
    if (host.codeUnitAt(0) == _LEFT_BRACKET) {
      if (host.codeUnitAt(host.length - 1) != _RIGHT_BRACKET) {
        throw new FormatException('Missing end `]` to match `[` in host');
      }
      parseIPv6Address(host.substring(1, host.length - 1));
      return host;
    }
    for (int i = 0; i < host.length; i++) {
      if (host.codeUnitAt(i) == _COLON) {
        parseIPv6Address(host);
        return '[$host]';
      }
    }
    return host;
  }

  static String _makeScheme(String scheme) {
    bool isSchemeLowerCharacter(int ch) {
      return ch < 128 &&
             ((_schemeLowerTable[ch >> 4] & (1 << (ch & 0x0f))) != 0);
    }

    if (scheme == null) return "";
    bool allLowercase = true;
    int length = scheme.length;
    for (int i = 0; i < length; i++) {
      int codeUnit = scheme.codeUnitAt(i);
      if (i == 0 && !_isAlphabeticCharacter(codeUnit)) {
        // First code unit must be an alphabetic character.
        throw new ArgumentError('Illegal scheme: $scheme');
      }
      if (!isSchemeLowerCharacter(codeUnit)) {
        if (_isSchemeCharacter(codeUnit)) {
          allLowercase = false;
        } else {
          throw new ArgumentError('Illegal scheme: $scheme');
        }
      }
    }

    return allLowercase ? scheme : scheme.toLowerCase();
  }

  String _makePath(String path, Iterable<String> pathSegments) {
    if (path == null && pathSegments == null) return "";
    if (path != null && pathSegments != null) {
      throw new ArgumentError('Both path and pathSegments specified');
    }
    var result;
    if (path != null) {
      result = _normalize(path);
    } else {
      result = pathSegments.map((s) => _uriEncode(_pathCharTable, s)).join("/");
    }
    if ((hasAuthority || (scheme == "file")) &&
        result.isNotEmpty && !result.startsWith("/")) {
      return "/$result";
    }
    return result;
  }

  static String _makeQuery(String query, Map<String, String> queryParameters) {
    if (query == null && queryParameters == null) return "";
    if (query != null && queryParameters != null) {
      throw new ArgumentError('Both query and queryParameters specified');
    }
    if (query != null) return _normalize(query);

    var result = new StringBuffer();
    var first = true;
    queryParameters.forEach((key, value) {
      if (!first) {
        result.write("&");
      }
      first = false;
      result.write(Uri.encodeQueryComponent(key));
      if (value != null && !value.isEmpty) {
        result.write("=");
        result.write(Uri.encodeQueryComponent(value));
      }
    });
    return result.toString();
  }

  static String _makeFragment(String fragment) {
    if (fragment == null) return "";
    return _normalize(fragment);
  }

  static String _normalize(String component) {
    int index = component.indexOf('%');
    if (index < 0) return component;

    bool isNormalizedHexDigit(int digit) {
      return (_ZERO <= digit && digit <= _NINE) ||
          (_UPPER_CASE_A <= digit && digit <= _UPPER_CASE_F);
    }

    bool isLowerCaseHexDigit(int digit) {
      return _LOWER_CASE_A <= digit && digit <= _LOWER_CASE_F;
    }

    bool isUnreserved(int ch) {
      return ch < 128 &&
             ((_unreservedTable[ch >> 4] & (1 << (ch & 0x0f))) != 0);
    }

    int normalizeHexDigit(int index) {
      var codeUnit = component.codeUnitAt(index);
      if (isLowerCaseHexDigit(codeUnit)) {
        return codeUnit - 0x20;
      } else if (!isNormalizedHexDigit(codeUnit)) {
        throw new ArgumentError("Invalid URI component: $component");
      } else {
        return codeUnit;
      }
    }

    int decodeHexDigitPair(int index) {
      int byte = 0;
      for (int i = 0; i < 2; i++) {
        var codeUnit = component.codeUnitAt(index + i);
        if (_ZERO <= codeUnit && codeUnit <= _NINE) {
          byte = byte * 16 + codeUnit - _ZERO;
        } else {
          // Check ranges A-F (0x41-0x46) and a-f (0x61-0x66).
          codeUnit |= 0x20;
          if (_LOWER_CASE_A <= codeUnit &&
              codeUnit <= _LOWER_CASE_F) {
            byte = byte * 16 + codeUnit - _LOWER_CASE_A + 10;
          } else {
            throw new ArgumentError(
                "Invalid percent-encoding in URI component: $component");
          }
        }
      }
      return byte;
    }

    // Start building the normalized component string.
    StringBuffer result;
    int length = component.length;
    int prevIndex = 0;

    // Copy a part of the component string to the result.
    void fillResult() {
      if (result == null) {
        assert(prevIndex == 0);
        result = new StringBuffer(component.substring(prevIndex, index));
      } else {
        result.write(component.substring(prevIndex, index));
      }
    }

    while (index < length) {
      // Normalize percent-encoding to uppercase and don't encode
      // unreserved characters.
      assert(component.codeUnitAt(index) == _PERCENT);
      if (length < index + 2) {
          throw new ArgumentError(
              "Invalid percent-encoding in URI component: $component");
      }

      var codeUnit1 = component.codeUnitAt(index + 1);
      var codeUnit2 = component.codeUnitAt(index + 2);
      var decodedCodeUnit = decodeHexDigitPair(index + 1);
      if (isNormalizedHexDigit(codeUnit1) &&
          isNormalizedHexDigit(codeUnit2) &&
          !isUnreserved(decodedCodeUnit)) {
        index += 3;
      } else {
        fillResult();
        if (isUnreserved(decodedCodeUnit)) {
          result.writeCharCode(decodedCodeUnit);
        } else {
          result.write("%");
          result.writeCharCode(normalizeHexDigit(index + 1));
          result.writeCharCode(normalizeHexDigit(index + 2));
        }
        index += 3;
        prevIndex = index;
      }
      int next = component.indexOf('%', index);
      if (next >= index) {
        index = next;
      } else {
        index = length;
      }
    }
    if (result == null) return component;

    if (result != null && prevIndex != index) fillResult();
    assert(index == length);

    return result.toString();
  }

  static bool _isSchemeCharacter(int ch) {
    return ch < 128 && ((_schemeTable[ch >> 4] & (1 << (ch & 0x0f))) != 0);
  }


  /**
   * Returns whether the URI is absolute.
   */
  bool get isAbsolute => scheme != "" && fragment == "";

  String _merge(String base, String reference) {
    if (base == "") return "/$reference";
    return "${base.substring(0, base.lastIndexOf("/") + 1)}$reference";
  }

  bool _hasDotSegments(String path) {
    if (path.length > 0 && path.codeUnitAt(0) == _COLON) return true;
    int index = path.indexOf("/.");
    return index != -1;
  }

  String _removeDotSegments(String path) {
    if (!_hasDotSegments(path)) return path;
    List<String> output = [];
    bool appendSlash = false;
    for (String segment in path.split("/")) {
      appendSlash = false;
      if (segment == "..") {
        if (!output.isEmpty &&
            ((output.length != 1) || (output[0] != ""))) output.removeLast();
        appendSlash = true;
      } else if ("." == segment) {
        appendSlash = true;
      } else {
        output.add(segment);
      }
    }
    if (appendSlash) output.add("");
    return output.join("/");
  }

  /**
   * Resolve [reference] as an URI relative to `this`.
   *
   * First turn [reference] into a URI using [Uri.parse]. Then resolve the
   * resulting URI relative to `this`.
   *
   * Returns the resolved URI.
   *
   * See [resolveUri] for details.
   */
  Uri resolve(String reference) {
    return resolveUri(Uri.parse(reference));
  }

  /**
   * Resolve [reference] as an URI relative to `this`.
   *
   * Returns the resolved URI.
   *
   * The algorithm for resolving a reference is described in
   * [RFC-3986 Section 5]
   * (http://tools.ietf.org/html/rfc3986#section-5 "RFC-1123").
   */
  Uri resolveUri(Uri reference) {
    // From RFC 3986.
    String targetScheme;
    String targetUserInfo;
    String targetHost;
    int targetPort;
    String targetPath;
    String targetQuery;
    if (reference.scheme != "") {
      targetScheme = reference.scheme;
      targetUserInfo = reference.userInfo;
      targetHost = reference.host;
      targetPort = reference.port;
      targetPath = _removeDotSegments(reference.path);
      targetQuery = reference.query;
    } else {
      if (reference.hasAuthority) {
        targetUserInfo = reference.userInfo;
        targetHost = reference.host;
        targetPort = reference.port;
        targetPath = _removeDotSegments(reference.path);
        targetQuery = reference.query;
      } else {
        if (reference.path == "") {
          targetPath = this.path;
          if (reference.query != "") {
            targetQuery = reference.query;
          } else {
            targetQuery = this.query;
          }
        } else {
          if (reference.path.startsWith("/")) {
            targetPath = _removeDotSegments(reference.path);
          } else {
            targetPath = _removeDotSegments(_merge(this.path, reference.path));
          }
          targetQuery = reference.query;
        }
        targetUserInfo = this.userInfo;
        targetHost = this.host;
        targetPort = this.port;
      }
      targetScheme = this.scheme;
    }
    return new Uri(scheme: targetScheme,
                   userInfo: targetUserInfo,
                   host: targetHost,
                   port: targetPort,
                   path: targetPath,
                   query: targetQuery,
                   fragment: reference.fragment);
  }

  /**
   * Returns whether the URI has an [authority] component.
   */
  bool get hasAuthority => host != "";

  /**
   * Returns the origin of the URI in the form scheme://host:port for the
   * schemes http and https.
   *
   * It is an error if the scheme is not "http" or "https".
   *
   * See: http://www.w3.org/TR/2011/WD-html5-20110405/origin-0.html#origin
   */
  String get origin {
    if (scheme == "" || _host == null || _host == "") {
      throw new StateError("Cannot use origin without a scheme: $this");
    }
    if (scheme != "http" && scheme != "https") {
      throw new StateError(
        "Origin is only applicable schemes http and https: $this");
    }
    if (_port == 0) return "$scheme://$_host";
    return "$scheme://$_host:$_port";
  }

  /**
   * Returns the file path from a file URI.
   *
   * The returned path has either Windows or non-Windows
   * semantics.
   *
   * For non-Windows semantics the slash ("/") is used to separate
   * path segments.
   *
   * For Windows semantics the backslash ("\") separator is used to
   * separate path segments.
   *
   * If the URI is absolute the path starts with a path separator
   * unless Windows semantics is used and the first path segment is a
   * drive letter. When Windows semantics is used a host component in
   * the uri in interpreted as a file server and a UNC path is
   * returned.
   *
   * The default for whether to use Windows or non-Windows semantics
   * determined from the platform Dart is running on. When running in
   * the standalone VM this is detected by the VM based on the
   * operating system. When running in a browser non-Windows semantics
   * is always used.
   *
   * To override the automatic detection of which semantics to use pass
   * a value for [windows]. Passing `true` will use Windows
   * semantics and passing `false` will use non-Windows semantics.
   *
   * If the URI ends with a slash (i.e. the last path component is
   * empty) the returned file path will also end with a slash.
   *
   * With Windows semantics URIs starting with a drive letter cannot
   * be relative to the current drive on the designated drive. That is
   * for the URI `file:///c:abc` calling `toFilePath` will throw as a
   * path segment cannot contain colon on Windows.
   *
   * Examples using non-Windows semantics (resulting of calling
   * toFilePath in comment):
   *
   *     Uri.parse("xxx/yyy");  // xxx/yyy
   *     Uri.parse("xxx/yyy/");  // xxx/yyy/
   *     Uri.parse("file:///xxx/yyy");  // /xxx/yyy
   *     Uri.parse("file:///xxx/yyy/");  // /xxx/yyy/
   *     Uri.parse("file:///C:");  // /C:
   *     Uri.parse("file:///C:a");  // /C:a
   *
   * Examples using Windows semantics (resulting URI in comment):
   *
   *     Uri.parse("xxx/yyy");  // xxx\yyy
   *     Uri.parse("xxx/yyy/");  // xxx\yyy\
   *     Uri.parse("file:///xxx/yyy");  // \xxx\yyy
   *     Uri.parse("file:///xxx/yyy/");  // \xxx\yyy/
   *     Uri.parse("file:///C:/xxx/yyy");  // C:\xxx\yyy
   *     Uri.parse("file:C:xxx/yyy");  // Throws as a path segment
   *                                   // cannot contain colon on Windows.
   *     Uri.parse("file://server/share/file");  // \\server\share\file
   *
   * If the URI is not a file URI calling this throws
   * [UnsupportedError].
   *
   * If the URI cannot be converted to a file path calling this throws
   * [UnsupportedError].
   */
  String toFilePath({bool windows}) {
    if (scheme != "" && scheme != "file") {
      throw new UnsupportedError(
          "Cannot extract a file path from a $scheme URI");
    }
    if (query != "") {
      throw new UnsupportedError(
          "Cannot extract a file path from a URI with a query component");
    }
    if (fragment != "") {
      throw new UnsupportedError(
          "Cannot extract a file path from a URI with a fragment component");
    }
    if (windows == null) windows = _isWindows;
    return windows ? _toWindowsFilePath() : _toFilePath();
  }

  String _toFilePath() {
    if (host != "") {
      throw new UnsupportedError(
          "Cannot extract a non-Windows file path from a file URI "
          "with an authority");
    }
    _checkNonWindowsPathReservedCharacters(pathSegments, false);
    var result = new StringBuffer();
    if (_isPathAbsolute) result.write("/");
    result.writeAll(pathSegments, "/");
    return result.toString();
  }

  String _toWindowsFilePath() {
    bool hasDriveLetter = false;
    var segments = pathSegments;
    if (segments.length > 0 &&
        segments[0].length == 2 &&
        segments[0].codeUnitAt(1) == _COLON) {
      _checkWindowsDriveLetter(segments[0].codeUnitAt(0), false);
      _checkWindowsPathReservedCharacters(segments, false, 1);
      hasDriveLetter = true;
    } else {
      _checkWindowsPathReservedCharacters(segments, false);
    }
    var result = new StringBuffer();
    if (_isPathAbsolute && !hasDriveLetter) result.write("\\");
    if (host != "") {
      result.write("\\");
      result.write(host);
      result.write("\\");
    }
    result.writeAll(segments, "\\");
    if (hasDriveLetter && segments.length == 1) result.write("\\");
    return result.toString();
  }

  bool get _isPathAbsolute {
    if (path == null || path.isEmpty) return false;
    return path.startsWith('/');
  }

  void _writeAuthority(StringSink ss) {
    _addIfNonEmpty(ss, userInfo, userInfo, "@");
    ss.write(_host == null ? "null" : _host);
    if (_port != 0) {
      ss.write(":");
      ss.write(_port.toString());
    }
  }

  String toString() {
    StringBuffer sb = new StringBuffer();
    _addIfNonEmpty(sb, scheme, scheme, ':');
    if (hasAuthority || (scheme == "file")) {
      sb.write("//");
      _writeAuthority(sb);
    }
    sb.write(path);
    _addIfNonEmpty(sb, query, "?", query);
    _addIfNonEmpty(sb, fragment, "#", fragment);
    return sb.toString();
  }

  bool operator==(other) {
    if (other is! Uri) return false;
    Uri uri = other;
    return scheme == uri.scheme &&
        userInfo == uri.userInfo &&
        host == uri.host &&
        port == uri.port &&
        path == uri.path &&
        query == uri.query &&
        fragment == uri.fragment;
  }

  int get hashCode {
    int combine(part, current) {
      // The sum is truncated to 30 bits to make sure it fits into a Smi.
      return (current * 31 + part.hashCode) & 0x3FFFFFFF;
    }
    return combine(scheme, combine(userInfo, combine(host, combine(port,
        combine(path, combine(query, combine(fragment, 1)))))));
  }

  static void _addIfNonEmpty(StringBuffer sb, String test,
                             String first, String second) {
    if ("" != test) {
      sb.write(first);
      sb.write(second);
    }
  }

  /**
   * Encode the string [component] using percent-encoding to make it
   * safe for literal use as a URI component.
   *
   * All characters except uppercase and lowercase letters, digits and
   * the characters `-_.!~*'()` are percent-encoded. This is the
   * set of characters specified in RFC 2396 and the which is
   * specified for the encodeUriComponent in ECMA-262 version 5.1.
   *
   * When manually encoding path segments or query components remember
   * to encode each part separately before building the path or query
   * string.
   *
   * For encoding the query part consider using
   * [encodeQueryComponent].
   *
   * To avoid the need for explicitly encoding use the [pathSegments]
   * and [queryParameters] optional named arguments when constructing
   * a [Uri].
   */
  static String encodeComponent(String component) {
    return _uriEncode(_unreserved2396Table, component);
  }

  /**
   * Encode the string [component] according to the HTML 4.01 rules
   * for encoding the posting of a HTML form as a query string
   * component.
   *
   * Encode the string [component] according to the HTML 4.01 rules
   * for encoding the posting of a HTML form as a query string
   * component.

   * The component is first encoded to bytes using [encoding].
   * The default is to use [UTF8] encoding, which preserves all
   * the characters that don't need encoding.

   * Then the resulting bytes are "percent-encoded". This transforms
   * spaces (U+0020) to a plus sign ('+') and all bytes that are not
   * the ASCII decimal digits, letters or one of '-._~' are written as
   * a percent sign '%' followed by the two-digit hexadecimal
   * representation of the byte.

   * Note that the set of characters which are percent-encoded is a
   * superset of what HTML 4.01 requires, since it refers to RFC 1738
   * for reserved characters.
   *
   * When manually encoding query components remember to encode each
   * part separately before building the query string.
   *
   * To avoid the need for explicitly encoding the query use the
   * [queryParameters] optional named arguments when constructing a
   * [Uri].
   *
   * See http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4.2 for more
   * details.
   */
  static String encodeQueryComponent(String component,
                                     {Encoding encoding: UTF8}) {
    return _uriEncode(
        _unreservedTable, component, encoding: encoding, spaceToPlus: true);
  }

  /**
   * Decodes the percent-encoding in [encodedComponent].
   *
   * Note that decoding a URI component might change its meaning as
   * some of the decoded characters could be characters with are
   * delimiters for a given URI componene type. Always split a URI
   * component using the delimiters for the component before decoding
   * the individual parts.
   *
   * For handling the [path] and [query] components consider using
   * [pathSegments] and [queryParameters] to get the separated and
   * decoded component.
   */
  static String decodeComponent(String encodedComponent) {
    return _uriDecode(encodedComponent);
  }

  /**
   * Decodes the percent-encoding in [encodedComponent], converting
   * pluses to spaces.
   *
   * It will create a byte-list of the decoded characters, and then use
   * [encoding] to decode the byte-list to a String. The default encoding is
   * UTF-8.
   */
  static String decodeQueryComponent(
      String encodedComponent,
      {Encoding encoding: UTF8}) {
    return _uriDecode(encodedComponent, plusToSpace: true, encoding: encoding);
  }

  /**
   * Encode the string [uri] using percent-encoding to make it
   * safe for literal use as a full URI.
   *
   * All characters except uppercase and lowercase letters, digits and
   * the characters `!#$&'()*+,-./:;=?@_~` are percent-encoded. This
   * is the set of characters specified in in ECMA-262 version 5.1 for
   * the encodeURI function .
   */
  static String encodeFull(String uri) {
    return _uriEncode(_encodeFullTable, uri);
  }

  /**
   * Decodes the percent-encoding in [uri].
   *
   * Note that decoding a full URI might change its meaning as some of
   * the decoded characters could be reserved characters. In most
   * cases an encoded URI should be parsed into components using
   * [Uri.parse] before decoding the separate components.
   */
  static String decodeFull(String uri) {
    return _uriDecode(uri);
  }

  /**
   * Returns the [query] split into a map according to the rules
   * specified for FORM post in the
   * [HTML 4.01 specification section 17.13.4]
   * (http://www.w3.org/TR/REC-html40/interact/forms.html#h-17.13.4
   * "HTML 4.01 section 17.13.4"). Each key and value in the returned
   * map has been decoded. If the [query]
   * is the empty string an empty map is returned.
   *
   * Keys in the query string that have no value are mapped to the
   * empty string.
   *
   * Each query component will be decoded using [encoding]. The default encoding
   * is UTF-8.
   */
  static Map<String, String> splitQueryString(String query,
                                              {Encoding encoding: UTF8}) {
    return query.split("&").fold({}, (map, element) {
      int index = element.indexOf("=");
      if (index == -1) {
        if (element != "") {
          map[decodeQueryComponent(element, encoding: encoding)] = "";
        }
      } else if (index != 0) {
        var key = element.substring(0, index);
        var value = element.substring(index + 1);
        map[Uri.decodeQueryComponent(key, encoding: encoding)] =
            decodeQueryComponent(value, encoding: encoding);
      }
      return map;
    });
  }

  /**
   * Parse the [host] as an IP version 4 (IPv4) address, returning the address
   * as a list of 4 bytes in network byte order (big endian).
   *
   * Throws a [FormatException] if [host] is not a valid IPv4 address
   * representation.
   */
  static List<int> parseIPv4Address(String host) {
    void error(String msg) {
      throw new FormatException('Illegal IPv4 address, $msg');
    }
    var bytes = host.split('.');
    if (bytes.length != 4) {
      error('IPv4 address should contain exactly 4 parts');
    }
    // TODO(ajohnsen): Consider using Uint8List.
    return bytes
        .map((byteString) {
          int byte = int.parse(byteString);
          if (byte < 0 || byte > 255) {
            error('each part must be in the range of `0..255`');
          }
          return byte;
        })
        .toList();
  }

  /**
   * Parse the [host] as an IP version 6 (IPv6) address, returning the address
   * as a list of 16 bytes in network byte order (big endian).
   *
   * Throws a [FormatException] if [host] is not a valid IPv6 address
   * representation.
   *
   * Some examples of IPv6 addresses:
   *  * ::1
   *  * FEDC:BA98:7654:3210:FEDC:BA98:7654:3210
   *  * 3ffe:2a00:100:7031::1
   *  * ::FFFF:129.144.52.38
   *  * 2010:836B:4179::836B:4179
   */
  static List<int> parseIPv6Address(String host) {
    // An IPv6 address consists of exactly 8 parts of 1-4 hex digits, seperated
    // by `:`'s, with the following exceptions:
    //
    //  - One (and only one) wildcard (`::`) may be present, representing a fill
    //    of 0's. The IPv6 `::` is thus 16 bytes of `0`.
    //  - The last two parts may be replaced by an IPv4 address.
    void error(String msg) {
      throw new FormatException('Illegal IPv6 address, $msg');
    }
    int parseHex(int start, int end) {
      if (end - start > 4) {
        error('an IPv6 part can only contain a maximum of 4 hex digits');
      }
      int value = int.parse(host.substring(start, end), radix: 16);
      if (value < 0 || value > (1 << 16) - 1) {
        error('each part must be in the range of `0x0..0xFFFF`');
      }
      return value;
    }
    if (host.length < 2) error('address is too short');
    List<int> parts = [];
    bool wildcardSeen = false;
    int partStart = 0;
    // Parse all parts, except a potential last one.
    for (int i = 0; i < host.length; i++) {
      if (host.codeUnitAt(i) == _COLON) {
        if (i == 0) {
          // If we see a `:` in the beginning, expect wildcard.
          i++;
          if (host.codeUnitAt(i) != _COLON) {
            error('invalid start colon.');
          }
          partStart = i;
        }
        if (i == partStart) {
          // Wildcard. We only allow one.
          if (wildcardSeen) {
            error('only one wildcard `::` is allowed');
          }
          wildcardSeen = true;
          parts.add(-1);
        } else {
          // Found a single colon. Parse [partStart..i] as a hex entry.
          parts.add(parseHex(partStart, i));
        }
        partStart = i + 1;
      }
    }
    if (parts.length == 0) error('too few parts');
    bool atEnd = partStart == host.length;
    bool isLastWildcard = parts.last == -1;
    if (atEnd && !isLastWildcard) {
      error('expected a part after last `:`');
    }
    if (!atEnd) {
      try {
        parts.add(parseHex(partStart, host.length));
      } catch (e) {
        // Failed to parse the last chunk as hex. Try IPv4.
        try {
          List<int> last = parseIPv4Address(host.substring(partStart));
          parts.add(last[0] << 8 | last[1]);
          parts.add(last[2] << 8 | last[3]);
        } catch (e) {
          error('invalid end of IPv6 address.');
        }
      }
    }
    if (wildcardSeen) {
      if (parts.length > 7) {
        error('an address with a wildcard must have less than 7 parts');
      }
    } else if (parts.length != 8) {
      error('an address without a wildcard must contain exactly 8 parts');
    }
    // TODO(ajohnsen): Consider using Uint8List.
    return parts
        .expand((value) {
          if (value == -1) {
            return new List.filled((9 - parts.length) * 2, 0);
          } else {
            return [(value >> 8) & 0xFF, value & 0xFF];
          }
        })
        .toList();
  }

  // Frequently used character codes.
  static const int _SPACE = 0x20;
  static const int _DOUBLE_QUOTE = 0x22;
  static const int _NUMBER_SIGN = 0x23;
  static const int _PERCENT = 0x25;
  static const int _ASTERISK = 0x2A;
  static const int _PLUS = 0x2B;
  static const int _SLASH = 0x2F;
  static const int _ZERO = 0x30;
  static const int _NINE = 0x39;
  static const int _COLON = 0x3A;
  static const int _LESS = 0x3C;
  static const int _GREATER = 0x3E;
  static const int _QUESTION = 0x3F;
  static const int _AT_SIGN = 0x40;
  static const int _UPPER_CASE_A = 0x41;
  static const int _UPPER_CASE_F = 0x46;
  static const int _UPPER_CASE_Z = 0x5A;
  static const int _LEFT_BRACKET = 0x5B;
  static const int _BACKSLASH = 0x5C;
  static const int _RIGHT_BRACKET = 0x5D;
  static const int _LOWER_CASE_A = 0x61;
  static const int _LOWER_CASE_F = 0x66;
  static const int _LOWER_CASE_Z = 0x7A;
  static const int _BAR = 0x7C;

  /**
   * This is the internal implementation of JavaScript's encodeURI function.
   * It encodes all characters in the string [text] except for those
   * that appear in [canonicalTable], and returns the escaped string.
   */
  static String _uriEncode(List<int> canonicalTable,
                           String text,
                           {Encoding encoding: UTF8,
                            bool spaceToPlus: false}) {
    byteToHex(byte, buffer) {
      const String hex = '0123456789ABCDEF';
      buffer.writeCharCode(hex.codeUnitAt(byte >> 4));
      buffer.writeCharCode(hex.codeUnitAt(byte & 0x0f));
    }

    // Encode the string into bytes then generate an ASCII only string
    // by percent encoding selected bytes.
    StringBuffer result = new StringBuffer();
    var bytes = encoding.encode(text);
    for (int i = 0; i < bytes.length; i++) {
      int byte = bytes[i];
      if (byte < 128 &&
          ((canonicalTable[byte >> 4] & (1 << (byte & 0x0f))) != 0)) {
        result.writeCharCode(byte);
      } else if (spaceToPlus && byte == _SPACE) {
        result.writeCharCode(_PLUS);
      } else {
        result.writeCharCode(_PERCENT);
        byteToHex(byte, result);
      }
    }
    return result.toString();
  }

  /**
   * Convert a byte (2 character hex sequence) in string [s] starting
   * at position [pos] to its ordinal value
   */
  static int _hexCharPairToByte(String s, int pos) {
    int byte = 0;
    for (int i = 0; i < 2; i++) {
      var charCode = s.codeUnitAt(pos + i);
      if (0x30 <= charCode && charCode <= 0x39) {
        byte = byte * 16 + charCode - 0x30;
      } else {
        // Check ranges A-F (0x41-0x46) and a-f (0x61-0x66).
        charCode |= 0x20;
        if (0x61 <= charCode && charCode <= 0x66) {
          byte = byte * 16 + charCode - 0x57;
        } else {
          throw new ArgumentError("Invalid URL encoding");
        }
      }
    }
    return byte;
  }

  /**
   * Uri-decode a percent-encoded string.
   *
   * It unescapes the string [text] and returns the unescaped string.
   *
   * This function is similar to the JavaScript-function `decodeURI`.
   *
   * If [plusToSpace] is `true`, plus characters will be converted to spaces.
   *
   * The decoder will create a byte-list of the percent-encoded parts, and then
   * decode the byte-list using [encoding]. The default encodingis UTF-8.
   */
  static String _uriDecode(String text,
                           {bool plusToSpace: false,
                            Encoding encoding: UTF8}) {
    // First check whether there is any characters which need special handling.
    bool simple = true;
    for (int i = 0; i < text.length && simple; i++) {
      var codeUnit = text.codeUnitAt(i);
      simple = codeUnit != _PERCENT && codeUnit != _PLUS;
    }
    List<int> bytes;
    if (simple) {
      if (encoding == UTF8 || encoding == LATIN1) {
        return text;
      } else {
        bytes = text.codeUnits;
      }
    } else {
      bytes = new List();
      for (int i = 0; i < text.length; i++) {
        var codeUnit = text.codeUnitAt(i);
        if (codeUnit > 127) {
          throw new ArgumentError("Illegal percent encoding in URI");
        }
        if (codeUnit == _PERCENT) {
          if (i + 3 > text.length) {
            throw new ArgumentError('Truncated URI');
          }
          bytes.add(_hexCharPairToByte(text, i + 1));
          i += 2;
        } else if (plusToSpace && codeUnit == _PLUS) {
          bytes.add(_SPACE);
        } else {
          bytes.add(codeUnit);
        }
      }
    }
    return encoding.decode(bytes);
  }

  static bool _isAlphabeticCharacter(int codeUnit)
    => (codeUnit >= _LOWER_CASE_A && codeUnit <= _LOWER_CASE_Z) ||
       (codeUnit >= _UPPER_CASE_A && codeUnit <= _UPPER_CASE_Z);

  // Tables of char-codes organized as a bit vector of 128 bits where
  // each bit indicate whether a character code on the 0-127 needs to
  // be escaped or not.

  // The unreserved characters of RFC 3986.
  static const _unreservedTable = const [
                //             LSB            MSB
                //              |              |
      0x0000,   // 0x00 - 0x0f  0000000000000000
      0x0000,   // 0x10 - 0x1f  0000000000000000
                //                           -.
      0x6000,   // 0x20 - 0x2f  0000000000000110
                //              0123456789
      0x03ff,   // 0x30 - 0x3f  1111111111000000
                //               ABCDEFGHIJKLMNO
      0xfffe,   // 0x40 - 0x4f  0111111111111111
                //              PQRSTUVWXYZ    _
      0x87ff,   // 0x50 - 0x5f  1111111111100001
                //               abcdefghijklmno
      0xfffe,   // 0x60 - 0x6f  0111111111111111
                //              pqrstuvwxyz   ~
      0x47ff];  // 0x70 - 0x7f  1111111111100010

  // The unreserved characters of RFC 2396.
  static const _unreserved2396Table = const [
                //             LSB            MSB
                //              |              |
      0x0000,   // 0x00 - 0x0f  0000000000000000
      0x0000,   // 0x10 - 0x1f  0000000000000000
                //               !     '()*  -.
      0x6782,   // 0x20 - 0x2f  0100000111100110
                //              0123456789
      0x03ff,   // 0x30 - 0x3f  1111111111000000
                //               ABCDEFGHIJKLMNO
      0xfffe,   // 0x40 - 0x4f  0111111111111111
                //              PQRSTUVWXYZ    _
      0x87ff,   // 0x50 - 0x5f  1111111111100001
                //               abcdefghijklmno
      0xfffe,   // 0x60 - 0x6f  0111111111111111
                //              pqrstuvwxyz   ~
      0x47ff];  // 0x70 - 0x7f  1111111111100010

  // Table of reserved characters specified by ECMAScript 5.
  static const _encodeFullTable = const [
                //             LSB            MSB
                //              |              |
      0x0000,   // 0x00 - 0x0f  0000000000000000
      0x0000,   // 0x10 - 0x1f  0000000000000000
                //               ! #$ &'()*+,-./
      0xf7da,   // 0x20 - 0x2f  0101101111101111
                //              0123456789:; = ?
      0xafff,   // 0x30 - 0x3f  1111111111110101
                //              @ABCDEFGHIJKLMNO
      0xffff,   // 0x40 - 0x4f  1111111111111111
                //              PQRSTUVWXYZ    _
      0x87ff,   // 0x50 - 0x5f  1111111111100001
                //               abcdefghijklmno
      0xfffe,   // 0x60 - 0x6f  0111111111111111
                //              pqrstuvwxyz   ~
      0x47ff];  // 0x70 - 0x7f  1111111111100010

  // Characters allowed in the scheme.
  static const _schemeTable = const [
                //             LSB            MSB
                //              |              |
      0x0000,   // 0x00 - 0x0f  0000000000000000
      0x0000,   // 0x10 - 0x1f  0000000000000000
                //                         + -.
      0x6800,   // 0x20 - 0x2f  0000000000010110
                //              0123456789
      0x03ff,   // 0x30 - 0x3f  1111111111000000
                //               ABCDEFGHIJKLMNO
      0xfffe,   // 0x40 - 0x4f  0111111111111111
                //              PQRSTUVWXYZ
      0x07ff,   // 0x50 - 0x5f  1111111111100001
                //               abcdefghijklmno
      0xfffe,   // 0x60 - 0x6f  0111111111111111
                //              pqrstuvwxyz
      0x07ff];  // 0x70 - 0x7f  1111111111100010

  // Characters allowed in scheme except for upper case letters.
  static const _schemeLowerTable = const [
                //             LSB            MSB
                //              |              |
      0x0000,   // 0x00 - 0x0f  0000000000000000
      0x0000,   // 0x10 - 0x1f  0000000000000000
                //                         + -.
      0x6800,   // 0x20 - 0x2f  0000000000010110
                //              0123456789
      0x03ff,   // 0x30 - 0x3f  1111111111000000
                //
      0x0000,   // 0x40 - 0x4f  0111111111111111
                //
      0x0000,   // 0x50 - 0x5f  1111111111100001
                //               abcdefghijklmno
      0xfffe,   // 0x60 - 0x6f  0111111111111111
                //              pqrstuvwxyz
      0x07ff];  // 0x70 - 0x7f  1111111111100010

  // Sub delimiter characters combined with unreserved as of 3986.
  // sub-delims  = "!" / "$" / "&" / "'" / "(" / ")"
  //             / "*" / "+" / "," / ";" / "="
  // RFC 3986 section 2.3.
  // unreserved  = ALPHA / DIGIT / "-" / "." / "_" / "~"
  static const _subDelimitersTable = const [
                //             LSB            MSB
                //              |              |
      0x0000,   // 0x00 - 0x0f  0000000000000000
      0x0000,   // 0x10 - 0x1f  0000000000000000
                //               !  $ &'()*+,-.
      0x7fd2,   // 0x20 - 0x2f  0100101111111110
                //              0123456789 ; =
      0x2bff,   // 0x30 - 0x3f  1111111111010100
                //               ABCDEFGHIJKLMNO
      0xfffe,   // 0x40 - 0x4f  0111111111111111
                //              PQRSTUVWXYZ    _
      0x87ff,   // 0x50 - 0x5f  1111111111100001
                //               abcdefghijklmno
      0xfffe,   // 0x60 - 0x6f  0111111111111111
                //              pqrstuvwxyz   ~
      0x47ff];  // 0x70 - 0x7f  1111111111100010

  // Characters allowed in the reg-name as of RFC 3986.
  // RFC 3986 Apendix A
  // reg-name = *( unreserved / pct-encoded / sub-delims )
  static const _regNameTable = const [
                //             LSB            MSB
                //              |              |
      0x0000,   // 0x00 - 0x0f  0000000000000000
      0x0000,   // 0x10 - 0x1f  0000000000000000
                //               !  $%&'()*+,-.
      0x7ff2,   // 0x20 - 0x2f  0100111111111110
                //              0123456789 ; =
      0x2bff,   // 0x30 - 0x3f  1111111111010100
                //               ABCDEFGHIJKLMNO
      0xfffe,   // 0x40 - 0x4f  0111111111111111
                //              PQRSTUVWXYZ    _
      0x87ff,   // 0x50 - 0x5f  1111111111100001
                //               abcdefghijklmno
      0xfffe,   // 0x60 - 0x6f  0111111111111111
                //              pqrstuvwxyz   ~
      0x47ff];  // 0x70 - 0x7f  1111111111100010

  // Characters allowed in the path as of RFC 3986.
  // RFC 3986 section 3.3.
  // pchar = unreserved / pct-encoded / sub-delims / ":" / "@"
  static const _pathCharTable = const [
                //             LSB            MSB
                //              |              |
      0x0000,   // 0x00 - 0x0f  0000000000000000
      0x0000,   // 0x10 - 0x1f  0000000000000000
                //               !  $ &'()*+,-.
      0x7fd2,   // 0x20 - 0x2f  0100101111111110
                //              0123456789:; =
      0x2fff,   // 0x30 - 0x3f  1111111111110100
                //              @ABCDEFGHIJKLMNO
      0xffff,   // 0x40 - 0x4f  1111111111111111
                //              PQRSTUVWXYZ    _
      0x87ff,   // 0x50 - 0x5f  1111111111100001
                //               abcdefghijklmno
      0xfffe,   // 0x60 - 0x6f  0111111111111111
                //              pqrstuvwxyz   ~
      0x47ff];  // 0x70 - 0x7f  1111111111100010

  // Characters allowed in the query as of RFC 3986.
  // RFC 3986 section 3.4.
  // query = *( pchar / "/" / "?" )
  static const _queryCharTable = const [
                //             LSB            MSB
                //              |              |
      0x0000,   // 0x00 - 0x0f  0000000000000000
      0x0000,   // 0x10 - 0x1f  0000000000000000
                //               !  $ &'()*+,-./
      0xffd2,   // 0x20 - 0x2f  0100101111111111
                //              0123456789:; = ?
      0xafff,   // 0x30 - 0x3f  1111111111110101
                //              @ABCDEFGHIJKLMNO
      0xffff,   // 0x40 - 0x4f  1111111111111111
                //              PQRSTUVWXYZ    _
      0x87ff,   // 0x50 - 0x5f  1111111111100001
                //               abcdefghijklmno
      0xfffe,   // 0x60 - 0x6f  0111111111111111
                //              pqrstuvwxyz   ~
      0x47ff];  // 0x70 - 0x7f  1111111111100010
}

class _UnmodifiableMap<K, V> implements Map<K, V> {
  final Map _map;
  const _UnmodifiableMap(this._map);

  bool containsValue(Object value) => _map.containsValue(value);
  bool containsKey(Object key) => _map.containsKey(key);
  V operator [](Object key) => _map[key];
  void operator []=(K key, V value) {
    throw new UnsupportedError("Cannot modify an unmodifiable map");
  }
  V putIfAbsent(K key, V ifAbsent()) {
    throw new UnsupportedError("Cannot modify an unmodifiable map");
  }
  addAll(Map other) {
    throw new UnsupportedError("Cannot modify an unmodifiable map");
  }
  V remove(Object key) {
    throw new UnsupportedError("Cannot modify an unmodifiable map");
  }
  void clear() {
    throw new UnsupportedError("Cannot modify an unmodifiable map");
  }
  void forEach(void f(K key, V value)) => _map.forEach(f);
  Iterable<K> get keys => _map.keys;
  Iterable<V> get values => _map.values;
  int get length => _map.length;
  bool get isEmpty => _map.isEmpty;
  bool get isNotEmpty => _map.isNotEmpty;
}
