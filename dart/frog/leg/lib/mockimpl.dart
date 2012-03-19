// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Mocks of classes and interfaces that Leg cannot read directly.

// TODO(ahe): Remove this file.

class JSSyntaxRegExp implements RegExp {
  final String pattern;
  final bool multiLine;
  final bool ignoreCase;
  final RegExpWrapper _re;

  const JSSyntaxRegExp(String pattern,
                       [bool multiLine = false, bool ignoreCase = false])
    // TODO(ahe): Redirect to _internal when that is supported.
    : this.pattern = pattern,
      this.multiLine = multiLine,
      this.ignoreCase = ignoreCase,
      this._re = const RegExpWrapper(pattern, multiLine, ignoreCase, false);

  const JSSyntaxRegExp._internal(String pattern, bool multiLine,
                                 bool ignoreCase, bool global)
    : this.pattern = pattern,
      this.multiLine = multiLine,
      this.ignoreCase = ignoreCase,
      this._re = const RegExpWrapper(pattern, multiLine, ignoreCase, global);

  Match firstMatch(String str) {
    List<String> m = _re.exec(str);
    if (m === null) return null;
    var matchStart = RegExpWrapper.matchStart(m);
    // m.lastIndex only works with flag 'g'.
    var matchEnd = matchStart + m[0].length;
    return new MatchImplementation(pattern, str, matchStart, matchEnd, m);
  }

  bool hasMatch(String str) => _re.test(str);

  String stringMatch(String str) {
    var match = firstMatch(str);
    return match === null ? null : match.group(0);
  }

  Iterable<Match> allMatches(String str) {
    checkString(str);
    return new _AllMatchesIterable(this, str);
  }

  /**
   * Returns a new RegExp with the same pattern as this one and with the
   * "global" flag set. This allows us to match this RegExp against a string
   * multiple times, to support things like [allMatches] and
   * [String.replaceAll].
   *
   * Note that the returned RegExp disobeys the normal API in that it maintains
   * state about the location of the last match.
   */
  JSSyntaxRegExp get _global() {
    return new JSSyntaxRegExp._internal(pattern, multiLine, ignoreCase, true);
  }
}

class MatchImplementation implements Match {
  const MatchImplementation(
      String this.pattern,
      String this.str,
      int this._start,
      int this._end,
      List<String> this._groups);

  final String pattern;
  final String str;
  final int _start;
  final int _end;
  final List<String> _groups;

  int start() => _start;
  int end() => _end;
  String group(int index) => _groups[index];
  String operator [](int index) => group(index);
  int groupCount() => _groups.length - 1;

  List<String> groups(List<int> groups) {
    List<String> out = [];
    for (int i in groups) {
      out.add(group(i));
    }
    return out;
  }
}

class _AllMatchesIterable implements Iterable<Match> {
  final JSSyntaxRegExp _re;
  final String _str;

  const _AllMatchesIterable(this._re, this._str);

  Iterator<Match> iterator() => new _AllMatchesIterator(_re, _str);
}

class _AllMatchesIterator implements Iterator<Match> {
  final RegExp _re;
  final String _str;
  Match _next;
  bool _done;

  _AllMatchesIterator(JSSyntaxRegExp re, String this._str)
    : _done = false, _re = re._global;

  Match next() {
    if (!hasNext()) {
      throw const NoMoreElementsException();
    }

    // _next is set by #hasNext
    var next = _next;
    _next = null;
    return next;
  }

  bool hasNext() {
    if (_done) {
      return false;
    } else if (_next != null) {
      return true;
    }

    _next = _re.firstMatch(_str);
    if (_next == null) {
      _done = true;
      return false;
    } else {
      return true;
    }
  }
}

class ReceivePortFactory {
  factory ReceivePort() {
    throw 'factory ReceivePort is not implemented';
  }
}

class StringBase {
  static String createFromCharCodes(List<int> charCodes) {
    checkNull(charCodes);
    if (!isJsArray(charCodes)) {
      if (charCodes is !List) throw new IllegalArgumentException(charCodes);
      charCodes = new List.from(charCodes);
    }
    return Primitives.stringFromCharCodes(charCodes);
  }

  static String join(List<String> strings, String separator) {
    checkNull(strings);
    checkNull(separator);
    var result = "";
    var first = true;
    for (var string in strings) {
      checkNull(string);
      if (string is !String) throw new IllegalArgumentException(string);
      if (!first) result += separator; // TODO(ahe): Use string buffer.
      result += string; // TODO(ahe): Use string buffer.
      first = false;
    }
    return result;
  }

