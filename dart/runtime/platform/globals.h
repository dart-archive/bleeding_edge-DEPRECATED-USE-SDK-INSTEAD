// Copyright (c) 2012, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#ifndef PLATFORM_GLOBALS_H_
#define PLATFORM_GLOBALS_H_

// __STDC_FORMAT_MACROS has to be defined before including <inttypes.h> to
// enable platform independent printf format specifiers.
#ifndef __STDC_FORMAT_MACROS
#define __STDC_FORMAT_MACROS
#endif

#if defined(_WIN32)
// Cut down on the amount of stuff that gets included via windows.h.
#define WIN32_LEAN_AND_MEAN
#define NOMINMAX
#define NOKERNEL
#define NOUSER
#define NOSERVICE
#define NOSOUND
#define NOMCX
#define _UNICODE
#define UNICODE
#include <windows.h>
#include <winsock2.h>
#include <Rpc.h>
#include <shellapi.h>
#endif

#if !defined(_WIN32)
#include <arpa/inet.h>
#include <inttypes.h>
#include <stdint.h>
#include <unistd.h>
#endif

#include <float.h>
#include <limits.h>
#include <math.h>
#include <stdarg.h>
#include <stddef.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <sys/types.h>

#if defined(_WIN32)
#include "platform/c99_support_win.h"
#include "platform/inttypes_support_win.h"
#include "platform/floating_point_win.h"
#endif

#if !defined(_WIN32)
#include "platform/floating_point.h"
#endif


// Target OS detection.
// for more information on predefined macros:
//   - http://msdn.microsoft.com/en-us/library/b0084kay.aspx
//   - with gcc, run: "echo | gcc -E -dM -"
#if defined(__ANDROID__)
#define TARGET_OS_ANDROID 1
#elif defined(__linux__) || defined(__FreeBSD__)
#define TARGET_OS_LINUX 1
#elif defined(__APPLE__)
#define TARGET_OS_MACOS 1
#elif defined(_WIN32)
#define TARGET_OS_WINDOWS 1
#else
#error Automatic target os detection failed.
#endif

struct simd128_value_t {
  float storage[4];
  simd128_value_t& readFrom(const float* v) {
    storage[0] = v[0];
    storage[1] = v[1];
    storage[2] = v[2];
    storage[3] = v[3];
    return *this;
  }
  simd128_value_t& readFrom(const int32_t* v) {
    const float* vv = reinterpret_cast<const float*>(v);
    storage[0] = vv[0];
    storage[1] = vv[1];
    storage[2] = vv[2];
    storage[3] = vv[3];
    return *this;
  }
  simd128_value_t& readFrom(const simd128_value_t* v) {
    *this = *v;
    return *this;
  }
  void writeTo(float* v) {
    v[0] = storage[0];
    v[1] = storage[1];
    v[2] = storage[2];
    v[3] = storage[3];
  }
  void writeTo(int32_t* v) {
    float* vv = reinterpret_cast<float*>(v);
    vv[0] = storage[0];
    vv[1] = storage[1];
    vv[2] = storage[2];
    vv[3] = storage[3];
  }
  void writeTo(simd128_value_t* v) {
    *v = *this;
  }
};

// Processor architecture detection.  For more info on what's defined, see:
//   http://msdn.microsoft.com/en-us/library/b0084kay.aspx
//   http://www.agner.org/optimize/calling_conventions.pdf
//   or with gcc, run: "echo | gcc -E -dM -"
#if defined(_M_X64) || defined(__x86_64__)
#define HOST_ARCH_X64 1
#define ARCH_IS_64_BIT 1
#define kFpuRegisterSize 16
typedef simd128_value_t fpu_register_t;
#elif defined(_M_IX86) || defined(__i386__)
#define HOST_ARCH_IA32 1
#define ARCH_IS_32_BIT 1
#if defined(TARGET_ARCH_MIPS)
#define kFpuRegisterSize 8
typedef double fpu_register_t;
#else
#define kFpuRegisterSize 16
typedef simd128_value_t fpu_register_t;
#endif
#elif defined(__ARMEL__)
#define HOST_ARCH_ARM 1
#define ARCH_IS_32_BIT 1
#define kFpuRegisterSize 16
typedef struct {
  union {
    uint32_t u;
    float    f;
  } data_[4];
} simd_value_t;
typedef simd_value_t fpu_register_t;
#define simd_value_safe_load(addr)                                             \
  (*reinterpret_cast<simd_value_t *>(addr))
