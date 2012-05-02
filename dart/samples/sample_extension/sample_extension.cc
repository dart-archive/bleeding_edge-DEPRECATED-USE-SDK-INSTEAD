// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.
#include <string.h>
#include <stdlib.h>
#include <stdio.h>
#include "dart_api.h"

Dart_NativeFunction ResolveName(Dart_Handle name, int argc);

DART_EXPORT Dart_Handle sample_extension_Init(Dart_Handle parent_library) {
  if (Dart_IsError(parent_library)) { return parent_library; }

  Dart_Handle result_code = Dart_SetNativeResolver(parent_library, ResolveName);
  if (Dart_IsError(result_code)) return result_code;

  return Dart_Null();
}

void HandleError(Dart_Handle handle) {
  if (Dart_IsError(handle)) Dart_PropagateError(handle);
}

void SystemRand(Dart_NativeArguments arguments) {
  Dart_EnterScope();
  Dart_Handle result = Dart_NewInteger(rand());
 HandleError(result);
  Dart_SetReturnValue(arguments, result);
  Dart_ExitScope();
}

void SystemSrand(Dart_NativeArguments arguments) {
  Dart_EnterScope();
  bool success = false;
  Dart_Handle seed_object = Dart_GetNativeArgument(arguments, 0);
  HandleError(seed_object);
  if (Dart_IsInteger(seed_object)) {
    bool fits;
    Dart_Handle error_check =
        Dart_IntegerFitsIntoInt64(seed_object, &fits);
    HandleError(error_check);
    if (fits) {
      int64_t seed;
      error_check = Dart_IntegerToInt64(seed_object, &seed);
      HandleError(error_check);
      srand(static_cast<unsigned>(seed));
      success = true;
    }
  }
  Dart_Handle result = Dart_NewBoolean(success);
  HandleError(result);
  Dart_SetReturnValue(arguments, result);
  Dart_ExitScope();
}

uint8_t* randomArray(int seed, int length) {
  if (length <= 0 || length > 10000000) return NULL;
  uint8_t* values = reinterpret_cast<uint8_t*>(malloc(length));
  if (NULL == values) return NULL;
  srand(seed);
  for (int i = 0; i < length; ++i) {
    values[i] = rand() % 256;
  }
  return values;
}

void wrappedRandomArray(Dart_Port dest_port_id,
                          Dart_Port reply_port_id,
                          Dart_CObject* message) {
  if (message->type == Dart_CObject::kArray &&
      2 == message->value.as_array.length) {
    Dart_CObject* param0 = message->value.as_array.values[0];
    Dart_CObject* param1 = message->value.as_array.values[1];
    if (param0->type == Dart_CObject::kInt32 &&
        param1->type == Dart_CObject::kInt32) {
      int length = param0->value.as_int32;
      int seed = param1->value.as_int32;

      uint8_t* values = randomArray(seed, length);

      if (values != NULL) {
        Dart_CObject result;
        result.type = Dart_CObject::kByteArray;
        result.value.as_byte_array.values = values;
        result.value.as_byte_array.length = length;
        Dart_PostCObject(reply_port_id, &result);
        free(values);
        return;
      }
    }
  }
  Dart_CObject result;
  result.type = Dart_CObject::kNull;
  Dart_PostCObject(reply_port_id, &result);
}

void randomArrayServicePort(Dart_NativeArguments arguments) {
  Dart_EnterScope();
  Dart_SetReturnValue(arguments, Dart_Null());
  Dart_Port service_port =
      Dart_NewNativePort("RandomArrayService", wrappedRandomArray, true);
  if (service_port != kIllegalPort) {
    Dart_Handle send_port = Dart_NewSendPort(service_port);
    Dart_SetReturnValue(arguments, send_port);
  }
  Dart_ExitScope();
}


struct FunctionLookup {
  const char* name;
  Dart_NativeFunction function;
};

FunctionLookup function_list[] = {
    {"SystemRand", SystemRand},
    {"SystemSrand", SystemSrand},
    {"RandomArray_ServicePort", randomArrayServicePort},
    {NULL, NULL}};

Dart_NativeFunction ResolveName(Dart_Handle name, int argc) {
  if (!Dart_IsString8(name)) return NULL;
  Dart_NativeFunction result = NULL;
  Dart_EnterScope();
  const char* cname;
  Dart_Handle check_error = Dart_StringToCString(name, &cname);
  HandleError(check_error);

  for (int i=0; function_list[i].name != NULL; ++i) {
    if (strcmp(function_list[i].name, cname) == 0) {
      result = function_list[i].function;
      break;
    }
  }
  Dart_ExitScope();
  return result;
}
