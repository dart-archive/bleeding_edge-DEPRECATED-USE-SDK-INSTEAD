// Copyright (c) 2014, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

library protocol;

import 'dart:convert' show JsonDecoder;

/**
 * Instances of the class [Request] represent a request that was received.
 */
class Request {
  /**
   * The name of the JSON attribute containing the id of the request.
   */
  static const String ID = 'id';

  /**
   * The name of the JSON attribute containing the name of the request.
   */
  static const String METHOD = 'method';

  /**
   * The name of the JSON attribute containing the request parameters.
   */
  static const String PARAMS = 'params';

  /**
   * The unique identifier used to identify this request.
   */
  final String id;

  /**
   * The method being requested.
   */
  final String method;

  /**
   * A table mapping the names of request parameters to their values.
   */
  final Map<String, Object> params = new Map<String, Object>();

  /**
   * A decoder that can be used to decode strings into JSON objects.
   */
  static const JsonDecoder DECODER = const JsonDecoder(null);

  /**
   * Initialize a newly created [Request] to have the given [id] and [method]
   * name.
   */
  Request(this.id, this.method);

  /**
   * Return a request parsed from the given [data], or `null` if the [data] is
   * not a valid json representation of a request. The [data] is expected to
   * have the following format:
   *
   *   {
   *     'id': String,
   *     'method': methodName,
   *     'params': {
   *       paramter_name: value
   *     }
   *   }
   *
   * where the parameters are optional and can contain any number of name/value
   * pairs.
   */
  factory Request.fromString(String data) {
    try {
      var result = DECODER.convert(data);
      if (result is! Map) {
        return null;
      }
      var id = result[Request.ID];
      var method = result[Request.METHOD];
      if (id is! String || method is! String) {
        return null;
      }
      var params = result[Request.PARAMS];
      Request request = new Request(id, method);
      if (params is Map) {
        params.forEach((String key, Object value) {
          request.setParameter(key, value);
        });
      } else if (params != null) {
        return null;
      }
      return request;
    } catch (exception) {
      return null;
    }
  }

  /**
   * Return the value of the parameter with the given [name], or `null` if there
   * is no such parameter associated with this request.
   */
  Object getParameter(String name) => params[name];

  /**
   * Return the value of the parameter with the given [name], or throw a
   * [RequestFailure] exception with an appropriate error message if there is no
   * such parameter associated with this request.
   */
  Object getRequiredParameter(String name) {
    Object value = params[name];
    if (value == null) {
      throw new RequestFailure(new Response.missingRequiredParameter(this, name));
    }
    return value;
  }

  /**
   * Set the value of the parameter with the given [name] to the given [value].
   */
  void setParameter(String name, Object value) {
    params[name] = value;
  }

  /**
   * Convert the given [value] to a boolean, or throw a [RequestFailure]
   * exception if the [value] could not be converted.
   *
   * The value is typically the result of invoking either [getParameter] or
   * [getRequiredParameter].
   */
  bool toBool(Object value) {
    if (value is bool) {
      return value;
    } else if (value is String) {
      return value == 'true';
    }
    throw new RequestFailure(new Response.expectedBoolean(this, value));
  }

  /**
   * Convert the given [value] to an integer, or throw a [RequestFailure]
   * exception if the [value] could not be converted.
   *
   * The value is typically the result of invoking either [getParameter] or
   * [getRequiredParameter].
   */
  int toInt(Object value) {
    if (value is int) {
      return value;
    } else if (value is String) {
      return int.parse(value, onError: (String value) {
        throw new RequestFailure(new Response.expectedInteger(this, value));
      });
    }
    throw new RequestFailure(new Response.expectedInteger(this, value));
  }

  /**
   * Return a table representing the structure of the Json object that will be
   * sent to the client to represent this response.
   */
  Map<String, Object> toJson() {
    Map<String, Object> jsonObject = new Map<String, Object>();
    jsonObject[ID] = id;
    jsonObject[METHOD] = method;
    if (params.isNotEmpty) {
      jsonObject[PARAMS] = params;
    }
    return jsonObject;
  }
}

/**
 * Instances of the class [Response] represent a response to a request.
 */
class Response {
  /**
   * The name of the JSON attribute containing the id of the request for which
   * this is a response.
   */
  static const String ID = 'id';

  /**
   * The name of the JSON attribute containing the error message.
   */
  static const String ERROR = 'error';

  /**
   * The name of the JSON attribute containing the result values.
   */
  static const String RESULT = 'result';

  /**
   * The unique identifier used to identify the request that this response is
   * associated with.
   */
  final String id;

  /**
   * The error that was caused by attempting to handle the request, or `null` if
   * there was no error.
   */
  final RequestError error;

  /**
   * A table mapping the names of result fields to their values. The table
   * should be empty if there was an error.
   */
  final Map<String, Object> result = new Map<String, Object>();

  /**
   * Initialize a newly created instance to represent a response to a request
   * with the given [id]. If an [error] is provided then the response will
   * represent an error condition.
   */
  Response(this.id, [this.error]);

