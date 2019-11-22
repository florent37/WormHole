import 'package:analyzer/dart/element/element.dart';
import 'package:analyzer/dart/element/type.dart';
import 'package:build/build.dart';
import 'package:built_collection/built_collection.dart';
import 'package:code_builder/code_builder.dart';
import 'package:dart_style/dart_style.dart';
import 'package:flutter_bridge/platform_annotations.dart' as flutter_bridge;
import 'package:flutter_bridge_generator/src/generator_helper.dart';
import 'package:source_gen/source_gen.dart';

class FlutterBridgeGenerator extends GeneratorForAnnotation<flutter_bridge.FlutterBridge> {
  static const _flutterBridge = "flutterBridge";
  static const _bridge = "_bridge";
  static const _waiters = "_waiters";
  static const _PlatformWaiter = "PlatformWaiter";
  static const _channelName = "channelName";
  static const _eventChannel = "\$eventChannel\$\$";
  static const _handleMessage = "\$bridge\$handleMessage";
  static const _target = "_target";
  static const _toReturn = "\$toReturn\$\$";
  static const _input = "\$input\$\$";

  bool _showLogs = true;

  FlutterBridgeGenerator() {}

  String generateForAnnotatedElement(Element element, ConstantReader annotation, BuildStep buildStep) {
    if (element is! ClassElement) {
      final name = element.displayName;
      throw new InvalidGenerationSourceError(
        'Generator cannot target `$name`.',
        todo: 'Remove the [RestApi] annotation from `$name`.',
      );
    }
    return _implementClass(element, annotation);
  }

  String _implementClass(ClassElement element, ConstantReader annotation) {
    final className = element.name;

    final annotatedMethods = _findAnnotatedMethods(element);
    final bindAnnotatedMethods = _findBindMethods(annotatedMethods);

    final classBuilder = new Class((c) {
      final List<FieldElement> injectFields = _findInjectFields(element.fields);

      if (bindAnnotatedMethods.isNotEmpty) {
        c
          ..name = 'Bridge\$$className'
          ..constructors.addAll([_generateBindConstructor()])
          ..fields.addAll([_buildBridgeFiled(), _buildChannelNameFiled(), _builTargetFiled(element)])
          ..methods.addAll(_generateBindMethods(element, bindAnnotatedMethods, injectFields));
      } else {
        if (element.isAbstract) {
          c
            ..name = '\$$className'
            ..constructors.addAll([_generateFromToConstructor()])
            ..fields.addAll([_buildBridgeFiled(), _buildChannelNameFiled(), _buildWaiterslFiled()])
            ..implements = ListBuilder([refer(className)])
            ..methods.addAll(_parseFromToMethods(element, annotatedMethods));
        } else {
          throw new InvalidGenerationSourceError(
            'Error generating binding for `$className`. @WaitPlatform and @CallPlatform should only be used on abstract classes',
            todo: '',
          );
        }
      }
    });

    final emitter = new DartEmitter();
    final geneatedCode = '${classBuilder.accept(emitter)}';
    if (_showLogs) {
      print(geneatedCode);
    }

    return new DartFormatter().format(geneatedCode);
  }

  Field _buildBridgeFiled() => Field((m) => m
    ..name = _bridge
    ..type = refer("BridgeManager"));

  Field _buildChannelNameFiled() => Field((m) => m
    ..name = _channelName
    ..type = refer("String")
    ..modifier = FieldModifier.final$);

  Field _builTargetFiled(ClassElement element) => Field((m) => m
    ..name = _target
    ..type = refer(element.type.toString()));

  Field _buildWaiterslFiled() => Field((m) => m
    ..name = _waiters
    ..type = refer("Map<String, $_PlatformWaiter>")
    ..modifier = FieldModifier.final$
    ..assignment = Code("Map<String, $_PlatformWaiter>()"));

  Constructor _generateFromToConstructor() => Constructor((c) {
        c.requiredParameters.add(Parameter((p) => p
          ..name = _channelName
          ..toThis = true));
        c.body = Block.of([
          Code("this.$_bridge = $_flutterBridge().findOrCreate($_channelName);"),
          Code("this.$_bridge.addMethodCallHandler((MethodCall call) async {"),
          Code("  final name = call.method;"),
          Code("  final arg = call.arguments;"),
          Code("  $_waiters[name]?.invoke(arg);"),
          Code("});"),
        ]);
      });

