// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

// Global constants.
class Const {
  // Bytes for "HTTP/1.0".
  static final HTTP10 = const [72, 84, 84, 80, 47, 49, 46, 48];
  // Bytes for "HTTP/1.1".
  static final HTTP11 = const [72, 84, 84, 80, 47, 49, 46, 49];

  static final END_CHUNKED = const [0x30, 13, 10, 13, 10];
}

// Frequently used character codes.
class CharCode {
  static final int HT = 9;
  static final int LF = 10;
  static final int CR = 13;
  static final int SP = 32;
  static final int COLON = 58;
}


// States of the HTTP parser state machine.
class State {
  static final int START = 0;
  static final int METHOD_OR_HTTP_VERSION = 1;
  static final int REQUEST_LINE_METHOD = 2;
  static final int REQUEST_LINE_URI = 3;
  static final int REQUEST_LINE_HTTP_VERSION = 4;
  static final int REQUEST_LINE_ENDING = 5;
  static final int RESPONSE_LINE_STATUS_CODE = 6;
  static final int RESPONSE_LINE_REASON_PHRASE = 7;
  static final int RESPONSE_LINE_ENDING = 8;
  static final int HEADER_START = 9;
  static final int HEADER_FIELD = 10;
  static final int HEADER_VALUE_START = 11;
  static final int HEADER_VALUE = 12;
  static final int HEADER_VALUE_FOLDING_OR_ENDING = 13;
  static final int HEADER_VALUE_FOLD_OR_END = 14;
  static final int HEADER_ENDING = 15;
  static final int CHUNK_SIZE_STARTING_CR = 16;
  static final int CHUNK_SIZE_STARTING_LF = 17;
  static final int CHUNK_SIZE = 18;
  static final int CHUNK_SIZE_ENDING = 19;
  static final int CHUNKED_BODY_DONE_CR = 20;
  static final int CHUNKED_BODY_DONE_LF = 21;
  static final int BODY = 22;
}


class _HTTPException implements HTTPException {
  const _HTTPException(String this.message);
  final String message;
}


/**
 * HTTP parser which parses the HTTP stream as data is supplied
 * through the writeList method. As the data is parsed the events
 *   RequestStart
 *   UriReceived
 *   HeaderReceived
 *   HeadersComplete
 *   DataReceived
 *   DataEnd
 * are generated.
 * Currently only HTTP requests with Content-Length header are supported.
 */
class HTTPParser {
  HTTPParser()
      : _state = State.START,
        _failure = false,
        _headerField = new StringBuffer(),
        _headerValue = new StringBuffer(),
        _method_or_status_code = new StringBuffer(),
        _uri_or_reason_phrase = new StringBuffer();

