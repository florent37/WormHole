import 'package:flutter/material.dart';
import 'package:flutter_bridge_example/usermanager/usermanager2.dart';
import 'package:flutter_bridge_example/userscreen/userscreen.dart';

void main() => runApp(MyApp());

class MyApp extends StatefulWidget {
  @override
  _MyAppState createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {

  final UserManager2 userManager2 = UserManager2().bind("user2");

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('Plugin example app'),
        ),
        body: UserScreen()
      ),
    );
  }
}