  Code _generateInjections(List<FieldElement> injectFields) {
    return Block.of(injectFields.map((field) {
      String channelName = getAnnotationName(getFieldAnnotation(field, [flutter_bridge.InjectBinding]));
      if (channelName == null) {
        return Code("$_target.${field.name} = ${field.type.toString()}($_channelName);");
      } else {
        return Code("$_target.${field.name} = ${field.type.toString()}(\"$channelName\");");
      }
    }));
  }

  Constructor _generateBindConstructor() => Constructor((c) {
        c.requiredParameters.add(Parameter((p) => p
          ..name = _channelName
          ..toThis = true));
        c.body = Code("""
            this.$_bridge = $_flutterBridge().findOrCreate($_channelName);
            this.$_bridge.addMethodCallHandler($_handleMessage);
        """);
      });

  List<MethodElement> _findAnnotatedMethods(ClassElement element) {
    return element.methods.where((MethodElement m) {
      final methodAnnot = getMethodAnnotation(m, _methodsAnnotations);
      return methodAnnot != null;
    }).toList();
  }

  List<MethodElement> _findBindMethods(List<MethodElement> annotatedMethods) {
    return annotatedMethods.where((MethodElement m) {
      final ConstantReader annotation = getMethodAnnotation(m, _methodsAnnotations);
      final direction = getAnnotationDirection(annotation);
      return direction == flutter_bridge.FlutterPlatformDirection.BIND;
    }).toList();
  }

  List<FieldElement> _findInjectFields(List<FieldElement> fields) {
    return fields.where((FieldElement m) {
      final ConstantReader annotation = getFieldAnnotation(m, [flutter_bridge.InjectBinding]);
      return annotation != null;
    }).toList();
  }

  List<MethodElement> _findToMethods(List<MethodElement> annotatedMethods) {
    return annotatedMethods.where((MethodElement m) {
      final ConstantReader annotation = getMethodAnnotation(m, _methodsAnnotations);
      final direction = getAnnotationDirection(annotation);
      return direction == flutter_bridge.FlutterPlatformDirection.TO;
    }).toList();
  }

  List<MethodElement> _findFromMethods(List<MethodElement> annotatedMethods) {
    return annotatedMethods.where((MethodElement m) {
      final ConstantReader annotation = getMethodAnnotation(m, _methodsAnnotations);
      final direction = getAnnotationDirection(annotation);
      return direction == flutter_bridge.FlutterPlatformDirection.FROM;
    }).toList();
  }

  List<Method> _parseFromToMethods(ClassElement element, List<MethodElement> annotatedMethods) {
    final List<Method> generatedMethods = List();

    //TO
    _findToMethods(annotatedMethods).forEach((m) {
      final ConstantReader annotation = getMethodAnnotation(m, _methodsAnnotations);
      final generatedMethod = _generateToMethod(m, annotation);
      generatedMethods.add(generatedMethod);
    });

    //FROM
    _findFromMethods(annotatedMethods).forEach((m) {
      final ConstantReader annotation = getMethodAnnotation(m, _methodsAnnotations);
      final generatedMethod = _generateFromMethod(m, annotation);
      generatedMethods.add(generatedMethod);
    });

    return generatedMethods;
  }

  final _methodsAnnotations = const [
    flutter_bridge.Wait,
    flutter_bridge.Call,
    flutter_bridge.Expose,
  ];

  String getAnnotationDirection(ConstantReader annotation) {
    if (annotation != null) {
      try {
        return annotation.peek("direction").stringValue;
      } catch (e) {}
    }
    return null;
  }

  String getAnnotationName(ConstantReader annotation) {
    if (annotation != null) {
      try {
        return annotation.peek("name").stringValue;
      } catch (e) {}
    }
    return null;
  }

  String getAnnotationChannelName(ConstantReader annotation) {
    if (annotation != null) {
      try {
        return annotation.peek("channelName").stringValue;
      } catch (e) {}
    }
    return null;
  }

