//
//  BCBridge.h
//  RNAlibcSdk
//
//  Created by IORI on 2019/2/14.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import <Foundation/Foundation.h>
#if __has_include("RCTBridgeModule.h")
#import "RCTBridgeModule.h"
#else
#import <React/RCTBridgeModule.h>
#endif
#import <UIKit/UIKit.h>

NS_ASSUME_NONNULL_BEGIN

@interface BCBridge : NSObject
+ (instancetype)sharedInstance;
- (void)initSDK: (RCTResponseSenderBlock)callback;
- (void)login: (RCTResponseSenderBlock)callback;
- (void)isLogin: (RCTResponseSenderBlock)callback;
- (void)getUser: (RCTResponseSenderBlock)callback;
- (void)logout: (RCTResponseSenderBlock)callback;
- (void)show: (NSDictionary *)param callback: (RCTResponseSenderBlock)callback;
- (void)showInWebView: (UIWebView *)webView param:(NSDictionary *)param;
@end

NS_ASSUME_NONNULL_END
