// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('dns');
#import('node.dart');
#import('nodeimpl.dart');

// module dns

typedef void DnsLookupCallback(Error err, String address, int family);
typedef void DnsResolveCallback(Error err, List addresses);
typedef void DnsResolve4Callback(Error err, List<String> addresses);
typedef void DnsResolve6Callback(Error err, List<String> addresses);
typedef void DnsResolveMxCallback(Error err, List<Map<String,String>> addresses);
typedef void DnsResolveTxtCallback(Error err, List<String> addresses);
typedef void DnsResolveSrvCallback(Error err, List<Map<String, Object>> addresses);
typedef void DnsReverseCallback(Error err, List<String> domains);
typedef void DnsResolveNsCallback(Error err, List<String> addresses);
typedef void DnsResolveCnameCallback(Error err, List<String> addresses);

class Dns {
  final BADNAME     = 'EBADNAME';
  final BADRESP     = 'EBADRESP';
  final CONNREFUSED = 'ECONNREFUSED';
  final DESTRUCTION = 'EDESTRUCTION';
  final REFUSED     = 'EREFUSED';
  final FORMERR     = 'EFORMERR';
  final NODATA      = 'ENODATA';
  final NOMEM       = 'ENOMEM';
  final NOTFOUND    = 'ENOTFOUND';
  final NOTIMP      = 'ENOTIMP';
  final SERVFAIL    = 'ESERVFAIL';
  final TIMEOUT     = 'ETIMEOUT';
  
  var _dns;
  Dns._from(var this._dns);

  void lookup(String domain, int family, DnsLookupCallback callback)
    native "this._dns.lookup(domain, family, callback);";
   
  void resolve(String domain, String rrtype, DnsResolveCallback callback)
      => _resolve(domain, rrtype, (Error err, address)
          => callback(err, new NativeListPrimitiveElement<Object>(address)));
  void _resolve(String domain, String rrtype, void callback(Error err, var address))
    native "this._dns.resolve(domain, rrtype, callback)";

  void resolve4(String domain, DnsResolve4Callback callback)
      => _resolve4(domain, (Error err, var addresses)
          => callback(err, new NativeListPrimitiveElement<String>(addresses)));
  void _resolve4(String domain, void callback(Error err, var addresses))
    native "this._dns.resolve4(domain, callback)";

  void resolve6(String domain, DnsResolve6Callback callback)
      => _resolve6(domain, (Error err, address)
          => callback(err, new NativeListPrimitiveElement<String>(address)));
  void _resolve6(String domain, void callback(Error err, var address))
    native "this._dns.resolve6(domain, callback)";

  void resolveMx(String domain, DnsResolveMxCallback callback)
      => _resolveMx(domain, (Error err, address)
          => callback(err, new NativeList<Map<String,String>>(address, (var element)
              => new NativeMapPrimitiveValue<String>(element))));
  void _resolveMx(String domain, void callback(Error err, var address))
    native "this._dns.resolveMx(domain, callback)";

  void resolveTxt(String domain, DnsResolveTxtCallback callback)
      => _resolveTxt(domain, (Error err, address)
          => callback(err, new NativeListPrimitiveElement<String>(address)));
  void _resolveTxt(String domain, void callback(Error err, var address))
    native "this._dns.resolveTxt(domain, callback)";

  void resolveSrv(String domain, DnsResolveSrvCallback callback)
      => _resolveSrv(domain, (Error err, address)
          => callback(err, new NativeList<Map<String,Object>>(address, (var element)
              => new NativeMapPrimitiveValue<Object>(element))));
  void _resolveSrv(String domain, void callback(Error err, var address))
    native "this._dns.resolveSrv(domain, callback)";

  void reverse(String ip, DnsReverseCallback callback)
      => _reverse(ip, (Error err, domains)
          => callback(err, new NativeListPrimitiveElement<String>(domains)));
  void _reverse(String ip, void callback(Error err, var domains))
    native "this._dns.reverse(ip, callback)";

  void resolveNs(String domain, DnsResolveNsCallback callback)
      => _resolveNs(domain, (Error err, address)
          => callback(err, new NativeListPrimitiveElement<String>(address)));
  void _resolveNs(String domain, void callback(Error err, var address))
    native "this._dns.resolveNs(domain, callback)";

  void resolveCname(String domain, DnsResolveCnameCallback callback)
      => _resolveCname(domain, (Error err, address)
          => callback(err, new NativeListPrimitiveElement<String>(address)));
  void _resolveCname(String domain, void callback(Error err, var address))
    native "this._dns.resolveCname(domain, callback)";

}

Dns get dns() => new Dns._from(require('dns'));