#define simd_value_safe_store(addr, value)                                     \
  do {                                                                         \
    reinterpret_cast<simd_value_t *>(addr)->data_[0] = value.data_[0];         \
    reinterpret_cast<simd_value_t *>(addr)->data_[1] = value.data_[1];         \
    reinterpret_cast<simd_value_t *>(addr)->data_[2] = value.data_[2];         \
    reinterpret_cast<simd_value_t *>(addr)->data_[3] = value.data_[3];         \
  } while (0)

#elif defined(__MIPSEL__)
#define HOST_ARCH_MIPS 1
#define ARCH_IS_32_BIT 1
#define kFpuRegisterSize 8
typedef double fpu_register_t;
#else
#error Architecture was not detected as supported by Dart.
#endif

// DART_FORCE_INLINE strongly hints to the compiler that a function should
// be inlined. Your function is not guaranteed to be inlined but this is
// stronger than just using "inline".
// See: http://msdn.microsoft.com/en-us/library/z8y1yy88.aspx for an
// explanation of some the cases when a function can never be inlined.
#ifdef _MSC_VER
#define DART_FORCE_INLINE __forceinline
#elif __GNUC__
#define DART_FORCE_INLINE inline __attribute__((always_inline))
#else
#error Automatic compiler detection failed.
#endif

// DART_UNUSED inidicates to the compiler that a variable/typedef is expected
// to be unused and disables the related warning.
#ifdef __GNUC__
#define DART_UNUSED __attribute__((unused))
#else
#define DART_UNUSED
#endif

#if !defined(TARGET_ARCH_MIPS)
#if !defined(TARGET_ARCH_ARM)
#if !defined(TARGET_ARCH_X64)
#if !defined(TARGET_ARCH_IA32)
// No target architecture specified pick the one matching the host architecture.
#if defined(HOST_ARCH_MIPS)
#define TARGET_ARCH_MIPS 1
#elif defined(HOST_ARCH_ARM)
#define TARGET_ARCH_ARM 1
#elif defined(HOST_ARCH_X64)
#define TARGET_ARCH_X64 1
#elif defined(HOST_ARCH_IA32)
#define TARGET_ARCH_IA32 1
#else
#error Automatic target architecture detection failed.
#endif
#endif
#endif
#endif
#endif

// Verify that host and target architectures match, we cannot
// have a 64 bit Dart VM generating 32 bit code or vice-versa.
#if defined(TARGET_ARCH_X64)
#if !defined(ARCH_IS_64_BIT)
#error Mismatched Host/Target architectures.
#endif
#elif defined(TARGET_ARCH_IA32) ||                                             \
      defined(TARGET_ARCH_ARM) ||                                              \
      defined(TARGET_ARCH_MIPS)
#if !defined(ARCH_IS_32_BIT)
#error Mismatched Host/Target architectures.
#endif
#endif


// Short form printf format specifiers
#define Pd PRIdPTR
#define Pu PRIuPTR
#define Px PRIxPTR
#define Pd64 PRId64
#define Pu64 PRIu64
#define Px64 PRIx64


// Suffixes for 64-bit integer literals.
#ifdef _MSC_VER
#define DART_INT64_C(x) x##I64
#define DART_UINT64_C(x) x##UI64
#else
#define DART_INT64_C(x) x##LL
#define DART_UINT64_C(x) x##ULL
#endif