  // From RFC 2616.
  // generic-message = start-line
  //                   *(message-header CRLF)
  //                   CRLF
  //                   [ message-body ]
  // start-line      = Request-Line | Status-Line
  // Request-Line    = Method SP Request-URI SP HTTP-Version CRLF
  // Status-Line     = HTTP-Version SP Status-Code SP Reason-Phrase CRLF
  // message-header  = field-name ":" [ field-value ]
  int writeList(List<int> buffer, int offset, int count) {
    int index = offset;
    int lastIndex = offset + count;
    while ((index < lastIndex) && !_failure) {
      int byte = buffer[index];
      switch (_state) {
        case State.START:
          _contentLength = 0;
          _keepAlive = false;
          _chunked = false;

          if (byte == Const.HTTP11[0]) {
            // Start parsing http method.
            _httpVersionIndex = 1;
            _state = State.METHOD_OR_HTTP_VERSION;
          } else {
            // Start parsing method.
            _method_or_status_code.addCharCode(byte);
            _state = State.REQUEST_LINE_METHOD;
          }
          break;

        case State.METHOD_OR_HTTP_VERSION:
          if (_httpVersionIndex < Const.HTTP11.length &&
              byte == Const.HTTP11[_httpVersionIndex]) {
            // Continue parsing HTTP version.
            _httpVersionIndex++;
          } else if (_httpVersionIndex == Const.HTTP11.length &&
                     byte == CharCode.SP) {
            // HTTP version parsed.
            _state = State.RESPONSE_LINE_STATUS_CODE;
          } else {
            // Did not parse HTTP version. Expect method instead.
            for (int i = 0; i < _httpVersionIndex; i++) {
              _method_or_status_code.addCharCode(Const.HTTP11[i]);
            }
            _state = State.REQUEST_LINE_URI;
          }
          break;

        case State.REQUEST_LINE_METHOD:
          if (byte == CharCode.SP) {
            _state = State.REQUEST_LINE_URI;
          } else {
            _method_or_status_code.addCharCode(byte);
          }
          break;

        case State.REQUEST_LINE_URI:
          if (byte == CharCode.SP) {
            _state = State.REQUEST_LINE_HTTP_VERSION;
            _httpVersionIndex = 0;
          } else {
            _uri_or_reason_phrase.addCharCode(byte);
          }
          break;

        case State.REQUEST_LINE_HTTP_VERSION:
          if (_httpVersionIndex < Const.HTTP11.length) {
            _expect(byte, Const.HTTP11[_httpVersionIndex]);
            _httpVersionIndex++;
          } else {
            _expect(byte, CharCode.CR);
            _state = State.REQUEST_LINE_ENDING;
          }
          break;

        case State.REQUEST_LINE_ENDING:
          _expect(byte, CharCode.LF);
          if (requestStart != null) {
            requestStart(_method_or_status_code.toString(),
                         _uri_or_reason_phrase.toString());
          }
          _method_or_status_code.clear();
          _uri_or_reason_phrase.clear();
          _state = State.HEADER_START;
          break;

        case State.RESPONSE_LINE_STATUS_CODE:
          if (byte == CharCode.SP) {
            _state = State.RESPONSE_LINE_REASON_PHRASE;
          } else {
            if (byte < 0x30 && 0x39 < byte) {
              _failure = true;
            } else {
              _method_or_status_code.addCharCode(byte);
            }
          }
          break;

        case State.RESPONSE_LINE_REASON_PHRASE:
          if (byte == CharCode.CR) {
            _state = State.RESPONSE_LINE_ENDING;
          } else {
            _uri_or_reason_phrase.addCharCode(byte);
          }
          break;

        case State.RESPONSE_LINE_ENDING:
          _expect(byte, CharCode.LF);
          // TODO(sgjesse): Check for valid status code.
          if (responseStart != null) {
            responseStart(Math.parseInt(_method_or_status_code.toString()),
                          _uri_or_reason_phrase.toString());
          }
          _method_or_status_code.clear();
          _uri_or_reason_phrase.clear();
          _state = State.HEADER_START;
          break;

        case State.HEADER_START:
          if (byte == CharCode.CR) {
            _state = State.HEADER_ENDING;
          } else {
            // Start of new header field.
            _headerField.addCharCode(_toLowerCase(byte));
            _state = State.HEADER_FIELD;
          }
          break;

        case State.HEADER_FIELD:
          if (byte == CharCode.COLON) {
            _state = State.HEADER_VALUE_START;
          } else {
            _headerField.addCharCode(_toLowerCase(byte));
          }
          break;

        case State.HEADER_VALUE_START:
          if (byte != CharCode.SP && byte != CharCode.HT) {
            // Start of new header value.
            _headerValue.addCharCode(byte);
            _state = State.HEADER_VALUE;
          }
          break;

        case State.HEADER_VALUE:
          if (byte == CharCode.CR) {
            _state = State.HEADER_VALUE_FOLDING_OR_ENDING;
          } else {
            _headerValue.addCharCode(byte);
          }
          break;

        case State.HEADER_VALUE_FOLDING_OR_ENDING:
          _expect(byte, CharCode.LF);
          _state = State.HEADER_VALUE_FOLD_OR_END;
          break;

        case State.HEADER_VALUE_FOLD_OR_END:
          if (byte == CharCode.SP || byte == CharCode.HT) {
            _state = State.HEADER_VALUE_START;
          } else {
            String headerField = _headerField.toString();
            String headerValue =_headerValue.toString();
            // Ignore the Content-Length header if Transfer-Encoding
            // is chunked (RFC 2616 section 4.4)
            if (headerField == "content-length" && !_chunked) {
              _contentLength = Math.parseInt(headerValue);
            } else if (headerField == "connection" &&
                       headerValue == "keep-alive") {
              _keepAlive = true;
            } else if (headerField == "transfer-encoding" &&
                       headerValue == "chunked") {
              _chunked = true;
              _contentLength = -1;
            }
            if (headerReceived != null) {
              headerReceived(headerField, headerValue);
            }
            _headerField.clear();
            _headerValue.clear();

            if (byte == CharCode.CR) {
              _state = State.HEADER_ENDING;
            } else {
              // Start of new header field.
              _headerField.addCharCode(_toLowerCase(byte));
              _state = State.HEADER_FIELD;
            }
          }
          break;

        case State.HEADER_ENDING:
          _expect(byte, CharCode.LF);
          if (headersComplete != null) headersComplete();

          // If there is no data get ready to process the next request.
          if (_chunked) {
            _state = State.CHUNK_SIZE;
            _remainingContent = 0;
          } else if (_contentLength == 0) {
            if (dataEnd != null) dataEnd();
            _state = State.START;
          } else if (_contentLength > 0) {
            _remainingContent = _contentLength;
            _state = State.BODY;
          } else {
            // TODO(sgjesse): Error handling.
          }
          break;

        case State.CHUNK_SIZE_STARTING_CR:
          _expect(byte, CharCode.CR);
          _state = State.CHUNK_SIZE_STARTING_LF;
          break;

        case State.CHUNK_SIZE_STARTING_LF:
          _expect(byte, CharCode.LF);
          _state = State.CHUNK_SIZE;
          break;

        case State.CHUNK_SIZE:
          if (byte == CharCode.CR) {
            _state = State.CHUNK_SIZE_ENDING;
          } else {
            int value = _expectHexDigit(byte);
            _remainingContent = _remainingContent * 16 + value;
          }
          break;

        case State.CHUNK_SIZE_ENDING:
          _expect(byte, CharCode.LF);
          if (_remainingContent > 0) {
            _state = State.BODY;
          } else {
            if (dataEnd != null) dataEnd();
            _state = State.CHUNKED_BODY_DONE_CR;
          }
          break;

        case State.CHUNKED_BODY_DONE_CR:
          _expect(byte, CharCode.CR);
          _state = State.CHUNKED_BODY_DONE_LF;
          break;

        case State.CHUNKED_BODY_DONE_LF:
          _expect(byte, CharCode.LF);
          _state = State.START;
          break;

        case State.BODY:
          // The body is not handled one byte at the time but in blocks.
          int dataAvailable = lastIndex - index;
          List<int> data;
          if (dataAvailable <= _remainingContent) {
            data = new List<int>(dataAvailable);
            data.copyFrom(buffer, index, 0, dataAvailable);
          } else {
            data = new List<int>(_remainingContent);
            data.copyFrom(buffer, index, 0, _remainingContent);
          }

          if (dataReceived != null) dataReceived(data);
          _remainingContent -= data.length;
          index += data.length;
          if (_remainingContent == 0) {
            if (!_chunked) {
              if (dataEnd != null) dataEnd();
              _state = State.START;
            } else {
              _state = State.CHUNK_SIZE_STARTING_CR;
            }
          }

          // Hack - as we always do index++ below.
          index--;
          break;

        default:
          // Should be unreachable.
          assert(false);
      }

      // Move to the next byte.
      index++;
    }

    // Return the number of bytes parsed.
    return index - offset;
  }