  /**
   * Initialize a newly created instance to represent an error condition caused
   * by a [request] referencing a context that does not exist.
   */
  Response.contextDoesNotExist(Request request)
    : this(request.id, new RequestError(-1, 'Context does not exist'));

  /**
   * Initialize a newly created instance to represent an error condition caused
   * by a [request] that was expected to have a boolean-valued parameter but was
   * passed a non-boolean value.
   */
  Response.expectedBoolean(Request request, String value)
    : this(request.id, new RequestError(-2, 'Expected a boolean value, but found "$value"'));

  /**
   * Initialize a newly created instance to represent an error condition caused
   * by a [request] that was expected to have a integer-valued parameter but was
   * passed a non-integer value.
   */
  Response.expectedInteger(Request request, String value)
    : this(request.id, new RequestError(-3, 'Expected an integer value, but found "$value"'));

  /**
   * Initialize a newly created instance to represent an error condition caused
   * by a malformed request.
   */
  Response.invalidRequestFormat()
    : this('', new RequestError(-4, 'Invalid request'));

  /**
   * Initialize a newly created instance to represent an error condition caused
   * by a [request] that does not have a required parameter.
   */
  Response.missingRequiredParameter(Request request, String parameterName)
    : this(request.id, new RequestError(-5, 'Missing required parameter: $parameterName'));

  /**
   * Initialize a newly created instance to represent an error condition caused
   * by a [request] that takes a set of analysis options but for which an
   * unknown analysis option was provided.
   */
  Response.unknownAnalysisOption(Request request, String optionName)
    : this(request.id, new RequestError(-6, 'Unknown analysis option: "$optionName"'));

  /**
   * Initialize a newly created instance to represent an error condition caused
   * by a [request] that cannot be handled by any known handlers.
   */
  Response.unknownRequest(Request request)
    : this(request.id, new RequestError(-7, 'Unknown request'));

  /**
   * Initialize a newly created instance based upon the given JSON data
   */
  factory Response.fromJson(Map<String, Object> json) {
    try {
      var id = json[Response.ID];
      if (id is! String) {
        return null;
      }
      var error = json[Response.ERROR];
      var result = json[Response.RESULT];
      Response response;
      if (error is Map) {
        response = new Response(id, new RequestError.fromJson(error));
      } else {
        response = new Response(id);
      }
      if (result is Map) {
        result.forEach((String key, Object value) {
          response.setResult(key, value);
        });
      }
      return response;
    } catch (exception) {
      return null;
    }
  }

  /**
   * Return the value of the result field with the given [name].
   */
  Object getResult(String name) {
    return result[name];
  }

  /**
   * Set the value of the result field with the given [name] to the given [value].
   */
  void setResult(String name, Object value) {
    result[name] = value;
  }

  /**
   * Return a table representing the structure of the Json object that will be
   * sent to the client to represent this response.
   */
  Map<String, Object> toJson() {
    Map<String, Object> jsonObject = new Map<String, Object>();
    jsonObject[ID] = id;
    if (error == null) {
      jsonObject[ERROR] = null;
    } else {
      jsonObject[ERROR] = error.toJson();
    }
    if (!result.isEmpty) {
      jsonObject[RESULT] = result;
    }
    return jsonObject;
  }
}

/**
 * Instances of the class [RequestError] represent information about an error
 * that occurred while attempting to respond to a [Request].
 */
class RequestError {
  /**
   * The name of the JSON attribute containing the code that uniquely identifies
   * the error that occurred.
   */
  static const String CODE = 'code';

  /**
   * The name of the JSON attribute containing an object with additional data
   * related to the error.
   */
  static const String DATA = 'data';

  /**
   * The name of the JSON attribute containing a short description of the error.
   */
  static const String MESSAGE = 'message';

  /**
   * An error code indicating a parse error. Invalid JSON was received by the
   * server. An error occurred on the server while parsing the JSON text.
   */
  static const int CODE_PARSE_ERROR = -32700;

  /**
   * An error code indicating an invalid request. The JSON sent is not a valid
   * [Request] object.
   */
  static const int CODE_INVALID_REQUEST = -32600;

  /**
   * An error code indicating a method not found. The method does not exist or
   * is not currently available.
   */
  static const int CODE_METHOD_NOT_FOUND = -32601;

  /**
   * An error code indicating one or more invalid parameters.
   */
  static const int CODE_INVALID_PARAMS = -32602;

  /**
   * An error code indicating an internal error.
   */
  static const int CODE_INTERNAL_ERROR = -32603;

  /**
   * An error code indicating a problem using the specified Dart SDK.
   */
  static const int CODE_SDK_ERROR = -32603;

  /*
   * In addition, codes -32000 to -32099 indicate a server error. They are
   * reserved for implementation-defined server-errors.
   */

  /**
   * The code that uniquely identifies the error that occurred.
   */
  final int code;

  /**
   * A short description of the error.
   */
  final String message;

  /**
   * A table mapping the names of notification parameters to their values.
   */
  final Map<String, Object> data = new Map<String, Object>();

