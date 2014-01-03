library url_template;

import 'url_matcher.dart';

final _specialChars = new RegExp(r'[\\\(\)\$\^\.\+\[\]\{\}\|]');

/**
 * A reversible URL template class that can match/parse and reverse URL
 * templates like: /foo/:bar/baz
 */
class UrlTemplate implements UrlMatcher {
  List<String> _fields;
  RegExp _pattern;
  List _chunks;

  String toString() {
    return '$_pattern';
  }

  UrlTemplate(String template) {
    _compileTemplate(template);
  }

  void _compileTemplate(String template) {
    template = template.
        replaceAllMapped(_specialChars, (m) => r'\' + m.group(0));
    _fields = <String>[];
    _chunks = [];
    var exp = new RegExp(r':([\w0-9]+)');
    StringBuffer sb = new StringBuffer('^');
    int start = 0;
    exp.allMatches(template).forEach((Match m) {
      var paramName = m.group(1);
      var txt = template.substring(start, m.start);
      _fields.add(paramName);
      _chunks.add(txt);
      _chunks.add((Map params) => params != null ? params[paramName] : null);
       sb.write(txt);
      sb.write(r'([^/?]+)');
      start = m.end;
    });
    if (start != template.length) {
      var txt = template.substring(start, template.length);
      sb.write(txt);
      _chunks.add(txt);
    }
    _pattern = new RegExp(sb.toString());
  }

  UrlMatch match(String url) {
    var matches = _pattern.allMatches(url);
    if (matches.isEmpty) {
      return null;
    }
    var parameters = new Map();
    Match match = matches.first;
    for (var i = 0; i < match.groupCount; i++) {
      parameters[_fields[i]] = match.group(i + 1);
    }
    var tail = url.substring(match.group(0).length);
    return new UrlMatch(match.group(0), tail, parameters);
  }

  String reverse({Map parameters, String tail: ''}) =>
    _chunks.map((c) => c is Function ? c(parameters) : c).join() + tail;

  List<String> urlParameterNames() {
    return _fields;
  }
}