  static String concatAll(List<String> strings) {
    checkNull(strings);
    var result = "";
    for (var string in strings) {
      checkNull(string);
      if (string is !String) throw new IllegalArgumentException(string);
      result += string; // TODO(ahe): Use string buffer.
    }
    return result;
  }
}

class TimeZoneImplementation implements TimeZone {
  const TimeZoneImplementation.utc() : isUtc = true;
  TimeZoneImplementation.local() : isUtc = false;

  bool operator ==(Object other) {
    if (!(other is TimeZoneImplementation)) return false;
    return isUtc == other.isUtc;
  }

  final bool isUtc;
}

class DateImplementation implements Date {
  final int value;
  final TimeZoneImplementation timeZone;

  factory DateImplementation(int years,
                             int month,
                             int day,
                             int hours,
                             int minutes,
                             int seconds,
                             int milliseconds) {
    return new DateImplementation.withTimeZone(
        years, month, day,
        hours, minutes, seconds, milliseconds,
        new TimeZoneImplementation.local());
  }

  DateImplementation.withTimeZone(int years,
                                  int month,
                                  int day,
                                  int hours,
                                  int minutes,
                                  int seconds,
                                  int milliseconds,
                                  TimeZoneImplementation timeZone)
      : this.timeZone = checkNull(timeZone),
        value = Primitives.valueFromDecomposedDate(years, month, day,
                                                   hours, minutes, seconds,
                                                   milliseconds,
                                                   timeZone.isUtc) {
    _asJs();
  }

  DateImplementation.now()
      : timeZone = new TimeZone.local(),
        value = Primitives.dateNow() {
    _asJs();
  }

  factory DateImplementation.fromString(String formattedString) {
    // Read in (a subset of) ISO 8601.
    // Examples:
    //    - "2012-02-27 13:27:00"
    //    - "2012-02-27 13:27:00.423z"
    //    - "20120227 13:27:00"
    //    - "20120227T132700"
    //    - "20120227"
    //    - "2012-02-27T14Z"
    //    - "-123450101 00:00:00 Z"  // In the year -12345.
    final RegExp re = const RegExp(
        @'^([+-]?\d?\d\d\d\d)-?(\d\d)-?(\d\d)' // The day part.
        @'(?:[ T](\d\d)(?::?(\d\d)(?::?(\d\d)(?:.(\d{1,5}))?)?)? ?([zZ])?)?$');
    Match match = re.firstMatch(formattedString);
    if (match !== null) {
      int parseIntOrZero(String matched) {
        // TODO(floitsch): we should not need to test against the empty string.
        if (matched === null || matched == "") return 0;
        return Math.parseInt(matched);
      }

      int years = Math.parseInt(match[1]);
      int month = Math.parseInt(match[2]);
      int day = Math.parseInt(match[3]);
      int hours = parseIntOrZero(match[4]);
      int minutes = parseIntOrZero(match[5]);
      int seconds = parseIntOrZero(match[6]);
      bool addOneMillisecond = false;
      int milliseconds = parseIntOrZero(match[7]);
      if (milliseconds != 0) {
        if (match[7].length == 1) {
          milliseconds *= 100;
        } else if (match[7].length == 2) {
          milliseconds *= 10;
        } else if (match[7].length == 3) {
          // Do nothing.
        } else if (match[7].length == 4) {
          addOneMillisecond = ((milliseconds % 10) >= 5);
          milliseconds ~/= 10;
        } else {
          assert(match[7].length == 5);
          addOneMillisecond = ((milliseconds %100) >= 50);
          milliseconds ~/= 100;
        }
        if (addOneMillisecond && milliseconds < 999) {
          addOneMillisecond = false;
          milliseconds++;
        }
      }
      // TODO(floitsch): we should not need to test against the empty string.
      bool isUtc = (match[8] !== null) && (match[8] != "");
      TimeZone timezone = isUtc ? const TimeZone.utc() : new TimeZone.local();
      int epochValue = Primitives.valueFromDecomposedDate(
          years, month, day, hours, minutes, seconds, milliseconds, isUtc);
      if (epochValue === null) {
        throw new IllegalArgumentException(formattedString);
      }
      if (addOneMillisecond) epochValue++;
      return new DateImplementation.fromEpoch(epochValue, timezone);
    } else {
      throw new IllegalArgumentException(formattedString);
    }
  }

  const DateImplementation.fromEpoch(this.value, this.timeZone);

  bool operator ==(other) {
    if (!(other is DateImplementation)) return false;
    return (value == other.value) && (timeZone == other.timeZone);
  }