  int get contentLength() => _contentLength;
  bool get keepAlive() => _keepAlive;

  int _toLowerCase(int byte) {
    final int aCode = "A".charCodeAt(0);
    final int zCode = "Z".charCodeAt(0);
    final int delta = "a".charCodeAt(0) - aCode;
    return (aCode <= byte && byte <= zCode) ? byte + delta : byte;
  }

  int _expect(int val1, int val2) {
    if (val1 != val2) {
      _failure = true;
    }
  }

  int _expectHexDigit(int byte) {
    if (0x30 <= byte && byte <= 0x39) {
      return byte - 0x30;  // 0 - 9
    } else if (0x41 <= byte && byte <= 0x46) {
      return byte - 0x41 + 10;  // A - F
    } else if (0x61 <= byte && byte <= 0x66) {
      return byte - 0x61 + 10;  // a - f
    } else {
      _failure = true;
      return 0;
    }
  }

  int _state;
  bool _failure;
  int _httpVersionIndex;
  StringBuffer _method_or_status_code;
  StringBuffer _uri_or_reason_phrase;
  StringBuffer _headerField;
  StringBuffer _headerValue;

  int _contentLength;
  bool _keepAlive;
  bool _chunked;

  int _remainingContent;

  // Callbacks.
  var requestStart;
  var responseStart;
  var headerReceived;
  var headersComplete;
  var dataReceived;
  var dataEnd;
}


// Utility class which can deliver bytes one by one from a number of
// buffers added.
class BufferList {
  BufferList() : _index = 0, _length = 0, _buffers = new Queue();

  void add(List<int> buffer) {
    _buffers.addLast(buffer);
    _length += buffer.length;
  }

  int next() {
    int value = _buffers.first()[_index++];
    _length--;
    if (_index == _buffers.first().length) {
      _buffers.removeFirst();
      _index = 0;
    }
    return value;
  }

  int get length() => _length;

  int _length;
  Queue<List<int>> _buffers;
  int _index;
}


// Utility class for decoding UTF-8 from data delivered as a stream of
// bytes.
class UTF8Decoder {
  UTF8Decoder()
      : _bufferList = new BufferList(),
        _result = new StringBuffer();

  // Add UTF-8 encoded data.
  int writeList(List<int> buffer) {
    _bufferList.add(buffer);
    // Only process as much data as we know is safe.
    while (_bufferList.length >= 4) {
      _processNext();
    }
  }

  // Return the decoded string.
  String toString() {
    // Process any leftover data.
    while (_bufferList.length > 0) {
      _processNext();
    }
    return _result.toString();
  }

  // Process the next UTF-8 encoded character.
  void _processNext() {
    int value = _bufferList.next() & 0xFF;
    if ((value & 0x80) == 0x80) {
      int additionalBytes;
      if ((value & 0xe0) == 0xc0) {  // 110xxxxx
        value = value & 0x1F;
        additionalBytes = 1;
      } else if ((value & 0xf0) == 0xe0) {  // 1110xxxx
        value = value & 0x0F;
        additionalBytes = 2;
      } else {  // 11110xxx
        value = value & 0x07;
        additionalBytes = 3;
      }
      for (int i = 0; i < additionalBytes; i++) {
        int byte = _bufferList.next();
        value = value << 6 | (byte & 0x3F);
      }
    }
    _result.addCharCode(value);
  }

  BufferList _bufferList;
  StringBuffer _result;
}


// Utility class for encoding a string into UTF-8 byte stream.
class UTF8Encoder {
  static List<int> encodeString(String string) {
    int size = _encodingSize(string);
    List result = new List<int>(size);
    _encodeString(string, result);
    return result;
  }

  static int _encodingSize(String string) => _encodeString(string, null);

  static int _encodeString(String string, List<int> buffer) {
    int pos = 0;
    int length = string.length;
    for (int i = 0; i < length; i++) {
      int additionalBytes;
      int charCode = string.charCodeAt(i);
      if (charCode <= 0x007F) {
        additionalBytes = 0;
        if (buffer != null) buffer[pos] = charCode;
      } else if (charCode <= 0x07FF) {
        // 110xxxxx (xxxxx is top 5 bits).
        if (buffer != null) buffer[pos] = ((charCode >> 6) & 0x1F) | 0xC0;
        additionalBytes = 1;
      } else if (charCode <= 0xFFFF) {
        // 1110xxxx (xxxx is top 4 bits)
        if (buffer != null) buffer[pos] = ((charCode >> 12) & 0x0F)| 0xE0;
        additionalBytes = 2;
      } else {
        // 11110xxx (xxx is top 3 bits)
        if (buffer != null) buffer[pos] = ((charCode >> 18) & 0x07) | 0xF0;
        additionalBytes = 3;
      }
      pos++;
      if (buffer != null) {
        for (int i = additionalBytes; i > 0; i--) {
          // 10xxxxxx (xxxxxx is next 6 bits from the top).
          buffer[pos++] = ((charCode >> (6 * (i - 1))) & 0x3F) | 0x80;
        }
      } else {
        pos += additionalBytes;
      }
    }
    return pos;
  }
}


class HTTPRequestOrResponse {
  HTTPRequestOrResponse(HTTPConnectionBase this._httpConnection)
      : _contentLength = -1,
        _keepAlive = false,
        _headers = new Map();

  int get contentLength() => _contentLength;
  bool get keepAlive() => _keepAlive;

