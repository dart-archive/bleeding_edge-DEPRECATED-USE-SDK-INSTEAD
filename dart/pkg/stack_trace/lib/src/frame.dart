// Copyright (c) 2013, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library frame;


import 'package:path/path.dart' as path;

import 'trace.dart';

// #1      Foo._bar (file:///home/nweiz/code/stuff.dart:42:21)
final _vmFrame = new RegExp(
    r'^#\d+\s+(\S.*) \((.+?):(\d+)(?::(\d+))?\)$');

//     at VW.call$0 (http://pub.dartlang.org/stuff.dart.js:560:28)
//     at VW.call$0 (eval as fn
//         (http://pub.dartlang.org/stuff.dart.js:560:28), efn:3:28)
//     at http://pub.dartlang.org/stuff.dart.js:560:28
final _v8Frame = new RegExp(
    r'^\s*at (?:(\S.*?)(?: \[as [^\]]+\])? \((.*)\)|(.*))$');

// http://pub.dartlang.org/stuff.dart.js:560:28
final _v8UrlLocation = new RegExp(r'^(.*):(\d+):(\d+)$');

// eval as function (http://pub.dartlang.org/stuff.dart.js:560:28), efn:3:28
// eval as function (http://pub.dartlang.org/stuff.dart.js:560:28)
// eval as function (eval as otherFunction
//     (http://pub.dartlang.org/stuff.dart.js:560:28))
final _v8EvalLocation = new RegExp(
    r'^eval at (?:\S.*?) \((.*)\)(?:, .*?:\d+:\d+)?$');

// foo$bar$0@http://pub.dartlang.org/stuff.dart.js:560:28
// http://pub.dartlang.org/stuff.dart.js:560:28
final _safariFrame = new RegExp(r"^(?:([0-9A-Za-z_$]*)@)?(.*):(\d*):(\d*)$");

// .VW.call$0@http://pub.dartlang.org/stuff.dart.js:560
// .VW.call$0("arg")@http://pub.dartlang.org/stuff.dart.js:560
// .VW.call$0/name<@http://pub.dartlang.org/stuff.dart.js:560
final _firefoxFrame = new RegExp(
    r'^([^@(/]*)(?:\(.*\))?((?:/[^/]*)*)(?:\(.*\))?@(.*):(\d+)$');

// foo/bar.dart 10:11 in Foo._bar
// http://dartlang.org/foo/bar.dart in Foo._bar
final _friendlyFrame = new RegExp(
    r'^(\S+)(?: (\d+)(?::(\d+))?)?\s+([^\d]\S*)$');

final _initialDot = new RegExp(r"^\.");

/// "dart:" libraries that are incorrectly reported without a "dart:" prefix.
///
/// See issue 11901. All these libraries should be in "dart:io".
final _ioLibraries = new Set.from([
  new Uri(path: 'timer_impl.dart'),
  new Uri(path: 'http_impl.dart'),
  new Uri(path: 'http_parser.dart')
]);

/// A single stack frame. Each frame points to a precise location in Dart code.
class Frame {
  /// The URI of the file in which the code is located.
  ///
  /// This URI will usually have the scheme `dart`, `file`, `http`, or `https`.
  final Uri uri;

  /// The line number on which the code location is located.
  ///
  /// This can be null, indicating that the line number is unknown or
  /// unimportant.
  final int line;

  /// The column number of the code location.
  ///
  /// This can be null, indicating that the column number is unknown or
  /// unimportant.
  final int column;

  /// The name of the member in which the code location occurs.
  ///
  /// Anonymous closures are represented as `<fn>` in this member string.
  final String member;

  /// Whether this stack frame comes from the Dart core libraries.
  bool get isCore => uri.scheme == 'dart';

  /// Returns a human-friendly description of the library that this stack frame
  /// comes from.
  ///
  /// This will usually be the string form of [uri], but a relative URI will be
  /// used if possible.
  String get library {
    if (uri.scheme != Uri.base.scheme) return uri.toString();
    if (path.style == path.Style.url) return path.relative(uri.toString());
    return path.relative(path.fromUri(uri));
  }

  /// Returns the name of the package this stack frame comes from, or `null` if
  /// this stack frame doesn't come from a `package:` URL.
  String get package {
    if (uri.scheme != 'package') return null;
    return uri.path.split('/').first;
  }

  /// A human-friendly description of the code location.
  String get location {
    if (line == null) return library;
    if (column == null) return '$library $line';
    return '$library $line:$column';
  }

  /// Returns a single frame of the current stack.
  ///
  /// By default, this will return the frame above the current method. If
  /// [level] is `0`, it will return the current method's frame; if [level] is
  /// higher than `1`, it will return higher frames.
  factory Frame.caller([int level=1]) {
    if (level < 0) {
      throw new ArgumentError("Argument [level] must be greater than or equal "
          "to 0.");
    }

    return new Trace.current(level + 1).frames.first;
  }

  /// Parses a string representation of a Dart VM stack frame.
  factory Frame.parseVM(String frame) {
    // The VM sometimes folds multiple stack frames together and replaces them
    // with "...".
    if (frame == '...') {
      return new Frame(new Uri(), null, null, '...');
    }

    var match = _vmFrame.firstMatch(frame);
    if (match == null) {
      throw new FormatException("Couldn't parse VM stack trace line '$frame'.");
    }

    // Get the pieces out of the regexp match. Function, URI and line should
    // always be found. The column is optional.
    var member = match[1].replaceAll("<anonymous closure>", "<fn>");
    var uri = Uri.parse(match[2]);
    if (_ioLibraries.contains(uri)) uri = Uri.parse('dart:io/${uri.path}');
    var line = int.parse(match[3]);
    var column = null;
    var columnMatch = match[4];
    if (columnMatch != null) {
      column = int.parse(columnMatch);
    }
    return new Frame(uri, line, column, member);
  }