  int compareTo(Date other) {
    checkNull(other);
    return value.compareTo(other.value);
  }

  Date changeTimeZone(TimeZone targetTimeZone) {
    if (targetTimeZone == null) {
      targetTimeZone = new TimeZoneImplementation.local();
    }
    return new Date.fromEpoch(value, targetTimeZone);
  }

  int get year() => Primitives.getYear(this);

  int get month() => Primitives.getMonth(this);

  int get day() => Primitives.getDay(this);

  int get hours() => Primitives.getHours(this);

  int get minutes() => Primitives.getMinutes(this);

  int get seconds() => Primitives.getSeconds(this);

  int get milliseconds() => Primitives.getMilliseconds(this);

  int get weekday() {
    // Adjust by one because JS weeks start on Sunday.
    var day = Primitives.getWeekday(this);
    return (day + 6) % 7;
  }

  bool isLocalTime() {
    return !timeZone.isUtc;
  }

  bool isUtc() {
    return timeZone.isUtc;
  }

  String toString() {
    String y = fourDigits(year);
    String m = twoDigits(month);
    String d = twoDigits(day);
    String h = twoDigits(hours);
    String min = twoDigits(minutes);
    String sec = twoDigits(seconds);
    String ms = threeDigits(milliseconds);
    if (timeZone.isUtc) {
      return "$y-$m-$d $h:$min:$sec.${ms}Z";
    } else {
      return "$y-$m-$d $h:$min:$sec.$ms";
    }
  }

  // Adds the [duration] to this Date instance.
  Date add(Duration duration) {
    checkNull(duration);
    return new DateImplementation.fromEpoch(value + duration.inMilliseconds,
                                            timeZone);
  }

  // Subtracts the [duration] from this Date instance.
  Date subtract(Duration duration) {
    checkNull(duration);
    return new DateImplementation.fromEpoch(value - duration.inMilliseconds,
                                            timeZone);
  }

  // Returns a [Duration] with the difference of [this] and [other].
  Duration difference(Date other) {
    checkNull(other);
    return new Duration(milliseconds: value - other.value);
  }

  // Lazily keep a JS Date stored in the dart object.
  var _asJs() => Primitives.lazyAsJsDate(this);
}

class ListFactory<E> implements List<E> {
  factory List([int length]) => Primitives.newList(length);
  factory List.from(Iterable<E> other) {
    List<E> result = new List<E>();
    // TODO(ahe): Use for-in when it is implemented correctly.
    Iterator<E> iterator = other.iterator();
    while (iterator.hasNext()) {
      result.add(iterator.next());
    }
    return result;
  }
}

class DurationImplementation implements Duration {
  final int inMilliseconds;

  const DurationImplementation([int days = 0,
                                int hours = 0,
                                int minutes = 0,
                                int seconds = 0,
                                int milliseconds = 0])
    : inMilliseconds = days * Duration.MILLISECONDS_PER_DAY +
                       hours * Duration.MILLISECONDS_PER_HOUR +
                       minutes * Duration.MILLISECONDS_PER_MINUTE +
                       seconds * Duration.MILLISECONDS_PER_SECOND +
                       milliseconds;

  int get inDays() {
    return inMilliseconds ~/ Duration.MILLISECONDS_PER_DAY;
  }

  int get inHours() {
    return inMilliseconds ~/ Duration.MILLISECONDS_PER_HOUR;
  }

  int get inMinutes() {
    return inMilliseconds ~/ Duration.MILLISECONDS_PER_MINUTE;
  }

  int get inSeconds() {
    return inMilliseconds ~/ Duration.MILLISECONDS_PER_SECOND;
  }

  bool operator ==(other) {
    if (other is !Duration) return false;
    return inMilliseconds == other.inMilliseconds;
  }

  int hashCode() {
    return inMilliseconds.hashCode();
  }

  int compareTo(Duration other) {
    return inMilliseconds.compareTo(other.inMilliseconds);
  }

  String toString() {
    if (inMilliseconds < 0) {
      Duration duration =
          new DurationImplementation(milliseconds: -inMilliseconds);
      return "-${duration}";
    }
    String twoDigitMinutes =
        twoDigits(inMinutes.remainder(Duration.MINUTES_PER_HOUR));
    String twoDigitSeconds =
        twoDigits(inSeconds.remainder(Duration.SECONDS_PER_MINUTE));
    String threeDigitMs =
        threeDigits(inMilliseconds.remainder(Duration.MILLISECONDS_PER_SECOND));
    return "${inHours}:${twoDigitMinutes}:${twoDigitSeconds}.${threeDigitMs}";
  }
}
