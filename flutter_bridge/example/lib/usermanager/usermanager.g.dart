// GENERATED CODE - DO NOT MODIFY BY HAND

part of 'usermanager.dart';

// **************************************************************************
// FlutterBridgeGenerator
// **************************************************************************

class _UserManager implements UserManager {
  _UserManager(this.channelName) {
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
  Future<User> getUser() async {
    try {
      final _$_toReturn = await _bridge.invokeMethod("getUser");
      if (_$_toReturn is Map<dynamic, dynamic>) {
        return User.fromJson(Map<String, dynamic>.from(_$_toReturn));
      }
    } catch (e) {
      print("error while calling getUser");
      print(e);
    }
    return null;
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
