
import 'package:flutter_bridge/flutter_bridge.dart';
import 'package:flutter_bridge_example/usermanager/user.dart';

part 'usermanager.g.dart';

@FlutterBridge()
abstract class UserManager {

  @Call("getUser")
  Future<User> getUser();

  @Call("saveUser")
  Future<void> saveUser(User user);

  @Call("clear")
  void clear();

  factory UserManager(String channelName) => _UserManager(channelName);
}