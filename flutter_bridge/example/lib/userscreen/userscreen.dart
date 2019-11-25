import 'package:flutter/material.dart';
import 'package:wormhole_example/usermanager/user.dart';
import 'package:wormhole_example/usermanager/usermanager.dart';

class UserScreen extends StatefulWidget {
  @override
  _UserScreenState createState() => _UserScreenState();
}

class _UserScreenState extends State<UserScreen> {
  final userManager = UserManager("user");

  TextEditingController _textEditingController = TextEditingController();
  int _selectedAge = 18;

  _saveUser() async {
    if (_textEditingController.text.isNotEmpty && _selectedAge != null) {
      userManager.saveUser(User(
        name: _textEditingController.text,
        age: _selectedAge,
      ));
    }
  }

  @override
  Widget build(BuildContext context) {
    return Column(
      children: <Widget>[
        TextField(
          decoration: InputDecoration(hintText: "save an user name"),
          controller: _textEditingController,
        ),
        DropdownButton(
            onChanged: (value) {
              setState(() {
                _selectedAge = value;
              });
            },
            value: _selectedAge,
            items: [18, 25, 30].map((age) => DropdownMenuItem(value: age, child: Text(age.toString()))).toList()),
        RaisedButton(
          child: Text("save"),
          onPressed: () {
            _saveUser();
          },
        ),
        Divider(
          color: Colors.black,
          height: 1,
        ),
        StreamBuilder(
          stream: userManager.getUser(),
          builder: (context, snapshot) {
            if(snapshot.hasData && snapshot.data != null){
              final _user = snapshot.data;
              return Column(
                children: <Widget>[
                  Text("loaded user :"),
                  Text(_user.name),
                  Text("user age"),
                  Text(_user.age.toString())
                ],
              );
            } else {
              return CircularProgressIndicator();
            }
          },
        )
      ],
    );
  }
}