  /**
   * Initialize a newly created [Error] to have the given [code] and [message].
   */
  RequestError(this.code, this.message);

  /**
   * Initialize a newly created [Error] to indicate a parse error. Invalid JSON
   * was received by the server. An error occurred on the server while parsing
   * the JSON text.
   */
  RequestError.parseError() : this(CODE_PARSE_ERROR, "Parse error");

  /**
   * Initialize a newly created [Error] to indicate an invalid request. The
   * JSON sent is not a valid [Request] object.
   */
  RequestError.invalidRequest() : this(CODE_INVALID_REQUEST, "Invalid request");

  /**
   * Initialize a newly created [Error] to indicate that a method was not found.
   * Either the method does not exist or is not currently available.
   */
  RequestError.methodNotFound() : this(CODE_METHOD_NOT_FOUND, "Method not found");

  /**
   * Initialize a newly created [Error] to indicate one or more invalid
   * parameters.
   */
  RequestError.invalidParameters() : this(CODE_INVALID_PARAMS, "Invalid parameters");

  /**
   * Initialize a newly created [Error] to indicate an internal error.
   */
  RequestError.internalError() : this(CODE_INTERNAL_ERROR, "Internal error");

  /**
   * Initialize a newly created [Error] from the given JSON.
   */
  factory RequestError.fromJson(Map<String, Object> json) {
    try {
      int code = json[RequestError.CODE];
      String message = json[RequestError.MESSAGE];
      Map<String, Object> data = json[RequestError.DATA];
      RequestError requestError = new RequestError(code, message);
      if (data != null) {
        data.forEach((String key, Object value) {
          requestError.setData(key, value);
        });
      }
      return requestError;
    } catch (exception) {
      return null;
    }
  }

  /**
   * Return the value of the data with the given [name], or `null` if there is
   * no such data associated with this error.
   */
  Object getData(String name) => data[name];

  /**
   * Set the value of the data with the given [name] to the given [value].
   */
  void setData(String name, Object value) {
    data[name] = value;
  }

  /**
   * Return a table representing the structure of the Json object that will be
   * sent to the client to represent this response.
   */
  Map<String, Object> toJson() {
    Map<String, Object> jsonObject = new Map<String, Object>();
    jsonObject[CODE] = code;
    jsonObject[MESSAGE] = message;
    if (!data.isEmpty) {
      jsonObject[DATA] = data;
    }
    return jsonObject;
  }
}

/**
 * Instances of the class [Notification] represent a notification from the
 * server about an event that occurred.
 */
class Notification {
  /**
   * The name of the JSON attribute containing the name of the event that
   * triggered the notification.
   */
  static const String EVENT = 'event';

  /**
   * The name of the JSON attribute containing the result values.
   */
  static const String PARAMS = 'params';

  /**
   * The name of the event that triggered the notification.
   */
  final String event;

  /**
   * A table mapping the names of notification parameters to their values.
   */
  final Map<String, Object> params = new Map<String, Object>();

  /**
   * Initialize a newly created [Notification] to have the given [event] name.
   */
  Notification(this.event);

  /**
   * Initialize a newly created instance based upon the given JSON data
   */
  factory Notification.fromJson(Map<String, Object> json) {
    try {
      String event = json[Notification.EVENT];
      var params = json[Notification.PARAMS];
      Notification notification = new Notification(event);
      if (params is Map) {
        params.forEach((String key, Object value) {
          notification.setParameter(key, value);
        });
      }
      return notification;
    } catch (exception) {
      return null;
    }
  }

  /**
   * Return the value of the parameter with the given [name], or `null` if there
   * is no such parameter associated with this notification.
   */
  Object getParameter(String name) => params[name];

  /**
   * Set the value of the parameter with the given [name] to the given [value].
   */
  void setParameter(String name, Object value) {
    params[name] = value;
  }

  /**
   * Return a table representing the structure of the Json object that will be
   * sent to the client to represent this response.
   */
  Map<String, Object> toJson() {
    Map<String, Object> jsonObject = new Map<String, Object>();
    jsonObject[EVENT] = event;
    if (!params.isEmpty) {
      jsonObject[PARAMS] = params;
    }
    return jsonObject;
  }
}

/**
 * Instances of the class [RequestHandler] implement a handler that can handle
 * requests and produce responses for them.
 */
abstract class RequestHandler {
  /**
   * Attempt to handle the given [request]. If the request is not recognized by
   * this handler, return `null` so that other handlers will be given a chance
   * to handle it. Otherwise, return the response that should be passed back to
   * the client.
   */
  Response handleRequest(Request request);
}

/**
 * Instances of the class [RequestFailure] represent an exception that occurred
 * during the handling of a request that requires that an error be returned to
 * the client.
 */
class RequestFailure implements Exception {
  /**
   * The response to be returned as a result of the failure.
   */
  final Response response;

  /**
   * Initialize a newly created exception to return the given reponse.
   */
  RequestFailure(this.response);
}