  Method _generateToMethod(MethodElement m, ConstantReader annotation) {
    if (_showLogs) {
      print("parsing method : ${m.name}");
      print("parsing annotation : ${annotation}");
    }

    final methodName = getAnnotationName(annotation);

    var returnType;
    if (m.returnType.isDartAsyncFuture) {
      returnType = refer(futureOf(getResponseType(m.returnType)));
    } else if (isStream(m.returnType)) {
      returnType = refer(m.returnType.toString());
    } else if (m.returnType.isVoid) {
      returnType = refer(m.returnType.toString());
    } else {
      throw new InvalidGenerationSourceError(
        'CallPlatform must return a void or Future',
        todo: 'change the return type of your method (Future<YourModel> or void)',
      );
    }

    return Method((mm) {
      mm
        ..name = m.displayName
        ..returns = returnType
        ..modifier = isStream(m.returnType) ? null : MethodModifier.async
        ..annotations = ListBuilder([CodeExpression(Code('override'))]);

      /// required parameters
      mm.requiredParameters.addAll(m.parameters.where((it) => it.isRequiredPositional || it.isRequiredNamed).map((it) => Parameter((p) => p
        ..type = refer(it.type.toString())
        ..name = it.name
        ..named = it.isNamed)));

      /// optional positional or named parameters
      mm.optionalParameters.addAll(m.parameters.where((i) => i.isOptional).map((it) => Parameter((p) => p
        ..name = it.name
        ..named = it.isNamed
        ..defaultTo = it.defaultValueCode == null ? null : Code(it.defaultValueCode))));
      mm.body = _generateToBody(m, methodName);
    });
  }

  Method _generateFromMethod(MethodElement m, ConstantReader annotation) {
    if (_showLogs) {
      print("parsing method : ${m.name}");
      print("parsing annotation : ${annotation}");
    }

    final methodName = getAnnotationName(annotation);

    if (!m.returnType.isDartAsyncFuture) {
      throw new InvalidGenerationSourceError(
        'FromFuture must return a void or Future, change the return type of your method (Future<YourModel> or void)',
        todo: '',
      );
    } else {
      final returnType = getResponseType(m.returnType);

      return Method((mm) {
        mm
          ..name = m.displayName
          ..modifier = MethodModifier.async
          ..returns = refer(futureOf(returnType))
          ..annotations = ListBuilder([CodeExpression(Code('override'))]);

        /// required parameters
        mm.requiredParameters.addAll(m.parameters.where((it) => it.isRequiredPositional || it.isRequiredNamed).map((it) => Parameter((p) => p
          ..name = it.name
          ..named = it.isNamed)));

        /// optional positional or named parameters
        mm.optionalParameters.addAll(m.parameters.where((i) => i.isOptional).map((it) => Parameter((p) => p
          ..name = it.name
          ..named = it.isNamed
          ..defaultTo = it.defaultValueCode == null ? null : Code(it.defaultValueCode))));
        mm.body = _generateFromBody(m, methodName);
      });
    }
  }

  Code _generateMapParameters(String mapName, MethodElement method) {
    final List<Code> codes = List();
    codes.add(Code("final $mapName = Map<String, dynamic>();"));

    final List<String> parametersNames = List();
    for (var i = 0; i < method.parameters.length; ++i) {
      final parameter = method.parameters[i];
      final parameterName = getAnnotationName(getParameterAnnotation(parameter, [flutter_bridge.Param])) ?? parameter.name;
      parametersNames.add(parameterName);

      print("parameter.type.name: ${parameter.type.name}");

      codes.add(Code("$mapName[\"$parameterName\"] = ${transformParameter(parameter)};"));
    }
    return Block.of(codes);
  }

  Code _generateToBody(MethodElement method, String methodName) {
    final mapName = "_\$_map";

    if (isStream(method.returnType)) {
      final List<Code> codes = List();
      codes.add(Code("final $_eventChannel = flutterBridge().findOrCreateEvent(\"\${this.channelName}/$methodName\");"));
      codes.add(Code("try {"));
      if (method.parameters.length == 0) {
        codes.add(Code("return $_eventChannel.getBroadcastSubscription().map(($_input) {"));
      } else if (method.parameters.length == 1) {
        final parameter = method.parameters[0];
        codes.add(Code("return $_eventChannel.getBroadcastSubscription(${transformParameter(parameter)}).map(($_input) {"));
      } else {
        codes.add(_generateMapParameters(mapName, method));
        codes.add(Code("return $_eventChannel.getBroadcastSubscription(${mapName}).map(($_input) {"));
      }
      codes.add(Code("  return ${getResponseInnerType(method.returnType)}.fromJson(Map<String, dynamic>.from($_input));"));
      codes.add(Code("});"));
      codes.addAll([
        Code("} catch (e) {"),
        Code("print(\"error while calling $methodName\");"),
        Code("print(e);"),
        Code("return Stream.empty();"),
        Code("}"),
      ]);

      return Block.of(codes);
    } else {
      String methodCall;
      if (method.parameters.isNotEmpty) {
        if (method.parameters.length == 1) {
          final parameter = method.parameters[0];

          methodCall = "$_bridge.invokeMethod(\"$methodName\", ${transformParameter(parameter)})";
        } else {
          methodCall = "$_bridge.invokeMethod(\"$methodName\", $mapName)";

          return Block.of([
            _generateMapParameters(mapName, method),
            generateToReturn(method, methodCall),
          ]);
        }
      } else {
        methodCall = "$_bridge.invokeMethod(\"$methodName\")";
      }

      return generateToReturn(method, methodCall);
    }
  }