  void _setHeader(String name, String value) {
    _headers[name] = value;
  }

  void _writeList(List<int> data, int offset, int count) {
    if (count > 0) {
      if (_contentLength < 0) {
        // Write chunk size if transfer encoding is chunked.
        _addSendHexString(count);
        _addSendCRLF();
        _httpConnection._addSendData(data, offset, count, true);
        _addSendCRLF();
      } else {
        _httpConnection._addSendData(data, offset, count, true);
      }
    }
  }

  void _writeString(String string) {
    if (string.length > 0) {
      // Encode as UTF-8 and write data.
      List<int> data = UTF8Encoder.encodeString(string);
      _writeList(data, 0, data.length);
    }
  }

  void _writeDone() {
    if (_contentLength < 0) {
      // Terminate the content if transfer encoding is chunked.
      _httpConnection._addSendData(
          Const.END_CHUNKED, 0, Const.END_CHUNKED.length, true);
    }
  }

  void _addSendHeaders() {
    List<int> data;

    // Format headers.
    _headers.forEach((String name, String value) {
                       data = name.charCodes();
                       _httpConnection._addSendData(data, 0, data.length);
                       data = ": ".charCodes();
                       _httpConnection._addSendData(data, 0, data.length);
                       data = value.charCodes();
                       _httpConnection._addSendData(data, 0, data.length);
                       _addSendCRLF();
                     });
    // Terminate header.
    _addSendCRLF();
  }

  int _addSendHexString(int x) {
    final List<int> hexDigits = [0x30, 0x31, 0x32, 0x33, 0x34,
                                  0x35, 0x36, 0x37, 0x38, 0x39,
                                  0x41, 0x42, 0x43, 0x44, 0x45, 0x46];
    List<int> hex = new List<int>(10);
    int index = hex.length;
    while (x > 0) {
      index--;
      hex[index] = hexDigits[x % 16];
      x = x >> 4;
    }
    return _httpConnection._addSendData(hex, index, hex.length - index);
  }

  void _addSendCRLF() {
    final CRLF = const [CharCode.CR, CharCode.LF];
    _httpConnection._addSendData(CRLF, 0, CRLF.length);
  }

  void _addSendSP() {
    final SP = const [CharCode.SP];
    _httpConnection._addSendData(SP, 0, SP.length);
  }

  void _dataReceivedHandler(List<int> data) {
    // If no data received handler exists collect data as a string.
    if (dataReceived != null) {
      dataReceived(data);
    } else {
      if (_decoder == null) _decoder = new UTF8Decoder();
      _decoder.writeList(data);
    }
  }

  void _dataEndHandler() {
    if (dataEnd != null) {
      // Pass the string collected if any.
      dataEnd(_decoder != null ? _decoder.toString() : null);
    }
  }

  HTTPConnectionBase _httpConnection;
  Map<String, String> _headers;

  // Length of the content body. If this is set to -1 (default value)
  // when starting to send data chunked transfer encoding will be
  // used.
  int _contentLength;
  bool _keepAlive;

  UTF8Decoder _decoder;

  // Callbacks.
  var dataReceived;
  var dataEnd;
}


// Parsed HTTP request providing information on the HTTP headers.
class HTTPRequestImplementation
    extends HTTPRequestOrResponse
    implements HTTPRequest {
  HTTPRequestImplementation(HTTPConnection connection) : super(connection);

  String get method() => _method;
  String get uri() => _uri;
  String get path() => _path;
  Map get headers() => _headers;
  String get queryString() => _queryString;
  Map get queryParameters() => _queryParameters;

  void _requestStartHandler(String method, String uri) {
    _method = method;
    _uri = uri;
    _parseRequestUri(uri);
  }

  void _headerReceivedHandler(String name, String value) {
    _setHeader(name, value);
  }

  void _headersCompleteHandler() {
    // Nothing to do.
  }

  void _parseRequestUri(String uri) {
    int position;
    position = uri.indexOf("?", 0);
    _queryParameters = new Map();
    if (position == -1) {
      _path = _uri;
      _queryString = null;
    } else {
      _path = _uri.substring(0, position);
      _queryString = _uri.substringToEnd(position + 1);

      // Simple parsing of the query string into request parameters.
      // TODO(sgjesse): Handle all detail e.g. encoding.
      int currentPosition = 0;
      while (currentPosition < _queryString.length) {
        position = _queryString.indexOf("=", currentPosition);
        if (position == -1) {
          break;
        }
        String name = _queryString.substring(currentPosition, position);
        currentPosition = position + 1;
        position = _queryString.indexOf("&", currentPosition);
        String value;
        if (position == -1) {
          value = _queryString.substringToEnd(currentPosition);
          currentPosition = _queryString.length;
        } else {
          value = _queryString.substring(currentPosition, position);
          currentPosition = position + 1;
        }
        _queryParameters[name] = value;
      }
    }
  }

  String _method;
  String _uri;
  String _path;
  String _queryString;
  Map<String, String> _queryParameters;
}