// The following macro works on both 32 and 64-bit platforms.
// Usage: instead of writing 0x1234567890123456ULL
//      write DART_2PART_UINT64_C(0x12345678,90123456);
#define DART_2PART_UINT64_C(a, b)                                              \
                 (((static_cast<uint64_t>(a) << 32) + 0x##b##u))

// Integer constants.
const int32_t kMinInt32 = 0x80000000;
const int32_t kMaxInt32 = 0x7FFFFFFF;
const uint32_t kMaxUint32 = 0xFFFFFFFF;
const int64_t kMinInt64 = DART_INT64_C(0x8000000000000000);
const int64_t kMaxInt64 = DART_INT64_C(0x7FFFFFFFFFFFFFFF);
const uint64_t kMaxUint64 = DART_2PART_UINT64_C(0xFFFFFFFF, FFFFFFFF);

// Types for native machine words. Guaranteed to be able to hold pointers and
// integers.
typedef intptr_t word;
typedef uintptr_t uword;

// Byte sizes.
const int kWordSize = sizeof(word);
const int kDoubleSize = sizeof(double);  // NOLINT
const int kFloatSize = sizeof(float);  // NOLINT
const int kSimd128Size = sizeof(simd128_value_t);  // NOLINT
#ifdef ARCH_IS_32_BIT
const int kWordSizeLog2 = 2;
const uword kUwordMax = kMaxUint32;
#else
const int kWordSizeLog2 = 3;
const uword kUwordMax = kMaxUint64;
#endif

// Bit sizes.
const int kBitsPerByte = 8;
const int kBitsPerByteLog2 = 3;
const int kBitsPerWord = kWordSize * kBitsPerByte;

// System-wide named constants.
const intptr_t KB = 1024;
const intptr_t KBLog2 = 10;
const intptr_t MB = KB * KB;
const intptr_t MBLog2 = KBLog2 + KBLog2;
const intptr_t GB = MB * KB;
const intptr_t GBLog2 = MBLog2 + KBLog2;

const intptr_t kIntptrOne = 1;
const intptr_t kIntptrMin = (kIntptrOne << (kBitsPerWord - 1));
const intptr_t kIntptrMax = ~kIntptrMin;

// Time constants.
const int kMillisecondsPerSecond = 1000;
const int kMicrosecondsPerMillisecond = 1000;
const int kMicrosecondsPerSecond = (kMicrosecondsPerMillisecond *
                                    kMillisecondsPerSecond);
const int kNanosecondsPerMicrosecond = 1000;
const int kNanosecondsPerMillisecond = (kNanosecondsPerMicrosecond *
                                        kMicrosecondsPerMillisecond);
const int kNanosecondsPerSecond = (kNanosecondsPerMicrosecond *
                                   kMicrosecondsPerSecond);

// A macro to disallow the copy constructor and operator= functions.
// This should be used in the private: declarations for a class.
#define DISALLOW_COPY_AND_ASSIGN(TypeName)                                     \
private:                                                                       \
  TypeName(const TypeName&);                                                   \
  void operator=(const TypeName&)


// A macro to disallow all the implicit constructors, namely the default
// constructor, copy constructor and operator= functions. This should be
// used in the private: declarations for a class that wants to prevent
// anyone from instantiating it. This is especially useful for classes
// containing only static methods.
#define DISALLOW_IMPLICIT_CONSTRUCTORS(TypeName)                               \
private:                                                                       \
  TypeName();                                                                  \
  DISALLOW_COPY_AND_ASSIGN(TypeName)


// Macro to disallow allocation in the C++ heap. This should be used
// in the private section for a class. Don't use UNREACHABLE here to
// avoid circular dependencies between platform/globals.h and
// platform/assert.h.
#define DISALLOW_ALLOCATION()                                                  \
public:                                                                        \
  void operator delete(void* pointer) {                                        \
    fprintf(stderr, "unreachable code\n");                                     \
    abort();                                                                   \
  }                                                                            \
private:                                                                       \
  void* operator new(size_t size);


// The USE(x) template is used to silence C++ compiler warnings issued
// for unused variables.
template <typename T>
static inline void USE(T) { }


// Use implicit_cast as a safe version of static_cast or const_cast
// for upcasting in the type hierarchy (i.e. casting a pointer to Foo
// to a pointer to SuperclassOfFoo or casting a pointer to Foo to
// a const pointer to Foo).
// When you use implicit_cast, the compiler checks that the cast is safe.
// Such explicit implicit_casts are necessary in surprisingly many
// situations where C++ demands an exact type match instead of an
// argument type convertable to a target type.
//
// The From type can be inferred, so the preferred syntax for using
// implicit_cast is the same as for static_cast etc.:
//
//   implicit_cast<ToType>(expr)
//
// implicit_cast would have been part of the C++ standard library,
// but the proposal was submitted too late.  It will probably make
// its way into the language in the future.
template<typename To, typename From>
inline To implicit_cast(From const &f) {
  return f;
}


// Use like this: down_cast<T*>(foo);
template<typename To, typename From>  // use like this: down_cast<T*>(foo);
inline To down_cast(From* f) {  // so we only accept pointers
  // Ensures that To is a sub-type of From *.  This test is here only
  // for compile-time type checking, and has no overhead in an
  // optimized build at run-time, as it will be optimized away completely.
  if (false) {
    implicit_cast<From, To>(0);
  }
  return static_cast<To>(f);
}


// The type-based aliasing rule allows the compiler to assume that
// pointers of different types (for some definition of different)
// never alias each other. Thus the following code does not work:
//
// float f = foo();
// int fbits = *(int*)(&f);
//
// The compiler 'knows' that the int pointer can't refer to f since
// the types don't match, so the compiler may cache f in a register,
// leaving random data in fbits.  Using C++ style casts makes no
// difference, however a pointer to char data is assumed to alias any
// other pointer. This is the 'memcpy exception'.
//
// The bit_cast function uses the memcpy exception to move the bits
// from a variable of one type to a variable of another type. Of
// course the end result is likely to be implementation dependent.
// Most compilers (gcc-4.2 and MSVC 2005) will completely optimize
// bit_cast away.
//
// There is an additional use for bit_cast. Recent gccs will warn when
// they see casts that may result in breakage due to the type-based
// aliasing rule. If you have checked that there is no breakage you
// can use bit_cast to cast one pointer type to another. This confuses
// gcc enough that it can no longer see that you have cast one pointer
// type to another thus avoiding the warning.
template <class D, class S>
inline D bit_cast(const S& source) {
  // Compile time assertion: sizeof(D) == sizeof(S). A compile error
  // here means your D and S have different sizes.
  DART_UNUSED typedef char VerifySizesAreEqual[sizeof(D) == sizeof(S) ? 1 : -1];

  D destination;
  // This use of memcpy is safe: source and destination cannot overlap.
  memcpy(&destination, &source, sizeof(destination));
  return destination;
}


// Similar to bit_cast, but allows copying from types of unrelated
// sizes. This method was introduced to enable the strict aliasing
// optimizations of GCC 4.4. Basically, GCC mindlessly relies on
// obscure details in the C++ standard that make reinterpret_cast
// virtually useless.
template<class D, class S>
inline D bit_copy(const S& source) {
  D destination;
  // This use of memcpy is safe: source and destination cannot overlap.
  memcpy(&destination,
         reinterpret_cast<const void*>(&source),
         sizeof(destination));
  return destination;
}


// On Windows the reentrent version of strtok is called
// strtok_s. Unify on the posix name strtok_r.
#if defined(TARGET_OS_WINDOWS)
#define snprintf _snprintf
#define strtok_r strtok_s
#endif

#if !defined(TARGET_OS_WINDOWS)
#if !defined(TEMP_FAILURE_RETRY)
// TEMP_FAILURE_RETRY is defined in unistd.h on some platforms. The
// definition below is copied from Linux and adapted to avoid lint
// errors (type long int changed to int64_t and do/while split on
// separate lines with body in {}s).
#define TEMP_FAILURE_RETRY(expression)                                         \
    ({ int64_t __result;                                                       \
       do {                                                                    \
         __result = static_cast<int64_t>(expression);                          \
       } while (__result == -1L && errno == EINTR);                            \
       __result; })
#endif  // !defined(TEMP_FAILURE_RETRY)

// This is a version of TEMP_FAILURE_RETRY which does not use the value
// returned from the expression.
#define VOID_TEMP_FAILURE_RETRY(expression)                                    \
    (static_cast<void>(TEMP_FAILURE_RETRY(expression)))

#endif  // !defined(TARGET_OS_WINDOWS)

#if defined(TARGET_OS_LINUX) || defined(TARGET_OS_MACOS)
// Tell the compiler to do printf format string checking if the
// compiler supports it; see the 'format' attribute in
// <http://gcc.gnu.org/onlinedocs/gcc-4.3.0/gcc/Function-Attributes.html>.
//
// N.B.: As the GCC manual states, "[s]ince non-static C++ methods
// have an implicit 'this' argument, the arguments of such methods
// should be counted from two, not one."
#define PRINTF_ATTRIBUTE(string_index, first_to_check) \
  __attribute__((__format__(__printf__, string_index, first_to_check)))
#else
#define PRINTF_ATTRIBUTE(string_index, first_to_check)
#endif

#endif  // PLATFORM_GLOBALS_H_