  String transformParameter(ParameterElement parameter) {
    if (isPrimitiveType(parameter.type)) {
      return parameter.name;
    } else if (parameter.type.name == "List") {
      final parameterizedType = getResponseType(parameter.type);
      if (isPrimitiveType(parameterizedType)) {
        return parameter.name;
      } else {
        return "${parameter.name}.map((e) => e.toJson()).toList()";
      }
    } else if (parameter.type.name == "Map") {
      //TODO better handling of maps
      return parameter.name;
    } else {
      return "${parameter.name}.toJson()";
    }
  }

  Code _generateFromBody(MethodElement m, String methodName) {
    final returnType = getResponseType(m.returnType);
    final innerReturnType = getResponseInnerType(returnType);

    final String waiterVar = "waiter";
    final String completerVar = "completer";

    final method = Block.of([
      Code("//create the waiter"),
      Code("if($_waiters[\"${methodName}\"] == null){"),
      Code("final $waiterVar = $_PlatformWaiter<${innerReturnType}>();"),
      Code("$_waiters[\"${methodName}\"] = $waiterVar;"),
      Code("$waiterVar.converter = ($_input) {"),
      Code("    if($_input is Map<dynamic, dynamic>) {"),
      Code("        return ${innerReturnType}.fromJson(Map<String, dynamic>.from($_input));"),
      Code("    }"),
      Code("    return null;"),
      Code(" };"),
      Code("}"),
      Code("//add our futur"),
      Code("final $completerVar = Completer<${innerReturnType}>();"),
      Code("$_waiters[\"${methodName}\"].add($completerVar);"),
      Code("return $completerVar.future;")
    ]);

    return method;
  }

  List<Method> _generateBindMethods(ClassElement element, List<MethodElement> bindAnnotatedMethods, List<FieldElement> injectFields) {
    final List<Method> methods = List();

    //bindMethod
    final bindMethod = Method((mm) {
      mm
        ..name = "bind"
        ..returns = refer(element.type.toString())
        ..body = Block.of([Code("this.$_target = target;"), _generateInjections(injectFields), Code("return target;")])
        ..requiredParameters.add(Parameter((p) => p
          ..type = refer(element.type.toString())
          ..name = "target"));
    });

    methods.add(bindMethod);

    final handleMessageMethod = Method((mm) {
      mm
        ..name = "$_handleMessage"
        ..returns = refer("Future<dynamic>")
        ..modifier = MethodModifier.async
        ..requiredParameters.add(Parameter((p) => p
          ..type = refer("MethodCall")
          ..name = "call"))
        ..body = _generateBindHandleMessagesBody(bindAnnotatedMethods);
    });

    methods.add(handleMessageMethod);

    return methods;
  }