// HTTP response object for sending a HTTP response.
class HTTPResponseImplementation
    extends HTTPRequestOrResponse
    implements HTTPResponse {
  static final int START = 0;
  static final int HEADERS_SENT = 1;
  static final int DONE = 2;

  HTTPResponseImplementation(HTTPConnection httpConnection)
      : super(httpConnection),
        statusCode = HTTPStatus.OK,
        _state = START;

  void set contentLength(int contentLength) => _contentLength = contentLength;
  void set keepAlive(bool keepAlive) => _keepAlive = keepAlive;

  // Set a header on the response. NOTE: If the same header is set
  // more than once only the last one will be part of the response.
  void setHeader(String name, String value) {
    _setHeader(name, value);
  }

  // Write response data. When this is called for the first time the
  // response header is send as well. Writing data might buffer the
  // data and send it in larger chunks than delivered to this
  // function. The optional callback can be used to stream data as
  // it will be called when more data should be written.
  bool writeList(List<int> data,
                  int offset,
                  int count,
                  [var callback = null]) {
    // Ensure that headers are written.
    if (_state == START) {
      _addSendHeader();
    }
    _writeList(data, offset, count);

    // Start sending data now if a callback is registered.
    if (callback != null) {
      return _httpConnection._startSending(callback);
    } else {
      return true;
    }
  }

  // Write string data to the response. The string characters will be
  // encoded using UFT-8. When this is called for the first time the
  // response header is send as well.
  bool writeString(String string, [var callback = null]) {
    // Ensure that headers are written.
    if (_state == START) {
      _addSendHeader();
    }
    _writeString(string);

    // Start sending data now if a callback is registered.
    if (callback != null) {
      return _httpConnection._startSending(callback);
    } else {
      return true;
    }
  }

  // Indicate that all the response data has been written.
  void writeDone() {
    // Ensure that headers are written.
    if (_state == START) {
      _addSendHeader();
    }
    _writeDone();
    _state = DONE;

    // No more data - now send it all.
    _httpConnection._startSending(null);
  }

  String _findReasonPhrase(int statusCode) {
    if (reasonPhrase != null) {
      return reasonPhrase;
    }

    switch (statusCode) {
      case HTTPStatus.CONTINUE: return "Continue";
      case HTTPStatus.SWITCHING_PROTOCOLS: return "Switching Protocols";
      case HTTPStatus.OK: return "OK";
      case HTTPStatus.CREATED: return "Created";
      case HTTPStatus.ACCEPTED: return "Accepted";
      case HTTPStatus.NON_AUTHORITATIVE_INFORMATION:
        return "Non-Authoritative Information";
      case HTTPStatus.NO_CONTENT: return "No Content";
      case HTTPStatus.RESET_CONTENT: return "Reset Content";
      case HTTPStatus.PARTIAL_CONTENT: return "Partial Content";
      case HTTPStatus.MULTIPLE_CHOICES: return "Multiple Choices";
      case HTTPStatus.MOVED_PERMANENTLY: return "Moved Permanently";
      case HTTPStatus.FOUND: return "Found";
      case HTTPStatus.SEE_OTHER: return "See Other";
      case HTTPStatus.NOT_MODIFIED: return "Not Modified";
      case HTTPStatus.USE_PROXY: return "Use Proxy";
      case HTTPStatus.TEMPORARY_REDIRECT: return "Temporary Redirect";
      case HTTPStatus.BAD_REQUEST: return "Bad Request";
      case HTTPStatus.UNAUTHORIZED: return "Unauthorized";
      case HTTPStatus.PAYMENT_REQUIRED: return "Payment Required";
      case HTTPStatus.FORBIDDEN: return "Forbidden";
      case HTTPStatus.NOT_FOUND: return "Not Found";
      case HTTPStatus.METHOD_NOT_ALLOWED: return "Method Not Allowed";
      case HTTPStatus.NOT_ACCEPTABLE: return "Not Acceptable";
      case HTTPStatus.PROXY_AUTHENTICATION_REQUIRED:
        return "Proxy Authentication Required";
      case HTTPStatus.REQUEST_TIMEOUT: return "Request Time-out";
      case HTTPStatus.CONFLICT: return "Conflict";
      case HTTPStatus.GONE: return "Gone";
      case HTTPStatus.LENGTH_REQUIRED: return "Length Required";
      case HTTPStatus.PRECONDITION_FAILED: return "Precondition Failed";
      case HTTPStatus.REQUEST_ENTITY_TOO_LARGE:
        return "Request Entity Too Large";
      case HTTPStatus.REQUEST_URI_TOO_LONG: return "Request-URI Too Large";
      case HTTPStatus.UNSUPPORTED_MEDIA_TYPE: return "Unsupported Media Type";
      case HTTPStatus.REQUESTED_RANGE_NOT_SATISFIABLE:
        return "Requested range not satisfiable";
      case HTTPStatus.EXPECTATION_FAILED: return "Expectation Failed";
      case HTTPStatus.INTERNAL_SERVER_ERROR: return "Internal Server Error";
      case HTTPStatus.NOT_IMPLEMENTED: return "Not Implemented";
      case HTTPStatus.BAD_GATEWAY: return "Bad Gateway";
      case HTTPStatus.SERVICE_UNAVAILABLE: return "Service Unavailable";
      case HTTPStatus.GATEWAY_TIMEOUT: return "Gateway Time-out";
      case HTTPStatus.HTTP_VERSION_NOT_SUPPORTED:
        return "HTTP Version not supported";
      default: return "Status " + statusCode.toString();
    }
  }

  void _addSendHeader() {
    List<int> data;

    // Write status line.
    _httpConnection._addSendData(Const.HTTP11, 0, Const.HTTP11.length);
    _addSendSP();
    data = statusCode.toString().charCodes();
    _httpConnection._addSendData(data, 0, data.length);
    _addSendSP();
    data = _findReasonPhrase(statusCode).charCodes();
    _httpConnection._addSendData(data, 0, data.length);
    _addSendCRLF();

    // Determine the value of the "Connection" header
    // based on the keep alive state.
    setHeader("Connection", keepAlive ? "keep-alive" : "close");
    // Determine the value of the "Transfer-Encoding" header based on
    // whether the content length is known.
    if (_contentLength >= 0) {
      setHeader("Content-Length", _contentLength.toString());
    } else {
      setHeader("Transfer-Encoding", "chunked");
    }

    // Write headers.
    _addSendHeaders();
    _state = HEADERS_SENT;
  }

  // Response status code.
  int statusCode;
  String reasonPhrase;

  int _state;
}


