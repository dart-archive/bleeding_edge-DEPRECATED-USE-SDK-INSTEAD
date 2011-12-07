// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library("http");
#source("http_impl.dart");

/**
 * HTTP status codes.
 */
interface HTTPStatus {
  static final int CONTINUE = 100;
  static final int SWITCHING_PROTOCOLS = 101;
  static final int OK = 200;
  static final int CREATED = 201;
  static final int ACCEPTED = 202;
  static final int NON_AUTHORITATIVE_INFORMATION = 203;
  static final int NO_CONTENT = 204;
  static final int RESET_CONTENT = 205;
  static final int PARTIAL_CONTENT = 206;
  static final int MULTIPLE_CHOICES = 300;
  static final int MOVED_PERMANENTLY = 301;
  static final int FOUND = 302;
  static final int MOVED_TEMPORARILY = 302; // Common alias for FOUND.
  static final int SEE_OTHER = 303;
  static final int NOT_MODIFIED = 304;
  static final int USE_PROXY = 305;
  static final int TEMPORARY_REDIRECT = 307;
  static final int BAD_REQUEST = 400;
  static final int UNAUTHORIZED = 401;
  static final int PAYMENT_REQUIRED = 402;
  static final int FORBIDDEN = 403;
  static final int NOT_FOUND = 404;
  static final int METHOD_NOT_ALLOWED = 405;
  static final int NOT_ACCEPTABLE = 406;
  static final int PROXY_AUTHENTICATION_REQUIRED = 407;
  static final int REQUEST_TIMEOUT = 408;
  static final int CONFLICT = 409;
  static final int GONE = 410;
  static final int LENGTH_REQUIRED = 411;
  static final int PRECONDITION_FAILED = 412;
  static final int REQUEST_ENTITY_TOO_LARGE = 413;
  static final int REQUEST_URI_TOO_LONG = 414;
  static final int UNSUPPORTED_MEDIA_TYPE = 415;
  static final int REQUESTED_RANGE_NOT_SATISFIABLE = 416;
  static final int EXPECTATION_FAILED = 417;
  static final int INTERNAL_SERVER_ERROR = 500;
  static final int NOT_IMPLEMENTED = 501;
  static final int BAD_GATEWAY = 502;
  static final int SERVICE_UNAVAILABLE = 503;
  static final int GATEWAY_TIMEOUT = 504;
  static final int HTTP_VERSION_NOT_SUPPORTED = 505;
}


/**
 * HTTP server.
 */
interface HTTPServer factory HTTPServerImplementation {
  HTTPServer();

  /**
   * Start listening on the specified [host] and [port]. For each HTTP
   * request the specified [callback] will be invoked. If a [port] of
   * 0 is specified the server will choose an ephemeral port. The
   * optional argument [backlog] can be used to specify the listen
   * backlog for the underlying OS listen setup.
   */
  void listen(String host,
              int port,
              void callback(HTTPRequest, HTTPResponse),
              [int backlog]);

  /**
   * Stop server listening.
   */
  void close();

  /**
   * Returns the port that the server is listening on.
   */
  int get port();
}


/**
 * HTTP request delivered to the HTTP server callback.
 */
interface HTTPRequest factory HTTPRequestImplementation {
  /**
   * Returns the content length of the request body. If the size of
   * the request body is not known in advance this -1.
   */
  int get contentLength();

  /**
   * Returns the keep alive state of the connection.
   */
  bool get keepAlive();

  /**
   * Returns the method for the request.
   */
  String get method();

  /**
   * Returns the URI for the request.
   */
  String get uri();

  /**
   * Returns the path part of the URI.
   */
  String get path();

  /**
   * Returns the query string.
   */
  String get queryString();

  /**
   * Returns the parsed query string.
   */
  Map<String, String> get queryParameters();

  /**
   * Returns the request headers.
   */
  Map<String, String> get headers();

  /**
   * Sets callback to be called as request data becomes available. The
   * data delivered are the raw received. If the data is UTF-8 encoded
   * and this callback is not set the callback [dataEnd] will be
   * called with the decoded [String].
   */
  void set dataReceived(void callback(List<int> data));

  /**
   * Sets the callback to be called when all request data have been
   * received. If the callback [dataReceived] is set the [String]
   * argument will be [null] otherwise it will be the result of UTF-8
   * decoding the request body,
   */
  void set dataEnd(void callback(String data));
}


/**
 * HTTP response to be send back to the client.
 */
interface HTTPResponse factory HTTPResponseImplementation {
  /**
   * Gets and sets the content length of the response. If the size of
   * the response is not known in advance set the content length to
   * -1 - which is also the default if not set.
   */
  void set contentLength(int contentLength);
  int get contentLength();

  /**
   * Gets and sets the keep alive state of the connection. If the
   * associated request have a keep alive state of false setting keep
   * alive to true will have no effect.
   */
  void set keepAlive(bool keepAlive);
  bool get keepAlive();

  /**
   * Sets a header on the response. NOTE: If the same header name is
   * set more than once only the last value will be part of the
   * response.
   */
  void setHeader(String name, String value);

  /**
   * Write [count] bytes from index [offset] in the List [data] to
   * the response. When this is called for the first time the response
   * header is send as well. Written data might be buffered and send
   * it in larger chunks than supplied to this function. The return
   * value is true if all data is actually sent right away. If the
   * return value is false the supplied [callback] will be called when
   * the data has been sent. The [callback] will not be called if the
   * return value is true.
   */
  bool writeList(List<int> data, int offset, int count, void callback());

