import Flutter
import UIKit

public class SwiftFlutterBridgePlugin: NSObject, FlutterPlugin {
  public static func register(with registrar: FlutterPluginRegistrar) {
    //let channel = FlutterMethodChannel(name: "wormhole", binaryMessenger: registrar.messenger())
    //let instance = SwiftFlutterBridgePlugin()
    //registrar.addMethodCallDelegate(instance, channel: channel)
  }

  public func handle(_ call: FlutterMethodCall, result: @escaping FlutterResult) {
    //result("iOS " + UIDevice.current.systemVersion)
  }
}