class SendBuffer {
  SendBuffer(this._buffer, this._offset, this._count);
  SendBuffer.empty([int size = DEFAULT_SEND_BUFFER_SIZE]) {
    _buffer = new List<int>(size);
    _offset = 0;
    _count = 0;
  }

  // Add data to this send buffer. Returns the number of bytes
  // actually added to the buffer.
  int _addData(List<int> data, int offset, int count) {
    int remaining = _buffer.length - _offset - _count;
    int copyCount = Math.min(remaining, count);
    _buffer.copyFrom(data, offset, _count, copyCount);
    _count += copyCount;
    return copyCount;
  }

  int _write(Socket socket) {
    int written = socket.writeList(_buffer, _offset, _count);
    _offset += written;
    _count -= written;
  }

  bool get _isFull() => _count == _buffer.length - _offset;
  bool get _isSent() => _count == 0;

  int _offset;
  int _count;
  List<int> _buffer;

  static final DEFAULT_SEND_BUFFER_SIZE = 1024;
}


class HTTPConnectionBase {
  static final BUFFER_SIZE = 80;

  HTTPConnectionBase(Socket this._socket)
      : _sendBuffers = new Queue(),
        _httpParser = new HTTPParser() {
    // Register handler for socket events.
    _socket.setDataHandler(() => _dataHandler());
    _socket.setCloseHandler(() => _closeHandler());
    _socket.setErrorHandler(() => _errorHandler());
  }

  // Add data to be send. If the flag keepBuffer is false (the
  // default) the data will be copied to an internal send buffer. If
  // the flag keepBuffer is true the data will be kept until it has
  // successfuly been sent. Note: calling this function will not
  // initiate sending of the data.
  void _addSendData(List<int> data,
                    int offset,
                    int count,
                    [bool keepBuffer = false]) {
    if (keepBuffer) {
      // If there is a current send buffer add that in front of the
      // buffer to keep.
      if (_currentSendBuffer != null) {
        _sendBuffers.addLast(_currentSendBuffer);
        _currentSendBuffer = null;
      }
      _sendBuffers.addLast(new SendBuffer(data, offset, count));
    } else {
      while (count > 0) {
        if (_currentSendBuffer == null) {
          _currentSendBuffer = new SendBuffer.empty();
        }
        int copied = _currentSendBuffer._addData(data, offset, count);
        offset += copied;
        count -= copied;
        if (_currentSendBuffer._isFull) {
          _sendBuffers.addLast(_currentSendBuffer);
          _currentSendBuffer = null;
        }
      }
    }
  }

  // Start sending the data bufferd for this connection. This will
  // eventually send all data buffered, as callbacks for when write is
  // possible will be set up if all data cannot be transmitted
  // immediately. If the return value is true all data was send
  // immediately and no callbacks will occour.
  bool _startSending(var callback) {
    void send() {

      void continueSending() {
        // Send more data.
        send();

        // If all data has been sent notify the callback.
        if (_sendBuffers.isEmpty() && callback != null) {
          _socket.setWriteHandler(null);
          callback();
        }
      }

      // Send as much data as possible.
      while (!_sendBuffers.isEmpty()) {
        SendBuffer buffer = _sendBuffers.first();
        buffer._write(_socket);
        if (!buffer._isSent) {
          _socket.setWriteHandler(continueSending);
          break;
        } else {
          _sendBuffers.removeFirst();
        }
      }
    }

    // Queue the current send buffer now if any.
    if (_currentSendBuffer != null) {
      _sendBuffers.addLast(_currentSendBuffer);
      _currentSendBuffer = null;
    }

    // Send as much as possible.
    send();

    // Return whether all data was send immediately.
    return _sendBuffers.isEmpty();
  }

  void _dataHandler() {
    int available = _socket.available();
    if (available == 0) {
      return;
    }

    List<int> buffer = new List<int>(BUFFER_SIZE);
    int bytesRead = _socket.readList(buffer, 0, BUFFER_SIZE);
    if (bytesRead > 0) {
      int parsed = _httpParser.writeList(buffer, 0, bytesRead);
      if (parsed != bytesRead) {
        print("Failed to parse HTTP data $parsed $bytesRead");
        _socket.close();
      }
    }
  }

  void _closeHandler() {
    // TODO(sgjesse): Remove this from active connections.
    _socket.close();
  }

  void _errorHandler() {
    print("ERROR!");
    // TODO(sgjesse): Remove this from active connections.
  }

  Socket _socket;
  HTTPParser _httpParser;

  SendBuffer _currentSendBuffer;
  Queue _sendBuffers;
}


// HTTP server connection over a socket.
class HTTPConnection extends HTTPConnectionBase {
  HTTPConnection(Socket socket) : super(socket) {
    // Register HTTP parser callbacks.
    _httpParser.requestStart =
        (method, uri) => _requestStartHandler(method, uri);
    _httpParser.responseStart =
        (statusCode, reasonPhrase) =>
            _responseStartHandler(statusCode, reasonPhrase);
    _httpParser.headerReceived =
        (name, value) => _headerReceivedHandler(name, value);
    _httpParser.headersComplete = () => _headersCompleteHandler();
    _httpParser.dataReceived = (data) => _dataReceivedHandler(data);
    _httpParser.dataEnd = () => _dataEndHandler();
  }

  void _requestStartHandler(String method, String uri) {
    // Create new request and response objects for this request.
    _request = new HTTPRequestImplementation(this);
    _response = new HTTPResponseImplementation(this);
    _request._requestStartHandler(method, uri);
  }

