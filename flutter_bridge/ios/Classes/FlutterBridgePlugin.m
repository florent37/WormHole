#import "FlutterBridgePlugin.h"
#import <flutter_bridge/flutter_bridge-Swift.h>

@implementation FlutterBridgePlugin
+ (void)registerWithRegistrar:(NSObject<FlutterPluginRegistrar>*)registrar {
  [SwiftFlutterBridgePlugin registerWithRegistrar:registrar];
}
@end
