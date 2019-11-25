import 'package:wormhole/wormhole.dart';
import 'dart:async';

part 'usermanager2.g.dart';

@WormHole()
class UserManager2 {

  UserManager2();

  @Expose("user")
  Future<String> getUserName(int age) async {
    return "florent $age";
  }

  bind(channel) => Expose$UserManager2(channel).expose(this);

}
