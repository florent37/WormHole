import 'package:flutter/material.dart';
import 'package:wormhole_example/usermanager/user.dart';
import 'package:wormhole_example/usermanager/usermanager.dart';

class UserScreen extends StatefulWidget {
  @override
  _UserScreenState createState() => _UserScreenState();
}

class _UserScreenState extends State<UserScreen> {
  final UserManager userManager = UserManager("user");

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
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: <Widget>[
        Card(
          margin: EdgeInsets.symmetric(vertical: 8, horizontal: 8),
          child: Padding(
            padding: const EdgeInsets.all(8.0),
            child: Column(
              children: <Widget>[
                Text("Create an user", style: TextStyle(color: Colors.grey[800], fontWeight: FontWeight.w700, fontSize: 18)),
                Row(children: <Widget>[
                  Text("Username :", style: TextStyle(color: Colors.grey[800], fontWeight: FontWeight.w700, fontSize: 16)),
                  SizedBox(
                    width: 12,
                  ),
                  Expanded(
                    child: TextField(
                      decoration: InputDecoration(hintText: "Username"),
                      controller: _textEditingController,
                    ),
                  ),
                ]),
                Row(children: <Widget>[
                  Text("Age :", style: TextStyle(color: Colors.grey[800], fontWeight: FontWeight.w700, fontSize: 16)),
                  SizedBox(
                    width: 12,
                  ),
                  Flexible(
                    child: DropdownButton(
                        onChanged: (value) {
                          setState(() {
                            _selectedAge = value;
                          });
                        },
                        value: _selectedAge,
                        items: [18, 25, 30].map((age) => DropdownMenuItem(value: age, child: Text(age.toString()))).toList()),
                  ),
                ]),
                RaisedButton(
                  color: Theme.of(context).accentColor,
                  child: Text(
                    "save",
                    style: Theme.of(context).accentTextTheme.button,
                  ),
                  onPressed: () {
                    _saveUser();
                  },
                )
              ],
            ),
          ),
        ),
        Card(
          margin: EdgeInsets.symmetric(vertical: 8, horizontal: 8),
          child: StreamBuilder(
            stream: userManager.getUser(),
            builder: (context, snapshot) {
              if (snapshot.hasData && snapshot.data != null) {
                final _user = snapshot.data;
                return Padding(
                  padding: const EdgeInsets.only(top: 8.0, left: 8.0, right: 8.0, bottom: 12.0),
                  child: Column(
                    children: <Widget>[
                      Text("Loaded user from native", style: TextStyle(color: Colors.grey[800], fontWeight: FontWeight.w700, fontSize: 18)),
                      SizedBox(height: 12,),
                      Row(children: <Widget>[
                        Text("Username :", style: TextStyle(color: Colors.grey[800], fontWeight: FontWeight.w700, fontSize: 16)),
                        SizedBox(
                          width: 12,
                        ),
                        Text(_user.name),
                      ],),
                      SizedBox(height: 12,),
                      Row(children: <Widget>[
                        Text("Age :", style: TextStyle(color: Colors.grey[800], fontWeight: FontWeight.w700, fontSize: 16)),
                        SizedBox(
                          width: 12,
                        ),
                        Text(_user.age.toString()),
                      ],)
                    ],
                  ),
                );
              } else {
                return CircularProgressIndicator();
              }
            },
          ),
        )
      ],
    );
  }
}