  /// Parses a string representation of a Chrome/V8 stack frame.
  factory Frame.parseV8(String frame) {
    var match = _v8Frame.firstMatch(frame);
    if (match == null) {
      throw new FormatException("Couldn't parse V8 stack trace line '$frame'.");
    }

    // v8 location strings can be arbitrarily-nested, since it adds a layer of
    // nesting for each eval performed on that line.
    parseLocation(location, member) {
      var evalMatch = _v8EvalLocation.firstMatch(location);
      while (evalMatch != null) {
        location = evalMatch[1];
        evalMatch = _v8EvalLocation.firstMatch(location);
      }

      var urlMatch = _v8UrlLocation.firstMatch(location);
      if (urlMatch == null) {
        throw new FormatException(
            "Couldn't parse V8 stack trace line '$frame'.");
      }

      return new Frame(
          _uriOrPathToUri(urlMatch[1]),
          int.parse(urlMatch[2]),
          int.parse(urlMatch[3]),
          member);
    }

    // V8 stack frames can be in two forms.
    if (match[2] != null) {
      // The first form looks like " at FUNCTION (LOCATION)". V8 proper lists
      // anonymous functions within eval as "<anonymous>", while IE10 lists them
      // as "Anonymous function".
      return parseLocation(match[2],
          match[1].replaceAll("<anonymous>", "<fn>")
                  .replaceAll("Anonymous function", "<fn>"));
    } else {
      // The second form looks like " at LOCATION", and is used for anonymous
      // functions.
      return parseLocation(match[3], "<fn>");
    }
  }

  /// Parses a string representation of an IE stack frame.
  ///
  /// IE10+ frames look just like V8 frames. Prior to IE10, stack traces can't
  /// be retrieved.
  factory Frame.parseIE(String frame) => new Frame.parseV8(frame);

  /// Parses a string representation of a Firefox stack frame.
  factory Frame.parseFirefox(String frame) {
    var match = _firefoxFrame.firstMatch(frame);
    if (match == null) {
      throw new FormatException(
          "Couldn't parse Firefox stack trace line '$frame'.");
    }

    // Normally this is a URI, but in a jsshell trace it can be a path.
    var uri = _uriOrPathToUri(match[3]);
    var member = match[1];
    member += new List.filled('/'.allMatches(match[2]).length, ".<fn>").join();
    if (member == '') member = '<fn>';

    // Some Firefox members have initial dots. We remove them for consistency
    // with other platforms.
    member = member.replaceFirst(_initialDot, '');
    return new Frame(uri, int.parse(match[4]), null, member);
  }

  /// Parses a string representation of a Safari 6.0 stack frame.
  ///
  /// Safari 6.0 frames look just like Firefox frames. Prior to Safari 6.0,
  /// stack traces can't be retrieved.
  factory Frame.parseSafari6_0(String frame) => new Frame.parseFirefox(frame);

  /// Parses a string representation of a Safari 6.1+ stack frame.
  factory Frame.parseSafari6_1(String frame) {
    var match = _safariFrame.firstMatch(frame);
    if (match == null) {
      throw new FormatException(
          "Couldn't parse Safari stack trace line '$frame'.");
    }

    var uri = Uri.parse(match[2]);
    var member = match[1];
    if (member == null) member = '<fn>';
    var line = match[3] == '' ? null : int.parse(match[3]);
    var column = match[4] == '' ? null : int.parse(match[4]);
    return new Frame(uri, line, column, member);
  }

  /// Parses this package's string representation of a stack frame.
  factory Frame.parseFriendly(String frame) {
    var match = _friendlyFrame.firstMatch(frame);
    if (match == null) {
      throw new FormatException(
          "Couldn't parse package:stack_trace stack trace line '$frame'.");
    }

    var uri = Uri.parse(match[1]);
    // If there's no scheme, this is a relative URI. We should interpret it as
    // relative to the current working directory.
    if (uri.scheme == '') {
      uri = path.toUri(path.absolute(path.fromUri(uri)));
    }

    var line = match[2] == null ? null : int.parse(match[2]);
    var column = match[3] == null ? null : int.parse(match[3]);
    return new Frame(uri, line, column, match[4]);
  }

  /// A regular expression matching an absolute URI.
  static final _uriRegExp = new RegExp(r'^[a-zA-Z][-+.a-zA-Z\d]*://');

  /// A regular expression matching a Windows path.
  static final _windowsRegExp = new RegExp(r'^([a-zA-Z]:[\\/]|\\\\)');

  /// Converts [uriOrPath], which can be a URI, a Windows path, or a Posix path,
  /// to a URI (absolute if possible).
  static Uri _uriOrPathToUri(String uriOrPath) {
    if (uriOrPath.contains(_uriRegExp)) {
      return Uri.parse(uriOrPath);
    } else if (uriOrPath.contains(_windowsRegExp)) {
      return new Uri.file(uriOrPath, windows: true);
    } else if (uriOrPath.startsWith('/')) {
      return new Uri.file(uriOrPath, windows: false);
    }

    // As far as I've seen, Firefox and V8 both always report absolute paths in
    // their stack frames. However, if we do get a relative path, we should
    // handle it gracefully.
    if (uriOrPath.contains('\\')) return path.windows.toUri(uriOrPath);
    return Uri.parse(uriOrPath);
  }

  Frame(this.uri, this.line, this.column, this.member);

  String toString() => '$location in $member';
}