  Code _generateBindHandleMessagesBody(List<MethodElement> bindAnnotatedMethods) {
    final List<Code> codes = List();

    codes.addAll(
      [Code("final name = call.method;"), Code("final List<dynamic> returns = List();")],
    );

    for (var i = 0; i < bindAnnotatedMethods.length; ++i) {
      final MethodElement method = bindAnnotatedMethods[i];
      if (isStream(method.returnType)) {
        print("Error generating Expose for ${method.name}, Events can only be streamed from native to Flutter currently");
      } else {
        final String annotationMethodName = getAnnotationName(getMethodAnnotation(method, _methodsAnnotations));
        final String realMethodName = method.name;

        codes.addAll([Code("if(name == \"${annotationMethodName}\") {"), Code("try {")]);

        if (method.parameters.isEmpty) {
          final methodCall = "$_target.$realMethodName()";
          codes.addAll([generateBindingReturn(method, methodCall, false)]);
        } else {
          if (method.parameters.length == 1) {
            final parameter = method.parameters[0];

            // 1 parameter -> set or decode json depending on type
            final parameterType = parameter.type;

            final String parameterVariable = "\$parameter\$\$";

            String methodCall;
            if (parameter.isOptionalNamed) {
              methodCall = "$_target.$realMethodName(${parameter.name} : $parameterVariable)";
            } else {
              methodCall = "$_target.$realMethodName($parameterVariable)";
            }

            final annotationParam = getParameterAnnotation(parameter, [flutter_bridge.Param]);
            final parameterName = getAnnotationName(annotationParam) ?? parameter.name;

            codes.addAll([
              Code("if(call.arguments != null && call.arguments is ${parameterType.toString()}) {"),
              Code("final $parameterVariable = call.arguments as ${parameterType.toString()};"),
              generateBindingReturn(method, methodCall, false),
              Code("} else "),
            ]);

            codes.addAll([
              Code("if(call.arguments != null && call.arguments is Map<dynamic, dynamic> ) {"),
              Code("final $_input = Map<String, dynamic>.from(call.arguments as Map<dynamic, dynamic>);"),
            ]);
            if (isPrimitiveType(parameterType)) {
              codes.add(Code("final $parameterVariable = $_input[\"$parameterName\"] as ${parameterType.toString()};"));
            } else {
              codes.add(
                  generateJsonDecodeFromMap(type: parameterType, parameterName: parameterName, variableName: parameterVariable, mapName: "$_input", forceUseAnnotationName: annotationParam != null));
            }
            codes.addAll([generateBindingReturn(method, methodCall, false), Code("}")]);
          } else {
            //multiple parameters -> unwrap

            codes.addAll([
              Code("final $_input = Map<String, dynamic>.from(call.arguments as Map<dynamic, dynamic>);"),
              Code("if(call.arguments.length <= ${method.parameters.length}) { "),
              Code("print(\"error while calling ${method.name}, received only ${method.parameters.length} parameters, are you sure your native implementation and flutter binding are symetric ?\");"),
              Code("} else { "),
            ]);
            final parametersNames = [];
            for (var i = 0; i < method.parameters.length; ++i) {
              final parameter = method.parameters[i];
              final parameterName = getAnnotationName(getParameterAnnotation(parameter, [flutter_bridge.Param])) ?? parameter.name;

              final parameterType = parameter.type;

              final variableName = "_${parameterName}Param$i";

              parametersNames.add(variableName);

              if (isPrimitiveType(parameterType)) {
                codes.add(Code("final $variableName = $_input[\"$parameterName\"] as ${parameterType.toString()};"));
              } else {
                codes.add(generateJsonDecodeFromMap(type: parameterType, parameterName: parameterName, variableName: variableName, mapName: "$_input"));
              }
            }

            String methodCall = "$_target.$realMethodName(";
            for (var i = 0; i < parametersNames.length; ++i) {
              final parameter = method.parameters[i];
              final parametersName = parametersNames[i];
              if (i > 0) {
                methodCall += ", ";
              }

              if (parameter.isOptionalNamed) {
                methodCall += "${parameter.name} : $parametersName";
              } else {
                methodCall += "$parametersName";
              }
            }
            methodCall += ")";

            codes.add(generateBindingReturn(method, methodCall, false));
            codes.add(Code("}"));
          }
        }

        codes.addAll([
          Code("  } catch(e) {"),
          Code("    print(\"error while calling $realMethodName\");"),
          Code("    print(e);"),
          Code("  }"),
          Code("}"),
        ]);
      }
    }

    codes.add(Code("return firstNotNull(returns);"));

    return Block.of(codes);
  }

  bool isPrimitiveType(DartType type) {
    return type.isDartCoreBool || type.isDartCoreDouble || type.isDartCoreInt || type.isDartCoreString;
  }

