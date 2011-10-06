// Copyright (c) 2011, the Dart project authors.  Please see the AUTHORS file
// for details. All rights reserved. Use of this source code is governed by a
// BSD-style license that can be found in the LICENSE file.

class ContinuationPair {
  final Success sk;
  final Failure fk;
  const ContinuationPair(this.sk, this.fk);
}

// TunnelMsg objects do not exist in caps.js. However, creating
// lightweight records is a pain. JavaScript's { x: 1, y : 2}.x
// syntax is much lighter than { 'x': 1, 'y': 2 }['x']. However,
// I appreciate the clear spec. of our protocol.
class TunnelMsg {

  abstract Map toJSON();

  TunnelMsg() { }

  static TunnelMsg fromJSON(Map msg) {
    switch (msg['op']) {
      case 'invoke':
        return new InvokeMsg(msg['txID'], msg['ser'], msg['method'], msg['data']);
      case 'response':
        return new ResponseMsg(msg['txID'], msg['type'], msg['data']);
      case 'outpost': return new OutpostMsg(msg['outpostData']);
      case 'ping': return new PingMsg();
      case 'pong': return new PongMsg();
      default: assert(false); break;
    }
  }

  abstract void respond(BcapTunnelImpl tunnel);
}

class InvokeMsg extends TunnelMsg {
  final int txID;
  final String ser;
  final String method;
  final String data;

  InvokeMsg(this.txID, this.ser, this.method, this.data) : super() { }

  Map toJSON() =>  { 'op': 'invoke', 'txID': txID, 'ser': ser, 'method': method, 'data': data };

  void _sendReply(BcapTunnelImpl tunnel, String type, var data) {
    TunnelMsg reply = new ResponseMsg(txID, type, data);
    tunnel.port.postMessage(reply.toJSON());
  }
  
  void respond(BcapTunnelImpl tunnel) {
    BcapServerInterface sendTo =
      tunnel.resolver(BcapUtil.decodeInstID(ser));
    if (sendTo != null) {
      sendTo.invoke(ser, method, data,
        void _(d) { _sendReply(tunnel, 'success', d); },
        void _(e) { _sendReply(tunnel, 'failure', e); }
        );
    } else {
      Map data;
      data['status'] = BcapError.notFound.status;
      data['message'] = BcapError.notFound.message;
      _sendReply(tunnel, 'failure', data);
    }
  }
}

class ResponseMsg extends TunnelMsg {
  final int txID;
  final String type;
  final data;
  ResponseMsg(this.txID, this.type, this.data) : super() { }

  Map toJSON() =>  { 'op': 'response', 'type': type, 'txID': txID, 'data': data };

  bool isSuccess() => type == "success";

  void respond(BcapTunnelImpl tunnel) {
    if (!tunnel.transactions.containsKey(txID)) {
      return;
    }

    ContinuationPair cp = tunnel.transactions[txID];
    tunnel.transactions.remove(txID);
    if (isSuccess()) {
      cp.sk(data);
    } else {
      Map err = data;
      cp.fk(new BcapError(err['status'], err['message']));
    }
  }
}


class OutpostMsg extends TunnelMsg {
  final outpostData;
  
  OutpostMsg(this.outpostData) : super() { }

  Map toJSON() => { 'op': 'outpost', 'outpostData': outpostData };
  
  void respond(BcapTunnelImpl tunnel) {
    if (tunnel.outpostHandler !== null) {
      tunnel.outpostHandler(outpostData);      
    }
  }
}

class PingMsg extends TunnelMsg {

  PingMsg() : super() { }

  Map toJSON() => { 'op': 'ping' };
  
  void respond(BcapTunnelImpl tunnel) {
    tunnel.port.postMessage((new PongMsg()).toJSON());
  }
}

class PongMsg extends TunnelMsg {

  PongMsg() : super() { }

  Map toJSON() =>  { 'op': 'pong' };
  
  void respond(BcapTunnelImpl tunnel) {
    // nothing to do
  }
}

interface BcapTunnel extends BcapServerInterface factory BcapTunnelImpl {
  
  void sendOutpost(var msg);

  BcapTunnel(BelayPort port, Resolver resolver, Function outpostHandler);
}

typedef void OutpostHandler(Map outpostData);

class BcapTunnelImpl implements BcapServerInterface {

  BelayPort port;
  int nextTxId;
  Map<int, ContinuationPair> transactions;
  OutpostHandler outpostHandler;
  Resolver resolver;
  
  void invoke(String ser, String method, String data,
              SuccessI ski, FailureI fki) {
    var txID = nextTxId++;
    var msg = new InvokeMsg(txID, ser, method, data);

    transactions[txID] = new ContinuationPair(ski, fki);
    port.postMessage(msg.toJSON());
  }

  BcapTunnelImpl(this.port, this.resolver, [this.outpostHandler = null]) {
    nextTxId = 0;
    transactions = new Map<int, ContinuationPair>();

    port.setOnMessage(void _(rawMsg) {
      TunnelMsg.fromJSON(rawMsg).respond(this);
    });
  }

  void sendOutpost(data) {
    port.postMessage((new OutpostMsg(data)).toJSON());
  }
}