  void _responseStartHandler(int statusCode, String reasonPhrase) {
    // TODO(sgjesse): Error handling.
  }

  void _headerReceivedHandler(String name, String value) {
    _request._headerReceivedHandler(name, value);
  }

  void _headersCompleteHandler() {
    _request._headersCompleteHandler();
    _response.keepAlive = _httpParser.keepAlive;
    if (requestReceived != null) {
      requestReceived(_request, _response);
    }
  }

  void _dataReceivedHandler(List<int> data) {
    _request._dataReceivedHandler(data);
  }

  void _dataEndHandler() {
    _request._dataEndHandler();
  }

  HTTPRequest _request;
  HTTPResponse _response;

  // Callbacks.
  var requestReceived;
}


// HTTP server waiting for socket connections. The connections are
// managed by the server and as requests are received the request.
class HTTPServerImplementation implements HTTPServer {

  static final BUFFER_SIZE = 80;

  HTTPServerImplementation () : _debugTrace = false;

  void listen(String host, int port, var callback) {

    void connectionHandler() {
      // Accept the client connection.
      Socket socket = _server.accept();
      HTTPConnection connection = new HTTPConnection(socket);
      connection.requestReceived = callback;
      _connections.addLast(connection);
      _connectionsCount++;
      if (_debugTrace) {
        print("New connection (total $_connectionsCount connections)");
      }
    }

    _connections = new Queue<HTTPConnection>();
    _connectionsCount = 0;
    _server = new ServerSocket(host, port, 5);
    _server.setConnectionHandler(connectionHandler);
  }

  void close() => _server.close();
  int get port() => _server.port;

  ServerSocket _server;  // The server listen socket.
  Queue<HTTPConnection> _connections;  // Queue of currently connected clients.
  int _connectionsCount;
  bool _debugTrace;
}


class HTTPClientRequestImplementation
    extends HTTPRequestOrResponse
    implements HTTPClientRequest {
  static final int START = 0;
  static final int HEADERS_SENT = 1;
  static final int DONE = 2;

  HTTPClientRequestImplementation(String this._method,
                                  String this._uri,
                                  HTTPClientConnection connection)
      : super(connection),
        _state = START {
    _connection = connection;
    // Default GET requests to have no content.
    if (_method == "GET") {
      _contentLength = 0;
    }
  }

  void set contentLength(int contentLength) => _contentLength = contentLength;
  void set keepAlive(bool keepAlive) => _keepAlive = keepAlive;
  int get statusCode() { return _statusCode; }
  String get reasonPhrase() { return _reasonPhrase; }

  void setHeader(String name, String value) {
    _setHeader(name, value);
  }

  // Write request data. When this is called for the first time the
  // request header is send as well. Writing data might buffer the
  // data and send it in larger chunks than delivered to this
  // function. The optional callback can be used to stream data as
  // it will be called when more data should be written.
  bool writeList(List<int> data,
                 int offset,
                 int count,
                 [var callback = null]) {
    // Ensure that headers are written.
    if (_state == START) {
      _addSendHeader();
    }
    _writeList(data, offset, count);

    // Start sending data now if a callback is registered.
    if (callback != null) {
      return _httpConnection._startSending(callback);
    } else {
      return true;
    }
  }

  // Write string data to the request. The string characters will be
  // encoded using UFT-8. When this is called for the first time the
  // request header is send as well.
  bool writeString(String string, [var callback = null]) {
    // Ensure that headers are written.
    if (_state == START) {
      _addSendHeader();
    }
    _writeString(string);

    // Start sending data now if a callback is registered.
    if (callback != null) {
      return _httpConnection._startSending(callback);
    } else {
      return true;
    }
  }

  // Indicate that all the request data has been written.
  void writeDone() {
    // Ensure that headers are written.
    if (_state == START) {
      _addSendHeader();
    }
    _writeDone();
    _state = DONE;

    // No more data - now send it all.
    _httpConnection._startSending(null);
  }

  void set responseReceived(void callback(HTTPClientResponse response)) {
    _connection._response._responseReceived = callback;
  }

  void _addSendHeader() {
    List<int> data;

    // Write request line.
    data = _method.toString().charCodes();
    _httpConnection._addSendData(data, 0, data.length);
    _addSendSP();
    data = _uri.toString().charCodes();
    _httpConnection._addSendData(data, 0, data.length);
    _addSendSP();
    _httpConnection._addSendData(Const.HTTP11, 0, Const.HTTP11.length);
    _addSendCRLF();

    // Determine the value of the "Connection" header
    // based on the keep alive state.
    setHeader("Connection", keepAlive ? "keep-alive" : "close");
    // Determine the value of the "Transfer-Encoding" header based on
    // whether the content length is known.
    if (_contentLength >= 0) {
      setHeader("Content-Length", _contentLength.toString());
    } else {
      setHeader("Transfer-Encoding", "chunked");
    }

    // Write headers.
    _addSendHeaders();
    _state = HEADERS_SENT;
  }

  String _method;
  String _uri;
  HTTPClientConnection _connection;
  int _state;
}


class HTTPClientResponseImplementation
    extends HTTPRequestOrResponse
    implements HTTPClientResponse {
  HTTPClientResponseImplementation(HTTPClientConnection connection)
      : super(connection) {
    _connection = connection;
  }

  int get statusCode() { return _statusCode; }
  int get reasonPhrase() { return _reasonPhrase; }
  Map get headers() => _headers;

  void _requestStartHandler(String method, String uri) {
    // TODO(sgjesse): Error handling
  }

  void _responseStartHandler(int statusCode, String reasonPhrase) {
    _statusCode = statusCode;
    _reasonPhrase = reasonPhrase;
  }

  void _headerReceivedHandler(String name, String value) {
    _setHeader(name, value);
  }

  void _headersCompleteHandler() {
    if (_responseReceived != null) {
      _responseReceived(this);
    }
  }

  int _statusCode;
  String _reasonPhrase;

  HTTPClientConnection _connection;
  var _responseReceived;
}


