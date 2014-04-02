// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#include "bin/dartutils.h"

#include "include/dart_api.h"
#include "include/dart_native_api.h"

#include "platform/assert.h"
#include "platform/globals.h"

#include "bin/crypto.h"
#include "bin/directory.h"
#include "bin/extensions.h"
#include "bin/file.h"
#include "bin/io_buffer.h"
#include "bin/socket.h"
#include "bin/utils.h"

namespace dart {
namespace bin {

const char* DartUtils::original_working_directory = NULL;
const char* DartUtils::kDartScheme = "dart:";
const char* DartUtils::kDartExtensionScheme = "dart-ext:";
const char* DartUtils::kAsyncLibURL = "dart:async";
const char* DartUtils::kBuiltinLibURL = "dart:builtin";
const char* DartUtils::kCoreLibURL = "dart:core";
const char* DartUtils::kInternalLibURL = "dart:_internal";
const char* DartUtils::kIsolateLibURL = "dart:isolate";
const char* DartUtils::kIOLibURL = "dart:io";
const char* DartUtils::kIOLibPatchURL = "dart:io-patch";
const char* DartUtils::kUriLibURL = "dart:uri";
const char* DartUtils::kHttpScheme = "http:";
const char* DartUtils::kVMServiceLibURL = "dart:vmservice";

const char* DartUtils::kIdFieldName = "_id";

uint8_t DartUtils::magic_number[] = { 0xf5, 0xf5, 0xdc, 0xdc };

static bool IsWindowsHost() {
#if defined(TARGET_OS_WINDOWS)
  return true;
#else  // defined(TARGET_OS_WINDOWS)
  return false;
#endif  // defined(TARGET_OS_WINDOWS)
}


const char* DartUtils::MapLibraryUrl(CommandLineOptions* url_mapping,
                                     const char* url_string) {
  ASSERT(url_mapping != NULL);
  // We need to check if the passed in url is found in the url_mapping array,
  // in that case use the mapped entry.
  intptr_t len = strlen(url_string);
  for (intptr_t idx = 0; idx < url_mapping->count(); idx++) {
    const char* url_name = url_mapping->GetArgument(idx);
    if (!strncmp(url_string, url_name, len) && (url_name[len] == ',')) {
      const char* url_mapped_name = url_name + len + 1;
      if (strlen(url_mapped_name) != 0) {
        return url_mapped_name;  // Found a mapping for this URL.
      }
    }
  }
  return NULL;  // Did not find a mapping for this URL.
}


int64_t DartUtils::GetIntegerValue(Dart_Handle value_obj) {
  int64_t value = 0;
  Dart_Handle result = Dart_IntegerToInt64(value_obj, &value);
  if (Dart_IsError(result)) Dart_PropagateError(result);
  return value;
}


int64_t DartUtils::GetInt64ValueCheckRange(
    Dart_Handle value_obj, int64_t lower, int64_t upper) {
  int64_t value = DartUtils::GetIntegerValue(value_obj);
  if (value < lower || upper < value) {
    Dart_PropagateError(Dart_NewApiError("Value outside expected range"));
  }
  return value;
}


intptr_t DartUtils::GetIntptrValue(Dart_Handle value_obj) {
  int64_t value = 0;
  Dart_Handle result = Dart_IntegerToInt64(value_obj, &value);
  if (Dart_IsError(result)) Dart_PropagateError(result);
  if (value < kIntptrMin || kIntptrMax < value) {
    Dart_PropagateError(Dart_NewApiError("Value outside intptr_t range"));
  }
  return static_cast<intptr_t>(value);
}


bool DartUtils::GetInt64Value(Dart_Handle value_obj, int64_t* value) {
  bool valid = Dart_IsInteger(value_obj);
  if (valid) {
    Dart_Handle result = Dart_IntegerFitsIntoInt64(value_obj, &valid);
    if (Dart_IsError(result)) Dart_PropagateError(result);
  }
  if (!valid) return false;
  Dart_Handle result = Dart_IntegerToInt64(value_obj, value);
  if (Dart_IsError(result)) Dart_PropagateError(result);
  return true;
}


const char* DartUtils::GetStringValue(Dart_Handle str_obj) {
  const char* cstring = NULL;
  Dart_Handle result = Dart_StringToCString(str_obj, &cstring);
  if (Dart_IsError(result)) Dart_PropagateError(result);
  return cstring;
}


bool DartUtils::GetBooleanValue(Dart_Handle bool_obj) {
  bool value = false;
  Dart_Handle result = Dart_BooleanValue(bool_obj, &value);
  if (Dart_IsError(result)) Dart_PropagateError(result);
  return value;
}


void DartUtils::SetIntegerField(Dart_Handle handle,
                                const char* name,
                                int64_t val) {
  Dart_Handle result = Dart_SetField(handle,
                                     NewString(name),
                                     Dart_NewInteger(val));
  if (Dart_IsError(result)) Dart_PropagateError(result);
}


void DartUtils::SetStringField(Dart_Handle handle,
                               const char* name,
                               const char* val) {
  Dart_Handle result = Dart_SetField(handle, NewString(name), NewString(val));
  if (Dart_IsError(result)) Dart_PropagateError(result);
}


bool DartUtils::IsDartSchemeURL(const char* url_name) {
  static const intptr_t kDartSchemeLen = strlen(kDartScheme);
  // If the URL starts with "dart:" then it is considered as a special
  // library URL which is handled differently from other URLs.
  return (strncmp(url_name, kDartScheme, kDartSchemeLen) == 0);
}


bool DartUtils::IsHttpSchemeURL(const char* url_name) {
  static const intptr_t kHttpSchemeLen = strlen(kHttpScheme);
  return (strncmp(url_name, kHttpScheme, kHttpSchemeLen) == 0);
}


bool DartUtils::IsDartExtensionSchemeURL(const char* url_name) {
  static const intptr_t kDartExtensionSchemeLen = strlen(kDartExtensionScheme);
  // If the URL starts with "dartext:" then it is considered as a special
  // extension library URL which is handled differently from other URLs.
  return
      (strncmp(url_name, kDartExtensionScheme, kDartExtensionSchemeLen) == 0);
}


bool DartUtils::IsDartIOLibURL(const char* url_name) {
  return (strcmp(url_name, kIOLibURL) == 0);
}


bool DartUtils::IsDartBuiltinLibURL(const char* url_name) {
  return (strcmp(url_name, kBuiltinLibURL) == 0);
}


Dart_Handle DartUtils::CanonicalizeURL(CommandLineOptions* url_mapping,
                                       Dart_Handle library,
                                       const char* url_str) {
  // Get the url of the including library.
  Dart_Handle library_url = Dart_LibraryUrl(library);
  if (Dart_IsError(library_url)) {
    return Dart_NewApiError("accessing library url failed");
  }
  if (!Dart_IsString(library_url)) {
    return Dart_NewApiError("library url is not a string");
  }
  const char* library_url_str = NULL;
  Dart_Handle result = Dart_StringToCString(library_url, &library_url_str);
  if (Dart_IsError(result)) {
    return Dart_NewApiError("accessing library url characters failed");
  }
  if (url_mapping != NULL) {
    const char* mapped_library_url_str = MapLibraryUrl(url_mapping,
                                                       library_url_str);
    if (mapped_library_url_str != NULL) {
      library_url_str = mapped_library_url_str;
    }
  }
  // Calculate the canonical path.
  const char* canon_url_str = GetCanonicalPath(library_url_str, url_str);
  Dart_Handle canon_url = NewString(canon_url_str);
  free(const_cast<char*>(canon_url_str));

  return canon_url;
}


void* DartUtils::OpenFile(const char* name, bool write) {
  File* file = File::Open(name, write ? File::kWriteTruncate : File::kRead);
  return reinterpret_cast<void*>(file);
}


void DartUtils::ReadFile(const uint8_t** data,
                         intptr_t* len,
                         void* stream) {
  ASSERT(data != NULL);
  ASSERT(len != NULL);
  ASSERT(stream != NULL);
  File* file_stream = reinterpret_cast<File*>(stream);
  int64_t file_len = file_stream->Length();
  if ((file_len < 0) || (file_len > kIntptrMax)) {
    *data = NULL;
    *len = -1;  // Indicates read was not successful.
    return;
  }
  *len = static_cast<intptr_t>(file_len);
  uint8_t* text_buffer = reinterpret_cast<uint8_t*>(malloc(*len));
  ASSERT(text_buffer != NULL);
  if (!file_stream->ReadFully(text_buffer, *len)) {
    *data = NULL;
    *len = -1;  // Indicates read was not successful.
    return;
  }
  *data = text_buffer;
}


void DartUtils::WriteFile(const void* buffer,
                          intptr_t num_bytes,
                          void* stream) {
  ASSERT(stream != NULL);
  File* file_stream = reinterpret_cast<File*>(stream);
  bool bytes_written = file_stream->WriteFully(buffer, num_bytes);
  ASSERT(bytes_written);
}


void DartUtils::CloseFile(void* stream) {
  delete reinterpret_cast<File*>(stream);
}


bool DartUtils::EntropySource(uint8_t* buffer, intptr_t length) {
  return Crypto::GetRandomBytes(length, buffer);
}


static Dart_Handle SingleArgDart_Invoke(Dart_Handle lib, const char* method,
                                        Dart_Handle arg) {
  const int kNumArgs = 1;
  Dart_Handle dart_args[kNumArgs];
  dart_args[0] = arg;
  return Dart_Invoke(lib, DartUtils::NewString(method), kNumArgs, dart_args);
}


// TODO(iposva): Allocate from the zone instead of leaking error string
// here. On the other hand the binary is about to exit anyway.
#define SET_ERROR_MSG(error_msg, format, ...)                                  \
  intptr_t len = snprintf(NULL, 0, format, __VA_ARGS__);                       \
  char* msg = reinterpret_cast<char*>(malloc(len + 1));                        \
  snprintf(msg, len + 1, format, __VA_ARGS__);                                 \
  *error_msg = msg


Dart_Handle MakeHttpRequest(Dart_Handle uri, Dart_Handle builtin_lib,
                            uint8_t** buffer, intptr_t* buffer_len) {
  const intptr_t HttpResponseCodeOK = 200;
  ASSERT(buffer != NULL);
  ASSERT(buffer_len != NULL);
  ASSERT(!Dart_HasLivePorts());
  SingleArgDart_Invoke(builtin_lib, "_makeHttpRequest", uri);
  // Run until all ports to isolate are closed.
  Dart_Handle result = Dart_RunLoop();
  if (Dart_IsError(result)) {
    return result;
  }
  result = Dart_Invoke(builtin_lib,
                       DartUtils::NewString("_getHttpRequestResponseCode"),
                       0,
                       NULL);
  if (Dart_IsError(result)) {
    return result;
  }
  int64_t responseCode = DartUtils::GetIntegerValue(result);
  if (responseCode != HttpResponseCodeOK) {
    // Return error.
    Dart_Handle responseStatus =
        Dart_Invoke(builtin_lib,
                    DartUtils::NewString("_getHttpRequestStatusString"),
                    0,
                    NULL);
    if (Dart_IsError(responseStatus)) {
      return responseStatus;
    }
    if (Dart_IsNull(responseStatus)) {
      return Dart_NewApiError("HTTP error.");
    }
    return Dart_NewApiError(DartUtils::GetStringValue(responseStatus));
  }
  Dart_Handle response =
      Dart_Invoke(builtin_lib, DartUtils::NewString("_getHttpRequestResponse"),
                  0, NULL);
  if (Dart_IsError(response)) {
    return response;
  }
  if (Dart_IsString(response)) {
    // Received response as string.
    uint8_t* responseString = NULL;
    intptr_t responseStringLength;
    Dart_Handle r = Dart_StringToUTF8(response, &responseString,
                                      &responseStringLength);
    if (Dart_IsError(r)) {
      *buffer = NULL;
      *buffer_len = 0;
      return r;
    }
    // Get payload as bytes.
    *buffer_len = responseStringLength;
    *buffer = reinterpret_cast<uint8_t*>(malloc(responseStringLength));
    memmove(*buffer, responseString, responseStringLength);
  } else {
    // Received response as list of bytes.
    ASSERT(Dart_IsList(response));
    // Query list length.
    result = Dart_ListLength(response, buffer_len);
    if (Dart_IsError(result)) {
      *buffer_len = 0;
      *buffer = NULL;
      return result;
    }
    // Get payload as bytes.
    *buffer = reinterpret_cast<uint8_t*>(malloc(*buffer_len));
    result = Dart_ListGetAsBytes(response, 0, *buffer, *buffer_len);
    if (Dart_IsError(result)) {
      free(*buffer);
      *buffer_len = 0;
      *buffer = NULL;
      return result;
    }
  }
  return result;
}


Dart_Handle DartUtils::ReadStringFromHttp(const char* script_uri) {
  Dart_Handle uri = NewString(script_uri);
  if (Dart_IsError(uri)) {
    return uri;
  }
  Dart_Handle builtin_lib =
      Builtin::LoadAndCheckLibrary(Builtin::kBuiltinLibrary);
  uint8_t* buffer;
  intptr_t bufferLen;
  Dart_Handle result = MakeHttpRequest(uri, builtin_lib, &buffer, &bufferLen);
  if (Dart_IsError(result)) {
    return result;
  }
  Dart_Handle str = Dart_NewStringFromUTF8(buffer,
                                           bufferLen);
  free(buffer);
  return str;
}


static const uint8_t* ReadFileFully(const char* filename,
                                    intptr_t* file_len,
                                    const char** error_msg) {
  *file_len = -1;
  void* stream = DartUtils::OpenFile(filename, false);
  if (stream == NULL) {
    SET_ERROR_MSG(error_msg, "Unable to open file: %s", filename);
    return NULL;
  }
  const uint8_t* text_buffer = NULL;
  DartUtils::ReadFile(&text_buffer, file_len, stream);
  if (text_buffer == NULL || *file_len == -1) {
    *error_msg = "Unable to read file contents";
    text_buffer = NULL;
  }
  DartUtils::CloseFile(stream);
  return text_buffer;
}


Dart_Handle DartUtils::ReadStringFromFile(const char* filename) {
  const char* error_msg = NULL;
  intptr_t len;
  const uint8_t* text_buffer = ReadFileFully(filename, &len, &error_msg);
  if (text_buffer == NULL) {
    return Dart_NewApiError(error_msg);
  }
  Dart_Handle str = Dart_NewStringFromUTF8(text_buffer, len);
  free(const_cast<uint8_t *>(text_buffer));
  return str;
}


Dart_Handle DartUtils::SetWorkingDirectory(Dart_Handle builtin_lib) {
  Dart_Handle directory = NewString(original_working_directory);
  return SingleArgDart_Invoke(builtin_lib, "_setWorkingDirectory", directory);
}


Dart_Handle DartUtils::ResolveScriptUri(Dart_Handle script_uri,
                                        Dart_Handle builtin_lib) {
  const int kNumArgs = 1;
  Dart_Handle dart_args[kNumArgs];
  dart_args[0] = script_uri;
  return Dart_Invoke(builtin_lib,
                     NewString("_resolveScriptUri"),
                     kNumArgs,
                     dart_args);
}


Dart_Handle DartUtils::FilePathFromUri(Dart_Handle script_uri,
                                       Dart_Handle builtin_lib) {
  const int kNumArgs = 1;
  Dart_Handle dart_args[kNumArgs];
  dart_args[0] = script_uri;
  return Dart_Invoke(builtin_lib,
                     NewString("_filePathFromUri"),
                     kNumArgs,
                     dart_args);
}


Dart_Handle DartUtils::ExtensionPathFromUri(Dart_Handle extension_uri,
                                            Dart_Handle builtin_lib) {
  const int kNumArgs = 1;
  Dart_Handle dart_args[kNumArgs];
  dart_args[0] = extension_uri;
  return Dart_Invoke(builtin_lib,
                     NewString("_extensionPathFromUri"),
                     kNumArgs,
                     dart_args);
}


Dart_Handle DartUtils::ResolveUri(Dart_Handle library_url,
                                  Dart_Handle url,
                                  Dart_Handle builtin_lib) {
  const int kNumArgs = 2;
  Dart_Handle dart_args[kNumArgs];
  dart_args[0] = library_url;
  dart_args[1] = url;
  return Dart_Invoke(
      builtin_lib, NewString("_resolveUri"), kNumArgs, dart_args);
}


Dart_Handle DartUtils::LibraryTagHandler(Dart_LibraryTag tag,
                                         Dart_Handle library,
                                         Dart_Handle url) {
  if (!Dart_IsLibrary(library)) {
    return Dart_NewApiError("not a library");
  }
  if (!Dart_IsString(url)) {
    return Dart_NewApiError("url is not a string");
  }
  const char* url_string = NULL;
  Dart_Handle result = Dart_StringToCString(url, &url_string);
  if (Dart_IsError(result)) {
    return result;
  }
  Dart_Handle library_url = Dart_LibraryUrl(library);
  const char* library_url_string = NULL;
  result = Dart_StringToCString(library_url, &library_url_string);
  if (Dart_IsError(result)) {
    return result;
  }

  bool is_dart_scheme_url = DartUtils::IsDartSchemeURL(url_string);
  bool is_io_library = DartUtils::IsDartIOLibURL(library_url_string);

  // Handle URI canonicalization requests.
  if (tag == Dart_kCanonicalizeUrl) {
    // If this is a Dart Scheme URL or 'part' of a io library
    // then it is not modified as it will be handled internally.
    if (is_dart_scheme_url || is_io_library) {
      return url;
    }
    // Resolve the url within the context of the library's URL.
    Dart_Handle builtin_lib =
        Builtin::LoadAndCheckLibrary(Builtin::kBuiltinLibrary);
    Dart_Handle library_url = Dart_LibraryUrl(library);
    if (Dart_IsError(library_url)) {
      return library_url;
    }
    return ResolveUri(library_url, url, builtin_lib);
  }

  // Handle 'import' of dart scheme URIs (i.e they start with 'dart:').
  if (is_dart_scheme_url) {
    if (tag == Dart_kImportTag) {
      // Handle imports of other built-in libraries present in the SDK.
      if (DartUtils::IsDartIOLibURL(url_string)) {
        return Builtin::LoadAndCheckLibrary(Builtin::kIOLibrary);
      }
      return NewError("The built-in library '%s' is not available"
                      " on the stand-alone VM.\n", url_string);
    } else {
      ASSERT(tag == Dart_kSourceTag);
      return NewError("Unable to load source '%s' ", url_string);
    }
  }

  // Handle 'part' of IO library.
  if (is_io_library) {
    if (tag == Dart_kSourceTag) {
      // Prepend the library URI to form a unique script URI for the part.
      intptr_t len = snprintf(NULL, 0, "%s/%s", library_url_string, url_string);
      char* part_uri = reinterpret_cast<char*>(malloc(len + 1));
      snprintf(part_uri, len + 1, "%s/%s", library_url_string, url_string);
      Dart_Handle part_uri_obj = DartUtils::NewString(part_uri);
      free(part_uri);
      return Dart_LoadSource(
          library,
          part_uri_obj,
          Builtin::PartSource(Builtin::kIOLibrary, url_string));
    } else {
      ASSERT(tag == Dart_kImportTag);
      return NewError("Unable to import '%s' ", url_string);
    }
  }

  Dart_Handle builtin_lib =
      Builtin::LoadAndCheckLibrary(Builtin::kBuiltinLibrary);
  if (DartUtils::IsDartExtensionSchemeURL(url_string)) {
    // Load a native code shared library to use in a native extension
    if (tag != Dart_kImportTag) {
      return NewError("Dart extensions must use import: '%s'", url_string);
    }
    Dart_Handle path_parts = DartUtils::ExtensionPathFromUri(url, builtin_lib);
    if (Dart_IsError(path_parts)) {
      return path_parts;
    }
    const char* extension_directory = NULL;
    Dart_StringToCString(Dart_ListGetAt(path_parts, 0), &extension_directory);
    const char* extension_filename = NULL;
    Dart_StringToCString(Dart_ListGetAt(path_parts, 1), &extension_filename);
    const char* extension_name = NULL;
    Dart_StringToCString(Dart_ListGetAt(path_parts, 2), &extension_name);

    return Extensions::LoadExtension(extension_directory,
                                     extension_filename,
                                     extension_name,
                                     library);
  } else {
    // Handle 'import' or 'part' requests for all other URIs.
    // Get the file path out of the url.
    Dart_Handle file_path = DartUtils::FilePathFromUri(url, builtin_lib);
    if (Dart_IsError(file_path)) {
      return file_path;
    }
    const char* final_path = NULL;
    Dart_StringToCString(file_path, &final_path);
    result = DartUtils::LoadSource(NULL,
                                   library,
                                   url,
                                   tag,
                                   final_path);
    return result;
  }
}


const uint8_t* DartUtils::SniffForMagicNumber(const uint8_t* text_buffer,
                                              intptr_t* buffer_len,
                                              bool* is_snapshot) {
  intptr_t len = sizeof(magic_number);
  for (intptr_t i = 0; i < len; i++) {
    if (text_buffer[i] != magic_number[i]) {
      *is_snapshot = false;
      return text_buffer;
    }
  }
  *is_snapshot = true;
  ASSERT(*buffer_len > len);
  *buffer_len -= len;
  return text_buffer + len;
}


void DartUtils::WriteMagicNumber(File* file) {
  // Write a magic number and version information into the snapshot file.
  bool bytes_written = file->WriteFully(magic_number, sizeof(magic_number));
  ASSERT(bytes_written);
}


Dart_Handle DartUtils::LoadScriptHttp(Dart_Handle uri,
                                      Dart_Handle builtin_lib) {
  intptr_t len = 0;
  uint8_t* buffer = NULL;
  Dart_Handle result = MakeHttpRequest(uri, builtin_lib, &buffer, &len);
  if (Dart_IsError(result)) {
    return result;
  }
  const uint8_t* payload = buffer;
  bool is_snapshot = false;
  payload = SniffForMagicNumber(payload, &len, &is_snapshot);
  if (is_snapshot) {
    return Dart_LoadScriptFromSnapshot(payload, len);
  } else {
    Dart_Handle source = Dart_NewStringFromUTF8(payload, len);
    free(buffer);
    if (Dart_IsError(source)) {
      return source;
    }
    return Dart_LoadScript(uri, source, 0, 0);
  }
}


Dart_Handle DartUtils::LoadScript(const char* script_uri,
                                  Dart_Handle builtin_lib) {
  Dart_Handle resolved_script_uri =
      ResolveScriptUri(NewString(script_uri), builtin_lib);
  if (Dart_IsError(resolved_script_uri)) {
    return resolved_script_uri;
  }
  // Handle http: requests separately.
  if (DartUtils::IsHttpSchemeURL(script_uri)) {
    return LoadScriptHttp(resolved_script_uri, builtin_lib);
  }
  Dart_Handle script_path = DartUtils::FilePathFromUri(resolved_script_uri,
                                                       builtin_lib);
  if (Dart_IsError(script_path)) {
    return script_path;
  }
  const char* script_path_cstr;
  Dart_StringToCString(script_path, &script_path_cstr);
  const char* error_msg = NULL;
  intptr_t len;
  const uint8_t* buffer = ReadFileFully(script_path_cstr,
                                        &len,
                                        &error_msg);
  if (buffer == NULL) {
    return Dart_NewApiError(error_msg);
  }
  bool is_snapshot = false;
  const uint8_t *payload = SniffForMagicNumber(buffer, &len, &is_snapshot);
  Dart_Handle returnValue;
  if (is_snapshot) {
    returnValue = Dart_LoadScriptFromSnapshot(payload, len);
  } else {
    Dart_Handle source = Dart_NewStringFromUTF8(buffer, len);
    if (Dart_IsError(source)) {
      returnValue = NewError("%s is not a valid UTF-8 script", script_uri);
    } else {
      returnValue = Dart_LoadScript(resolved_script_uri, source, 0, 0);
    }
  }
  free(const_cast<uint8_t *>(buffer));
  return returnValue;
}


Dart_Handle DartUtils::LoadSource(CommandLineOptions* url_mapping,
                                  Dart_Handle library,
                                  Dart_Handle url,
                                  Dart_LibraryTag tag,
                                  const char* url_string) {
  bool is_http_scheme_url = DartUtils::IsHttpSchemeURL(url_string);
  if (url_mapping != NULL && IsDartSchemeURL(url_string)) {
    const char* mapped_url_string = MapLibraryUrl(url_mapping, url_string);
    if (mapped_url_string == NULL) {
      return NewError("Do not know how to load %s", url_string);
    }
    // We have a URL mapping specified, just read the file that the
    // URL mapping specifies and load it.
    url_string = mapped_url_string;
  }
  Dart_Handle source;
  if (is_http_scheme_url) {
    // Read the file over http.
    source = DartUtils::ReadStringFromHttp(url_string);
  } else {
    // Read the file.
    source = DartUtils::ReadStringFromFile(url_string);
  }
  if (Dart_IsError(source)) {
    return source;  // source contains the error string.
  }
  // The tag is either an import or a source tag.
  // Load it according to the specified tag.
  if (tag == Dart_kImportTag) {
    // Return library object or an error string.
    return Dart_LoadLibrary(url, source);
  } else if (tag == Dart_kSourceTag) {
    return Dart_LoadSource(library, url, source);
  }
  return Dart_NewApiError("wrong tag");
}


Dart_Handle DartUtils::PrepareForScriptLoading(const char* package_root,
                                               Dart_Handle builtin_lib) {
  // Setup the internal library's 'internalPrint' function.
  Dart_Handle print = Dart_Invoke(
      builtin_lib, NewString("_getPrintClosure"), 0, NULL);
  Dart_Handle url = NewString(kInternalLibURL);
  DART_CHECK_VALID(url);
  Dart_Handle internal_lib = Dart_LookupLibrary(url);
  DART_CHECK_VALID(internal_lib);
  Dart_Handle result = Dart_SetField(internal_lib,
                                     NewString("_printClosure"),
                                     print);
  DART_CHECK_VALID(result);

  // Setup the 'timer' factory.
  url = NewString(kAsyncLibURL);
  DART_CHECK_VALID(url);
  Dart_Handle async_lib = Dart_LookupLibrary(url);
  DART_CHECK_VALID(async_lib);
  Dart_Handle io_lib = Builtin::LoadAndCheckLibrary(Builtin::kIOLibrary);
  Dart_Handle timer_closure =
      Dart_Invoke(io_lib, NewString("_getTimerFactoryClosure"), 0, NULL);
  Dart_Handle args[1];
  args[0] = timer_closure;
  DART_CHECK_VALID(Dart_Invoke(
      async_lib, NewString("_setTimerFactoryClosure"), 1, args));

  // Setup the 'scheduleImmediate' closure.
  url = NewString(kIsolateLibURL);
  DART_CHECK_VALID(url);
  Dart_Handle isolate_lib = Dart_LookupLibrary(url);
  DART_CHECK_VALID(isolate_lib);
  Dart_Handle schedule_immediate_closure =
      Dart_Invoke(isolate_lib, NewString("_getIsolateScheduleImmediateClosure"),
                  0, NULL);
  args[0] = schedule_immediate_closure;
  DART_CHECK_VALID(Dart_Invoke(
      async_lib, NewString("_setScheduleImmediateClosure"), 1, args));

  // Setup the corelib 'Uri.base' getter.
  url = NewString(kCoreLibURL);
  DART_CHECK_VALID(url);
  Dart_Handle corelib = Dart_LookupLibrary(url);
  DART_CHECK_VALID(corelib);
  Dart_Handle uri_base = Dart_Invoke(
      builtin_lib, NewString("_getUriBaseClosure"), 0, NULL);
  DART_CHECK_VALID(uri_base);
  result = Dart_SetField(corelib,
                         NewString("_uriBaseClosure"),
                         uri_base);
  DART_CHECK_VALID(result);

  if (IsWindowsHost()) {
    // Set running on Windows flag.
    result = Dart_Invoke(builtin_lib, NewString("_setWindows"), 0, NULL);
    if (Dart_IsError(result)) {
      return result;
    }
  }

  // Set current working directory.
  result = SetWorkingDirectory(builtin_lib);
  if (Dart_IsError(result)) {
    return result;
  }

  // Set up package root if specified.
  if (package_root != NULL) {
    result = NewString(package_root);
    if (!Dart_IsError(result)) {
      const int kNumArgs = 1;
      Dart_Handle dart_args[kNumArgs];
      dart_args[0] = result;
      return Dart_Invoke(builtin_lib,
                         NewString("_setPackageRoot"),
                         kNumArgs,
                         dart_args);
    }
  }
  return result;
}


const char* DartUtils::GetCanonicalPath(const char* reference_dir,
                                        const char* filename) {
  if (File::IsAbsolutePath(filename)) {
    return strdup(filename);
  }

  char* canonical_path = File::GetCanonicalPath(reference_dir);
  if  (canonical_path == NULL) {
    canonical_path = strdup(reference_dir);
    ASSERT(canonical_path != NULL);
  }
  ASSERT(File::PathSeparator() != NULL && strlen(File::PathSeparator()) == 1);
  char* path_sep = strrchr(canonical_path, File::PathSeparator()[0]);
  if (path_sep == NULL) {
    // No separator found: Reference is a file in local directory.
    free(canonical_path);
    return strdup(filename);
  }
  *path_sep = '\0';
  intptr_t len = snprintf(NULL, 0, "%s%s%s",
                          canonical_path, File::PathSeparator(), filename);
  char* absolute_filename = reinterpret_cast<char*>(malloc(len + 1));
  ASSERT(absolute_filename != NULL);

  snprintf(absolute_filename, len + 1, "%s%s%s",
           canonical_path, File::PathSeparator(), filename);
  free(canonical_path);
  canonical_path = File::GetCanonicalPath(absolute_filename);
  if (canonical_path == NULL) {
    return absolute_filename;
  }
  free(absolute_filename);
  return canonical_path;
}


bool DartUtils::PostNull(Dart_Port port_id) {
  // Post a message with just the null object.
  return Dart_PostCObject(port_id, CObject::Null()->AsApiCObject());
}


bool DartUtils::PostInt32(Dart_Port port_id, int32_t value) {
  // Post a message with the integer value.
  int32_t min = 0xc0000000;  // -1073741824
  int32_t max = 0x3fffffff;  // 1073741823
  ASSERT(min <= value && value < max);
  Dart_CObject object;
  object.type = Dart_CObject_kInt32;
  object.value.as_int32 = value;
  return Dart_PostCObject(port_id, &object);
}


bool DartUtils::PostInt64(Dart_Port port_id, int64_t value) {
  // Post a message with the integer value.
  Dart_CObject object;
  object.type = Dart_CObject_kInt64;
  object.value.as_int64 = value;
  return Dart_PostCObject(port_id, &object);
}


Dart_Handle DartUtils::GetDartType(const char* library_url,
                                   const char* class_name) {
  return Dart_GetType(Dart_LookupLibrary(NewString(library_url)),
                      NewString(class_name), 0, NULL);
}


Dart_Handle DartUtils::NewDartOSError() {
  // Extract the current OS error.
  OSError os_error;
  return NewDartOSError(&os_error);
}


Dart_Handle DartUtils::NewDartOSError(OSError* os_error) {
  // Create a dart:io OSError object with the information retrieved from the OS.
  Dart_Handle type = GetDartType(kIOLibURL, "OSError");
  ASSERT(!Dart_IsError(type));
  Dart_Handle args[2];
  args[0] = NewString(os_error->message());
  args[1] = Dart_NewInteger(os_error->code());
  return Dart_New(type, Dart_Null(), 2, args);
}


Dart_Handle DartUtils::NewDartExceptionWithOSError(const char* library_url,
                                                   const char* exception_name,
                                                   const char* message,
                                                   Dart_Handle os_error) {
  // Create a Dart Exception object with a message and an OSError.
  Dart_Handle type = GetDartType(library_url, exception_name);
  ASSERT(!Dart_IsError(type));
  Dart_Handle args[2];
  args[0] = NewString(message);
  args[1] = os_error;
  return Dart_New(type, Dart_Null(), 2, args);
}


Dart_Handle DartUtils::NewDartExceptionWithMessage(const char* library_url,
                                                   const char* exception_name,
                                                   const char* message) {
  // Create a Dart Exception object with a message.
  Dart_Handle type = GetDartType(library_url, exception_name);
  ASSERT(!Dart_IsError(type));
  if (message != NULL) {
    Dart_Handle args[1];
    args[0] = NewString(message);
    return Dart_New(type, Dart_Null(), 1, args);
  } else {
    return Dart_New(type, Dart_Null(), 0, NULL);
  }
}


Dart_Handle DartUtils::NewDartArgumentError(const char* message) {
  return NewDartExceptionWithMessage(kCoreLibURL,
                                     "ArgumentError",
                                     message);
}


Dart_Handle DartUtils::NewDartIOException(const char* exception_name,
                                          const char* message,
                                          Dart_Handle os_error) {
  // Create a dart:io exception object of the given type.
  return NewDartExceptionWithOSError(kIOLibURL,
                                     exception_name,
                                     message,
                                     os_error);
}


Dart_Handle DartUtils::NewError(const char* format, ...) {
  va_list args;
  va_start(args, format);
  intptr_t len = vsnprintf(NULL, 0, format, args);
  va_end(args);

  char* buffer = reinterpret_cast<char*>(Dart_ScopeAllocate(len + 1));
  va_list args2;
  va_start(args2, format);
  vsnprintf(buffer, (len + 1), format, args2);
  va_end(args2);

  return Dart_NewApiError(buffer);
}


Dart_Handle DartUtils::NewInternalError(const char* message) {
  return NewDartExceptionWithMessage(kCoreLibURL, "_InternalError", message);
}


bool DartUtils::SetOriginalWorkingDirectory() {
  original_working_directory = Directory::Current();
  return original_working_directory != NULL;
}


// Statically allocated Dart_CObject instances for immutable
// objects. As these will be used by different threads the use of
// these depends on the fact that the marking internally in the
// Dart_CObject structure is not marking simple value objects.
Dart_CObject CObject::api_null_ = { Dart_CObject_kNull , { 0 } };
Dart_CObject CObject::api_true_ = { Dart_CObject_kBool , { true } };
Dart_CObject CObject::api_false_ = { Dart_CObject_kBool, { false } };
CObject CObject::null_ = CObject(&api_null_);
CObject CObject::true_ = CObject(&api_true_);
CObject CObject::false_ = CObject(&api_false_);


CObject* CObject::Null() {
  return &null_;
}


CObject* CObject::True() {
  return &true_;
}


CObject* CObject::False() {
  return &false_;
}


CObject* CObject::Bool(bool value) {
  return value ? &true_ : &false_;
}


Dart_CObject* CObject::New(Dart_CObject_Type type, int additional_bytes) {
  Dart_CObject* cobject = reinterpret_cast<Dart_CObject*>(
      Dart_ScopeAllocate(sizeof(Dart_CObject) + additional_bytes));
  cobject->type = type;
  return cobject;
}


Dart_CObject* CObject::NewInt32(int32_t value) {
  Dart_CObject* cobject = New(Dart_CObject_kInt32);
  cobject->value.as_int32 = value;
  return cobject;
}


Dart_CObject* CObject::NewInt64(int64_t value) {
  Dart_CObject* cobject = New(Dart_CObject_kInt64);
  cobject->value.as_int64 = value;
  return cobject;
}


Dart_CObject* CObject::NewIntptr(intptr_t value) {
  // Pointer values passed as intptr_t are always send as int64_t.
  Dart_CObject* cobject = New(Dart_CObject_kInt64);
  cobject->value.as_int64 = value;
  return cobject;
}


Dart_CObject* CObject::NewDouble(double value) {
  Dart_CObject* cobject = New(Dart_CObject_kDouble);
  cobject->value.as_double = value;
  return cobject;
}


Dart_CObject* CObject::NewString(intptr_t length) {
  Dart_CObject* cobject = New(Dart_CObject_kString, length + 1);
  cobject->value.as_string = reinterpret_cast<char*>(cobject + 1);
  return cobject;
}


Dart_CObject* CObject::NewString(const char* str) {
  intptr_t length = strlen(str);
  Dart_CObject* cobject = NewString(length);
  memmove(cobject->value.as_string, str, length + 1);
  return cobject;
}


Dart_CObject* CObject::NewArray(intptr_t length) {
  Dart_CObject* cobject =
      New(Dart_CObject_kArray, length * sizeof(Dart_CObject*));  // NOLINT
  cobject->value.as_array.length = length;
  cobject->value.as_array.values =
      reinterpret_cast<Dart_CObject**>(cobject + 1);
  return cobject;
}


Dart_CObject* CObject::NewUint8Array(intptr_t length) {
  Dart_CObject* cobject = New(Dart_CObject_kTypedData, length);
  cobject->value.as_typed_data.type = Dart_TypedData_kUint8;
  cobject->value.as_typed_data.length = length;
  cobject->value.as_typed_data.values = reinterpret_cast<uint8_t*>(cobject + 1);
  return cobject;
}


Dart_CObject* CObject::NewExternalUint8Array(
    intptr_t length, uint8_t* data, void* peer,
    Dart_WeakPersistentHandleFinalizer callback) {
  Dart_CObject* cobject = New(Dart_CObject_kExternalTypedData);
  cobject->value.as_external_typed_data.type = Dart_TypedData_kUint8;
  cobject->value.as_external_typed_data.length = length;
  cobject->value.as_external_typed_data.data = data;
  cobject->value.as_external_typed_data.peer = peer;
  cobject->value.as_external_typed_data.callback = callback;
  return cobject;
}


Dart_CObject* CObject::NewIOBuffer(int64_t length) {
  // Make sure that we do not have an integer overflow here. Actual check
  // against max elements will be done at the time of writing, as the constant
  // is not part of the public API.
  if ((length < 0) || (length > kIntptrMax)) {
    return NULL;
  }
  uint8_t* data = IOBuffer::Allocate(static_cast<intptr_t>(length));
  ASSERT(data != NULL);
  return NewExternalUint8Array(
      static_cast<intptr_t>(length), data, data, IOBuffer::Finalizer);
}


void CObject::FreeIOBufferData(Dart_CObject* cobject) {
  ASSERT(cobject->type == Dart_CObject_kExternalTypedData);
  cobject->value.as_external_typed_data.callback(
      NULL,
      NULL,
      cobject->value.as_external_typed_data.peer);
  cobject->value.as_external_typed_data.data = NULL;
}


CObject* CObject::IllegalArgumentError() {
  CObjectArray* result = new CObjectArray(CObject::NewArray(1));
  result->SetAt(0, new CObjectInt32(CObject::NewInt32(kArgumentError)));
  return result;
}


CObject* CObject::FileClosedError() {
  CObjectArray* result = new CObjectArray(CObject::NewArray(1));
  result->SetAt(0, new CObjectInt32(CObject::NewInt32(kFileClosedError)));
  return result;
}


CObject* CObject::NewOSError() {
  OSError os_error;
  return NewOSError(&os_error);
}

CObject* CObject::NewOSError(OSError* os_error) {
  CObject* error_message =
      new CObjectString(CObject::NewString(os_error->message()));
  CObjectArray* result = new CObjectArray(CObject::NewArray(3));
  result->SetAt(0, new CObjectInt32(CObject::NewInt32(kOSError)));
  result->SetAt(1, new CObjectInt32(CObject::NewInt32(os_error->code())));
  result->SetAt(2, error_message);
  return result;
}

}  // namespace bin
}  // namespace dart