  /**
   * Write string data to the response. The string characters will be
   * encoded using UFT-8. When this is called for the first time the
   * response header is send as well. See [writeList] for information
   * on the return value and the [callback] argument. NOTE: The content
   * length set must be -1 (unknown) to use this method.
   */
  bool writeString(String string, void callback());

  /**
   * Indicate that all the response data has been written.
   */
  void writeDone();
}


/**
 * HTTP client factory.
 */
interface HTTPClient factory HTTPClientImplementation {
  HTTPClient();

  /**
   * Open a HTTP connection. The [openHandler] is called with an
   * HTTPClientRequest when the connection has been successfully
   * opened.
   */
  void open(String method, String host, int port, String path);

  /**
   * Shutdown the HTTP client releasing all resources.
   */
  void shutdown();

  /**
   * Set the open handler that is called on successful open operations.
   */
  void set openHandler(void handler(HTTPClientRequest request));
}


/**
 * HTTP request for a client connection.
 */
interface HTTPClientRequest factory HTTPClientRequestImplementation {
  /**
   * Gets and sets the content length of the request. If the size of
   * the request is not known in advance set content length to -1,
   * which is also the default.
   */
  void set contentLength(int contentLength);
  int get contentLength();

  /**
   * Gets and sets the keep alive state of the connection.
   */
  void set keepAlive(bool keepAlive);
  bool get keepAlive();

  /**
   * Sets a header on the request. NOTE: If the same header name is
   * set more than once only the last value set will be part of the
   * request.
   */
  void setHeader(String name, String value);

  /**
   * Sets callback to be called when the request have been send and
   * the response is ready for processing. The callback is called when
   * all headers of the response are received and data is reqdy to be
   * received.
   */
  void set responseReceived(void callback(HTTPClientResponse response));

  /**
   * Write [count] bytes from index [offset] in the List [data] to
   * the request. When this is called for the first time the request
   * header is send as well. Written data might be buffered and send
   * it in larger chunks than supplied to this function. The return
   * value is true if all data is actually sent right away. If the
   * return value is false the supplied [callback] will be called when
   * the data has been sent. The [callback] will not be called if the
   * return value is true.
   */
  bool writeList(List<int> data, int offset, int count, void callback());

  /**
   * Write string data to the request. The string characters will be
   * encoded using UFT-8. When this is called for the first time the
   * request header is send as well. See [writeList] for information
   * in the return value and the [callback] argumnt. NOTE: The content
   * length set must be -1 (unknown) to use this method.
   */
  bool writeString(String string, void callback());

  /**
   * Indicate that all the request data has been written. After this
   * metod is called no more data can be written. When the response is
   * ready for processing the callback set with [setResponseReceived]
   * is called.
   */
  void writeDone();
}


/**
 * HTTP response for a client connection.
 */
interface HTTPClientResponse factory HTTPClientResponseImplementation {
  /**
   * Returns the status code.
   */
  int get statusCode();

  /**
   * Returns the reason phrase associated with the status code.
   */
  String get reasonPhrase();

  /**
   * Returns the content length of the request body. If the size of
   * the request body is not known in advance this -1.
   */
  int get contentLength();

  /**
   * Returns the keep alive state of the connection.
   */
  bool get keepAlive();

  /**
   * Returns the response headers.
   */
  Map get headers();

  /**
   * Sets callback to be called as request data becomes available. The
   * data delivered are the raw received. If the data is UTF-8 encoded
   * and this callback is not set the callback [dataEnd] will be
   * called with the decoded [String].
   */
  void set dataReceived(void callback(List<int> data));

  /**
   * Sets the callback to be called when all request data have been
   * received. If the callback [dataReceived] is set the [String]
   * argument will be [null] otherwise it will be the result of UTF-8
   * decoding the request body,
   */
  void set dataEnd(void callback(String data));
}


interface HTTPException {
  /*
   * Returns the error message for the exception.
   */
  String get message();
}

class HTTPUtil {
  static String decodeUrlEncodedString(String urlEncoded) {
    void invalidEscape() {
      // TODO(jrgfogh): Handle the error.
      print("Invalid escape code.");
    }

    StringBuffer result = new StringBuffer();
    for (int ii = 0; urlEncoded.length > ii; ++ii) {
      if ('+' == urlEncoded[ii]) {
        result.add(' ');
      } else if ('%' == urlEncoded[ii] &&
                 urlEncoded.length - 2 > ii) {
        try {
          int charCode =
            Math.parseInt('0x' + urlEncoded.substring(ii + 1, ii + 3));
          if (charCode <= 0x7f) {
            result.add(new String.fromCharCodes([charCode]));
            ii += 2;
          } else {
            invalidEscape();
            return '';
          }
        } catch (BadNumberFormatException ignored) {
          invalidEscape();
          return '';
        }
      } else {
        result.add(urlEncoded[ii]);
      }
    }
    return result.toString();
  }

  static Map<String, String> splitQueryString(String queryString) {
    Map<String, String> result = new Map<String, String>();
    int currentPosition = 0;
    while (currentPosition < queryString.length) {
      int position = queryString.indexOf("=", currentPosition);
      if (position == -1) {
        break;
      }
      String name = queryString.substring(currentPosition, position);
      currentPosition = position + 1;
      position = queryString.indexOf("&", currentPosition);
      String value;
      if (position == -1) {
        value = queryString.substring(currentPosition);
        currentPosition = queryString.length;
      } else {
        value = queryString.substring(currentPosition, position);
        currentPosition = position + 1;
      }
      result[HTTPUtil.decodeUrlEncodedString(name)] =
        HTTPUtil.decodeUrlEncodedString(value);
    }
    return result;
  }
}
