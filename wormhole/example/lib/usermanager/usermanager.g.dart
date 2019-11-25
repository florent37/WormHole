// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'usermanager.dart';

// **************************************************************************
// FlutterWormHoleGenerator
// **************************************************************************

class Retrieve$UserManager implements UserManager {
  Retrieve$UserManager(this.channelName) {
    this._bridge = flutterBridge().findOrCreate(channelName);
    this._bridge.addMethodCallHandler((MethodCall call) async {
      final name = call.method;
      final arg = call.arguments;
      _waiters[name]?.invoke(arg);
    });
  }

  BridgeManager _bridge;

  final String channelName;

  final Map<String, PlatformWaiter> _waiters = Map<String, PlatformWaiter>();

  @override
  Stream<User> getUser() {
    final $eventChannel$$ =
        flutterBridge().findOrCreateEvent("${this.channelName}/getUser");
    try {
      return $eventChannel$$.getBroadcastSubscription().map(($input$$) {
        return User.fromJson(Map<String, dynamic>.from($input$$));
      });
    } catch (e) {
      print("error while calling getUser");
      print(e);
      return Stream.empty();
    }
  }

  @override
  Future<void> saveUser(User user) async {
    await _bridge.invokeMethod("saveUser", user.toJson());
  }

  @override
  void clear() async {
    _bridge.invokeMethod("clear");
  }
}
