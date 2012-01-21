// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

#library('NodeCryptoTest');

#import('unittest_node.dart');
#import('../../../lib/node/node.dart');
#import('../../../lib/node/crypto.dart');

void main() {
  test('Credentials', () {
    Credentials credentials = crypto.createCredentials();
  });
  
  test('Hash', () {
    {
      Hash hash = crypto.createHash('md5');
      hash.update("The quick brown fox jumps over the lazy dog");
      String digest = hash.digest('hex');
      Expect.equals('9e107d9d372bb6826bd81d3542a419d6', digest);
    }
    {
      Hash hash = crypto.createHash('md5');
      hash.updateBuffer(new Buffer.fromString(
          "The quick brown fox jumps over the lazy dog"));
      String digest = hash.digest('hex');
      Expect.equals('9e107d9d372bb6826bd81d3542a419d6', digest);
    }
  });
  
  test('Hmac', () {
    {
      Hmac hmac = crypto.createHmac('sha1', '1234567'); 
      hmac.update("hello world"); 
      String digest = hmac.digest('base64'); 
      Expect.equals('c6jmoNeWbPMlfL48Wm7HzwanzaQ=', digest);
    }
    {
      Hmac hmac = crypto.createHmac('sha1', '1234567'); 
      hmac.updateBuffer(new Buffer.fromString("hello world")); 
      String digest = hmac.digest('base64'); 
      Expect.equals('c6jmoNeWbPMlfL48Wm7HzwanzaQ=', digest);
    }
  });
  
  test('Cipher / Decipher', () {
    String algorithm = 'aes-256-cbc';
    String password = 'password';
    String plaintext = 'This is a test.';
    StringBuffer enciphered = new StringBuffer();
    {
      Cipher cipher = crypto.createCipher(algorithm, password);
      enciphered.add(cipher.update(plaintext));
      enciphered.add(cipher.finalData());
    }
    StringBuffer deciphered = new StringBuffer();
    {
      Decipher decipher = crypto.createDecipher(algorithm, password);
      decipher.update(enciphered.toString());
      deciphered.add(decipher.finalData());
    }
    Expect.equals(plaintext, deciphered.toString());
  });
  
  test('Signer / Verifier', () {
    // Dummy certificate created with: openssl req -x509 -nodes \
    // -days 36500 -subj '/C=US/ST=California/L=MountainView/CN=www.example.com' \
    // -newkey rsa:1024 -keyout mykey.pem -out mycert.pem
    
    String privateKey =
"""
-----BEGIN PRIVATE KEY-----
MIICdgIBADANBgkqhkiG9w0BAQEFAASCAmAwggJcAgEAAoGBAKUnAuAX/cMhPm8O
Q5cLiXqR/15sZ38ab+jqi2+aRP63pTbA1Utpl9nd5IM+Sgf2HJ94uqSyxV3jGBBH
HBxQ4Y/+LL2S6VnKolEStnlP5lQa/yKRnvLFUX+4qIgdyXln+iFNmO6R/OgdHfBb
TVr7883hLavjnrTVeRgske2Zxw+ZAgMBAAECgYAacyUCvtTla22UW7R3fGGIP9mm
RbZNpO5HX0j1lr92C/Np0XhXm5G2UtNGMbOpksECyVMbDYaOgtBXywu1fT6ijzNL
if/c4vUW+OC6agV59Wl+tX/AnNK7xDO/i0aWklnOrUmHG73wrSMLdvJyLeVkK3jx
j3QbWcuLgH0E87BQAQJBANFARIPxCQEvfCItvu1ULAMNYwWFIFecPkj0YKXuYYeR
YDcZUaDQBoi1aI9HI06eJMYLNELMhcr8NT/0C8h5dg0CQQDKDJnnquEbvegccZ+W
FfZEWQ/2SOR2mh6JFNfh2kSJYCTROab1vDAjiX3ME0NgRZw09g1G7TzALF3ypx2B
9Ii9AkEAmR4CBNpX0HpCx2/aCihRnForX1qu8+zs1s2b+0+YJm+GjEsGpDoUzeyQ
+mb/uwOVvSVttIOcU5CCFq4qASR/8QJAZgslM67G0CcCclMkYT2oSe6dNCquUAQY
he0j9uowkR0gmxa97v/jZB9NjGLyNU4SzWCzZe3tL7V4oVOrgHXLFQJAbRiYs0U9
6R3qWQfekNgJ9bqbq0Hc7SLQthmHPdA5thVZNycP2IEJkVNzgqyBjSYqFD+kXisr
Y92xR3J1qaPAdg==
-----END PRIVATE KEY-----
""";
    
    String cert =
"""
-----BEGIN CERTIFICATE-----
MIICdjCCAd+gAwIBAgIJAItQw3IRpdUeMA0GCSqGSIb3DQEBBQUAMFMxCzAJBgNV
BAYTAlVTMRMwEQYDVQQIDApDYWxpZm9ybmlhMRUwEwYDVQQHDAxNb3VudGFpblZp
ZXcxGDAWBgNVBAMMD3d3dy5leGFtcGxlLmNvbTAgFw0xMjAxMDIwMTQ5NDlaGA8y
MTExMTIwOTAxNDk0OVowUzELMAkGA1UEBhMCVVMxEzARBgNVBAgMCkNhbGlmb3Ju
aWExFTATBgNVBAcMDE1vdW50YWluVmlldzEYMBYGA1UEAwwPd3d3LmV4YW1wbGUu
Y29tMIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQClJwLgF/3DIT5vDkOXC4l6
kf9ebGd/Gm/o6otvmkT+t6U2wNVLaZfZ3eSDPkoH9hyfeLqkssVd4xgQRxwcUOGP
/iy9kulZyqJRErZ5T+ZUGv8ikZ7yxVF/uKiIHcl5Z/ohTZjukfzoHR3wW01a+/PN
4S2r45601XkYLJHtmccPmQIDAQABo1AwTjAdBgNVHQ4EFgQU2AQF3BvY+8hKXlQt
zbtLORu57/AwHwYDVR0jBBgwFoAU2AQF3BvY+8hKXlQtzbtLORu57/AwDAYDVR0T
BAUwAwEB/zANBgkqhkiG9w0BAQUFAAOBgQCRmCqt8gn0VnfwD1+2zMLXtMFovEnY
Ywncm+7FeUmtgkn92chKpaCgd5SZ9ca2/CfwsdORG5BFzDTZavfM4Mas+lk9CG4V
8jmYLiSosRlbLoA17erYHOSxbTJROqWxvdznub1aoDHmr/cV4OoIUEfyX2IT4KgF
4/bg0eByfq9n7Q==
-----END CERTIFICATE-----
""";
    
    String document = 'I am a document.';
    Signer signer = crypto.createSign('RSA-SHA256');
    signer.update(document);
    String signature = signer.sign(privateKey);

    Verifier verifier = crypto.createVerify('RSA-SHA256');
    verifier.update(document);
    Expect.equals(true, verifier.verify(cert, signature));
  });
  
  test('DiffieHellman', () {
    DiffieHellman server = crypto.createDiffieHellman(512);
    String prime = server.getPrime();
    // sharing secret key on a pair                                                 
    DiffieHellman alice = crypto.createDiffieHellmanFromPrime(prime);
    alice.generateKeys();
    String alicePub = alice.getPublicKey();

    DiffieHellman bob = crypto.createDiffieHellmanFromPrime(prime);
    bob.generateKeys();
    String bobPub = bob.getPublicKey();

    String bobAliceSecret = bob.computeSecret(alicePub);
    String aliceBobSecret = alice.computeSecret(bobPub);

    // public keys are different, but secret is common.                             
    Expect.equals(false, bobPub == alicePub);
    Expect.equals(true, bobAliceSecret == aliceBobSecret);
  });
  
  asyncTest('pbkdf2', 1, () {
    String password = 'password';
    String salt = 'salt';
    int iterations = 5;
    int keylen = 256;
    crypto.pbkdf2(password, salt, iterations, keylen,
      (Error err, String derivedKey1) {
        crypto.pbkdf2(password, salt, iterations, keylen,
          (Error err, String derivedKey2) {
            Expect.equals(derivedKey1, derivedKey2);
            callbackDone();
          });
      });
  });
  
  group('rand', () {
    test('sync', (){
        SlowBuffer buf = crypto.randomBytes(12);
        Expect.equals(12, buf.length);
    });
    asyncTest('async', 1, (){
        crypto.randomBytes(12, (Error err, SlowBuffer buf) {
        Expect.equals(12, buf.length);
        callbackDone();     
       });
    });
  });
}