class HTTPClientConnection extends HTTPConnectionBase {
  HTTPClientConnection(HTTPClientImplementation this._client,
                       SocketConnection socketConn)
      : super(socketConn._socket) {
    _socketConn = socketConn;
    // Register HTTP parser callbacks.
    _httpParser.requestStart =
        (method, uri) => _requestStartHandler(method, uri);
    _httpParser.responseStart =
        (statusCode, reasonPhrase) =>
            _responseStartHandler(statusCode, reasonPhrase);
    _httpParser.headerReceived =
        (name, value) => _headerReceivedHandler(name, value);
    _httpParser.headersComplete = () => _headersCompleteHandler();
    _httpParser.dataReceived = (data) => _dataReceivedHandler(data);
    _httpParser.dataEnd = () => _dataEndHandler();
  }

  HTTPClientRequest open(String method, String uri) {
    _request = new HTTPClientRequestImplementation(method, uri, this);
    _request.keepAlive = true;
    _response = new HTTPClientResponseImplementation(this);
    return _request;
  }

  void _requestStartHandler(String method, String uri) {
    // TODO(sgjesse): Error handling.
  }

  void _responseStartHandler(int statusCode, String reasonPhrase) {
    _response._responseStartHandler(statusCode, reasonPhrase);
  }

  void _headerReceivedHandler(String name, String value) {
    _response._headerReceivedHandler(name, value);
  }

  void _headersCompleteHandler() {
    _response._headersCompleteHandler();
  }

  void _dataReceivedHandler(List<int> data) {
    _response._dataReceivedHandler(data);
  }

  void _dataEndHandler() {
    _response._dataEndHandler();
    if (_response.headers["connection"] == "close") {
      _socket.close();
    } else {
      _client._returnSocketConnection(_socketConn);
    }
  }

  HTTPClientImplementation _client;
  SocketConnection _socketConn;
  HTTPClientRequest _request;
  HTTPClientResponse _response;

  // Callbacks.
  var requestReceived;

}


// Class for holding keep-alive sockets in the cache for the HTTP
// client together with the connection information.
class SocketConnection {
  SocketConnection(String this._host,
                   int this._port,
                   Socket this._socket);

  void _markReturned() => _returnTime = new Date.now();
  Duration _idleTime(Date now) => now.difference(_returnTime);

  String _host;
  int _port;
  Socket _socket;
  Date _returnTime;
}


class HTTPClientImplementation implements HTTPClient{
  static final int DEFAULT_EVICTION_TIMEOUT = 60000;

  HTTPClientImplementation() : _openSockets = new Map(), _shutdown = false;

  HTTPClientRequest open(String method,
                         String host,
                         int port,
                         String path) {
    // TODO(sgjesse): Throw exception.
    if (_shutdown) return null;
    SocketConnection socketConn = _getSocketConnection(host, port);
    HTTPClientConnection connection =
        new HTTPClientConnection(this, socketConn);
    HTTPClientRequest request = connection.open(method, path);
    return request;
  }

  void shutdown() {
     _openSockets.forEach(
         void _(String key, Queue<SocketConnection> connections) {
           while (!connections.isEmpty()) {
             var socketConn = connections.removeFirst();
             socketConn._socket.close();
           }
         });
     if (_evictionTimer != null) {
       _evictionTimer.cancel();
     }
     _shutdown = true;
  }

  String _connectionKey(String host, int port) {
    return "$host:$port";
  }

  SocketConnection _getSocketConnection(String host, int port) {
    SocketConnection entry;

    // If there are active connections for this key get the first one
    // otherwise create a new one.
    Queue socketConnections = _openSockets[_connectionKey(host, port)];
    if (socketConnections == null || socketConnections.isEmpty()) {
      Socket socket = new Socket(host, port);
      entry = new SocketConnection(host, port, socket);
    } else {
      entry = socketConnections.removeFirst();

      // Get rid of eviction timer if there are no more active connections.
      if (socketConnections.isEmpty()) {
        _evictionTimer.cancel();
        _evictionTimer = null;
      }
    }

    return entry;
  }

  void _returnSocketConnection(SocketConnection socketConn) {
    // If the HTTP client is beeing shutdown don't return the connection.
    if (_shutdown) {
      socketConn._socket.close();
      return;
    };

    String key = _connectionKey(socketConn._host, socketConn._port);

    // Get or create the connection list for this key.
    Queue sockets = _openSockets[key];
    if (sockets == null) {
      sockets = new Queue();
      _openSockets[key] = sockets;
    }

    // If there is currently no eviction timer start one.
    if (_evictionTimer == null) {
      void _handleEviction(Timer timer) {
        Date now = new Date.now();
        _openSockets.forEach(
            void _(String key, Queue<SocketConnection> connections) {
              // As returned connections are added at the head of the
              // list remove from the tail.
              while (!connections.isEmpty()) {
                SocketConnection socketConn = connections.last();
                if (socketConn._idleTime(now).inMilliseconds >
                    DEFAULT_EVICTION_TIMEOUT) {
                  connections.removeLast();
                } else {
                  break;
                }
              }
            });
      }
      _evictionTimer = new Timer(_handleEviction, 10000, true);
    }

    // Return connection.
    sockets.addFirst(socketConn);
    socketConn._markReturned();
  }

  Map<String, Queue<SocketConnection>> _openSockets;
  Timer _evictionTimer;
  bool _shutdown;  // Has this HTTP client been shutdown?
}
