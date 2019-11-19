export 'platform_annotations.dart';
export 'package:flutter/services.dart'; //for MethodChannel import
import 'package:flutter/services.dart';

class FlutterBridgeManager {

  FlutterBridgeManager._();

  final bridgesManagers = Map<String, BridgeManager>();
  final bridgesEventManagers = Map<String, BridgeEventManager>();

  BridgeManager findOrCreate(String channelName) {
    final bridgeManager = bridgesManagers[channelName];
    if (bridgeManager != null) {
      return bridgeManager;
    } else {
      final newBridgeManager = BridgeManager.named(channelName);
      bridgesManagers[channelName] = newBridgeManager;
      return newBridgeManager;
    }
  }

  BridgeEventManager findOrCreateEvent(String channelName) {
    final bridgeManager = bridgesEventManagers[channelName];
    if (bridgeManager != null) {
      return bridgeManager;
    } else {
      final newBridgeManager = BridgeEventManager.named(channelName);
      bridgesEventManagers[channelName] = newBridgeManager;
      return newBridgeManager;
    }
  }

}

final FlutterBridgeManager FlutterBridgeInstance = FlutterBridgeManager._();

FlutterBridgeManager flutterBridge() => FlutterBridgeInstance;

typedef Future<dynamic> PlatformCallHandler(MethodCall call);

class BridgeManager {
  final MethodChannel methodChannel;

  final List<PlatformCallHandler> handlers = List();

  BridgeManager(this.methodChannel) {
    methodChannel.setMethodCallHandler(handler);
  }

  BridgeManager.named(String methodName) : this(MethodChannel(methodName));

  void addMethodCallHandler(PlatformCallHandler handler) {
    handlers.add(handler);
  }

  Future<dynamic> invokeMethod(String method, [ dynamic arguments ]) => methodChannel.invokeMethod(method, arguments);

  Future<dynamic> handler(MethodCall call) async {
    final List<Future<dynamic>> returnFutures = handlers.map((h) {
      return h(call);
    }).toList();

    final valueToReturn =  await waitAllAndReturnFirst(returnFutures);
    return valueToReturn;
  }
}

typedef Future<dynamic> PlatformEventHandler(MethodCall call);

class BridgeEventManager {
  final EventChannel _eventChannel;

  Stream _broadcastSubscription;

  BridgeEventManager(this._eventChannel);

  BridgeEventManager.named(String methodName) : this(EventChannel(methodName));

  Stream getBroadcastSubscription([ dynamic arguments ]) {
    if(_broadcastSubscription == null) {
      _broadcastSubscription = _eventChannel.receiveBroadcastStream(arguments);
    }
    return _broadcastSubscription;
  }
}

Future<dynamic> waitAllAndReturnFirst(List<Future<dynamic>> futures) async {
  final List<dynamic> returnValues = List();
  for (var i = 0; i < futures.length; ++i) {
    final returnFuture = futures[i];
    if (returnFuture != null) {
      dynamic value = await returnFuture;
      if(value != null) {
        returnValues.add(value);
      }
    }
  }

  if (returnValues.isNotEmpty) {
    return returnValues.first;
  } else {
    return null;
  }
}

dynamic firstNotNull(List<dynamic> list) {
  return list.where((i) => i != null).toList()?.first;
}