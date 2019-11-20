import 'package:flutter_bridge/flutter_bridge.dart';

part 'usermanager2.g.dart';

@FlutterBridge()
class UserManager2 {

  UserManager2();

  @Expose("user")
  Future<String> getUserName(int age) async {
    return "florent $age";
  }

  bind(channel) => Bridge_UserManager2(channel).bind(this);

}
