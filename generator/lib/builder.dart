import 'package:build/build.dart';
import 'src/generator.dart';

Builder flutterBridgeBuilder(BuilderOptions options) =>
    generatorFactoryBuilder(options);