  Code generateJsonDecodeFromMap({DartType type, String mapName, String parameterName, String variableName, bool forceUseAnnotationName = false}) {
    if (type is ParameterizedType) {
      final isList = type.toString().startsWith("List");

      //only work for lists for now

      if (isList) {
        //isList
        if (type.typeArguments.length > 0) {
          final parameterizedType = type.typeArguments[0];
          if (isPrimitiveType(parameterizedType)) {
            //parameter isPrimitive
            return Block.of([
              Code("  final $variableName = $mapName[\"$parameterName\"] as $type;"),
            ]);
          } else {
            return Block.of([
              Code(
                  "  final $variableName = ($mapName[\"$parameterName\"] as List)?.map((e) => e == null ? null : $parameterizedType.fromJson(Map<String, dynamic>.from(e as Map<dynamic, dynamic>)))?.toList();"),
            ]);
          }
        }
      }
    }

    List<Code> codes = List();

    codes.add(Code("dynamic $variableName;"));

    final fromJsonMapWithAnnotationName = Block.of([
      Code("/* look inside the map to fetch the object, using @Param */"),
      Code("if ((call.arguments as Map<dynamic, dynamic>).length >= 1) {"),
      Code("    $variableName = $type.fromJson(Map<String, dynamic>.from($mapName[\"$parameterName\"] as Map<dynamic, dynamic>));"),
      Code("} else {"),
      Code(" print(\"cannot retrieve a $type from arguments, are you sure the native implementation method send at lease one argument ?\");"),
      Code("}"),
    ]);

    if (forceUseAnnotationName) {
      codes.add(fromJsonMapWithAnnotationName);
    } else {
      codes.addAll([
        Code("try {"),
        Code("/* trying to decode the map directly to our object */"),
        Code("   $variableName = $type.fromJson(Map<String, dynamic>.from($mapName));"),
        Code("} catch (t) {"),
        fromJsonMapWithAnnotationName,
        Code("}"),
      ]);
    }

    return Block.of(codes);
  }

  Code generateBindingReturn(MethodElement method, String methodCall, bool mustReturn) {
    final List<Code> codes = List();
    final returnType = method.returnType;
    if (returnType.isVoid) {
      return Code("$methodCall;");
    } else if (returnType.isDartAsyncFuture) {
      if (returnType is ParameterizedType && returnType.typeArguments.length > 0) {
        if (isPrimitiveType(returnType.typeArguments[0])) {
          codes.addAll([
            Code("final $_toReturn = await $methodCall;"),
          ]);
        } else {
          codes.addAll([
            Code("final _returnValue = await $methodCall;"),
            Code("final $_toReturn = _returnValue.toJson();"),
          ]);
        }
      }
    } else if (isPrimitiveType(returnType)) {
      codes.addAll([
        Code("final $_toReturn = $methodCall;"),
      ]);
    } else {
      throw new InvalidGenerationSourceError(
        'Error generating binding for `${method.name}`. This method should only return a void or Future<YourModel>.',
        todo: '',
      );
    }

    if (mustReturn) {
      codes.add(Code("return $_toReturn;"));
    } else {
      codes.add(Code("returns.add($_toReturn);"));
    }

    return Block.of(codes);
  }

  Code generateToReturn(MethodElement method, String methodCall) {
    final returnType = method.returnType;
    if (returnType.isVoid) {
      return Code("$methodCall;");
    } else if (returnType.isDartAsyncFuture) {
      if (returnType is ParameterizedType && returnType.typeArguments.length > 0) {
        final innerReturnType = returnType.typeArguments[0];
        if (innerReturnType.isVoid) {
          return Block.of([
            Code("await $methodCall;"),
          ]);
        } else if (isPrimitiveType(innerReturnType)) {
          return Block.of([
            Code("final $_toReturn = await $methodCall;"),
            Code("if($_toReturn is ${getResponseType(returnType)}){"),
            Code("  return $_toReturn;"),
            Code("}"),
            Code("return null;"),
          ]);
        } else {
          return Block.of([
            Code("try {"),
            Code("  final $_toReturn = await $methodCall;"),
            //Code("  if(returned is String) {"),
            //Code("    final jsonMap = json.decode(returned) as Map<String, dynamic>;"),
            //Code("    return ${getResponseType(returnType).toString()}.fromJson(jsonMap);"),
            //Code("  } else if(returned is Map<String, dynamic>) {"),
            Code(" if($_toReturn is Map<dynamic, dynamic>) {"),
            Code("    return ${getResponseType(returnType).toString()}.fromJson(Map<String, dynamic>.from($_toReturn));"),
            Code("  } "),
            Code("} catch (e) {"),
            Code("  print(\"error while calling ${method.name}\");"),
            Code("  print(e);"),
            Code("}"),
            Code("return null;"),
          ]);
        }
      }
    } else if (isPrimitiveType(returnType)) {
      return Block.of([
        Code("final $_toReturn = $methodCall;"),
        Code("if($_toReturn is $returnType){"),
        Code("  return $_toReturn;"),
        Code("}"),
        Code("return null;"),
      ]);
    } else {
      throw new InvalidGenerationSourceError(
        'Error generating binding for `${method.name}`. This method should only return a void or Future<YourModel>.',
        todo: '',
      );
    }
    return null;
  }
}

Builder generatorFactoryBuilder(BuilderOptions options) => new SharedPartBuilder([new FlutterBridgeGenerator()], "flutter_bridge");
