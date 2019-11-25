
import 'package:wormhole/wormhole.dart';
import 'package:wormhole_example/usermanager/user.dart';

part 'usermanager.g.dart';

@WormHole()
abstract class UserManager {

  @Call("getUser")
  Stream<User> getUser();

  @Call("saveUser")
  Future<void> saveUser(User user);

  @Call("clear")
  void clear();

  factory UserManager(String channelName) => Retrieve$UserManager(channelName);
}