
#import "RNAlibcSdk.h"
#import "BCBridge.h"
#import <AlibabaAuthSDK/ALBBSDK.h>
#import <AlibcTradeBiz/AlibcTradeBiz.h>
#import <AlibcTradeSDK/AlibcTradeSDK.h>

@implementation RNAlibcSdk {
    bool hasListeners;
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}
+ (BOOL) requiresMainQueueSetup {
    return YES;
}
RCT_EXPORT_MODULE()

// Will be called when this module's first listener is added.
-(void)startObserving {
    hasListeners = YES;
    // Set up any upstream listeners or background tasks as necessary
}

// Will be called when this module's last listener is removed, or on dealloc.
-(void)stopObserving {
    hasListeners = NO;
    // Remove upstream listeners, stop unnecessary background tasks
}

- (instancetype)init {
    self = [super init];
    if (self) {
        [[NSNotificationCenter defaultCenter] addObserver:self
                                                 selector:@selector(handleOpenURL:)
                                                     name:@"RCTOpenURLNotification"
                                                   object:nil];
    }
    return self;
}

- (void)handleOpenURL:(NSNotification *)note {
    NSDictionary *userInfo = note.userInfo;
    NSString *url = userInfo[@"url"];
    NSURL *URL = [NSURL URLWithString:url];
    [[AlibcTradeSDK sharedInstance] application:nil
                                        openURL:URL
                                        options:nil];
    
    
}

RCT_EXPORT_METHOD(initSDK: (RCTResponseSenderBlock)callback)
{
    [[BCBridge sharedInstance] initSDK:callback];
}

RCT_EXPORT_METHOD(login: (RCTResponseSenderBlock)callback)
{
    [[BCBridge sharedInstance] login:callback];
}

RCT_EXPORT_METHOD(isLogin: (RCTResponseSenderBlock)callback)
{
    [[BCBridge sharedInstance] isLogin:callback];
}

RCT_EXPORT_METHOD(getUser: (RCTResponseSenderBlock)callback)
{
    [[BCBridge sharedInstance] getUser:callback];
}

RCT_EXPORT_METHOD(logout: (RCTResponseSenderBlock)callback)
{
    [[BCBridge sharedInstance] logout:callback];
}

RCT_EXPORT_METHOD(show: (NSDictionary *)param callback: (RCTResponseSenderBlock)callback){
    [[BCBridge sharedInstance] show:param callback: callback];
}

@end
  
