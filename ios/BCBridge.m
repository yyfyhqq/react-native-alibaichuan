//
//  BCBridge.m
//  RNAlibcSdk
//
//  Created by IORI on 2019/2/14.
//  Copyright © 2019 Facebook. All rights reserved.
//

#import "BCBridge.h"
#import "BCWebView.h"
#import <AlibabaAuthSDK/ALBBSDK.h>
#import <AlibcTradeSDK/AlibcTradeSDK.h>
#import <AlibcTradeSDK/AlibcTradeService.h>
#import <AlibcTradeSDK/AlibcTradePageFactory.h>
#import <React/RCTLog.h>

#define NOT_LOGIN (@"not login")

@implementation BCBridge {
    AlibcTradeTaokeParams *taokeParams;
    AlibcTradeShowParams *showParams;
}

+ (instancetype) sharedInstance {
    static BCBridge *instance = nil;
    if (!instance) {
        instance = [[BCBridge alloc] init];
    }
    return instance;
}

- (void)initSDK: (RCTResponseSenderBlock)callback
{
    NSString* isvVersion = @"4.0.1.5";
    NSString* isvAppName = @"alibaichuan";
    // 百川平台基础SDK初始化，加载并初始化各个业务能力插件
   ALBBSDK *albbSDK = [ALBBSDK sharedInstance];
   [[AlibcTradeSDK sharedInstance] setDebugLogOpen:YES];
   [[AlibcTradeSDK sharedInstance] setIsvAppName:isvAppName];
   [[AlibcTradeSDK sharedInstance] setIsvVersion:isvVersion];
   [albbSDK setAppkey:@"32609009"];
   [albbSDK setAuthOption:NormalAuth];
   [[AlibcTradeSDK sharedInstance] setEnv:AlibcEnvironmentRelease];
   [[AlibcTradeSDK sharedInstance] asyncInitWithSuccess:^{
       NSLog(@"百川SDK初始化成功");
       callback(@[[NSNull null], @"init success"]);
    } failure:^(NSError *error) {
       NSLog(@"百川SDK初始化失败");
       NSDictionary *ret = @{@"code": @(error.code), @"msg":error.description};
       callback(@[ret]);
   }];
}

- (void)login: (RCTResponseSenderBlock)callback
{
    if(![[ALBBSession sharedInstance] isLogin]){
        [[ALBBSDK sharedInstance] auth:[UIApplication sharedApplication].delegate.window.rootViewController successCallback:^(ALBBSession *session) {
            ALBBUser *user = [session getUser];
            NSLog(@"session == %@, user.nick == %@,user.avatarUrl == %@,user.openId ==                 %@,user.openSid == %@,user.topAccessToken ==         %@",session,user.nick,user.avatarUrl,user.openId,user.openSid,user.topAccessToken);
            //授权成功
            NSString *isLg;
            if([session isLogin]){
               isLg=@"true";
            }else{
               isLg=@"false";
            }
            NSDictionary *ret = @{
                                 @"userNick" :user.nick,
                                 @"avatarUrl":user.avatarUrl,
                                 @"openId":user.openId,
                                 @"isLogin":isLg
                                 };
            callback(@[[NSNull null], ret]);
        } failureCallback:^(ALBBSession *session, NSError *error) {
           NSLog(@"session == %@,error == %@",session,error);
           NSDictionary *ret = @{@"code": @(error.code), @"msg":error.description};
           callback(@[ret]);
        }];
    }else{
        //如果登录的输出用户信息
        ALBBSession *session=[ALBBSession sharedInstance];
        ALBBUser *user = [session getUser];
        NSString *isLg;
        if([session isLogin]){
            isLg=@"true";
        }else{
            isLg=@"false";
        }
        NSDictionary *ret = @{
                               @"userNick" :user.nick,
                               @"avatarUrl":user.avatarUrl,
                               @"openId":user.openId,
                               @"isLogin":isLg};
        callback(@[[NSNull null], ret]);
    }
}

- (void)isLogin: (RCTResponseSenderBlock)callback
{
    bool isLogin = [[ALBBSession sharedInstance] isLogin];
    callback(@[[NSNull null], [NSNumber numberWithBool: isLogin]]);
}

- (void)getUser: (RCTResponseSenderBlock)callback
{
    if([[ALBBSession sharedInstance] isLogin]){
        ALBBUser *s = [[ALBBSession sharedInstance] getUser];
        NSDictionary *ret = @{@"nick": s.nick, @"avatarUrl":s.avatarUrl, @"openId":s.openId, @"openSid":s.openSid};
        callback(@[[NSNull null], ret]);
    } else {
        callback(@[NOT_LOGIN]);
    }
}

- (void)logout: (RCTResponseSenderBlock)callback
{
   [[ALBBSDK sharedInstance] logout];
    callback(@[[NSNull null]]);
}

- (void)show: (NSDictionary *)param callback: (RCTResponseSenderBlock)callback
{
    NSString *type = param[@"type"];
    NSDictionary *payload = (NSDictionary *)param[@"payload"];
    
    id<AlibcTradePage> page;
    if ([type isEqualToString:@"detail"]) {
        page = [AlibcTradePageFactory itemDetailPage:(NSString *)payload[@"itemid"]];
        [self _show:page param:param bizCode:@"detail" callback:callback];
    } else if ([type isEqualToString:@"url"]) {
        NSString* url = payload[@"url"];
        [self _showUrl:url param:param callback:callback];
    } else if ([type isEqualToString:@"shop"]) {
        page = [AlibcTradePageFactory shopPage:(NSString *)payload[@"shopid"]];
        [self _show:page param:param bizCode:@"shop" callback:callback];
    } else if ([type isEqualToString:@"orders"]) {
        page = [AlibcTradePageFactory myOrdersPage:[payload[@"orderStatus"] integerValue] isAllOrder:[payload[@"allOrder"] boolValue]];
        [self _show:page param:param bizCode:@"orders" callback:callback];
    } else if ([type isEqualToString:@"addCard"]) {
        page = [AlibcTradePageFactory addCartPage:(NSString *)payload[@"itemid"]];
        [self _show:page param:param bizCode:@"addCart" callback:callback];
    } else if ([type isEqualToString:@"mycard"]) {
        page = [AlibcTradePageFactory myCartsPage];
        [self _show:page param:param bizCode:@"cart" callback:callback];
    } else {
        RCTLog(@"not implement");
        return;
    }
}

- (void)_show: (id<AlibcTradePage>)page param:(NSDictionary *)param bizCode: (NSString *)bizCode callback: (RCTResponseSenderBlock)callback
{
    //处理参数
    NSDictionary* result = [self dealParam:param];
    AlibcTradeTaokeParams *taokeParams = [[AlibcTradeTaokeParams alloc] init];
    taokeParams = result[@"taokeParams"];
    AlibcTradeShowParams* showParams = [[AlibcTradeShowParams alloc] init];
    showParams = result[@"showParams"];
    NSDictionary *trackParam = result[@"trackParam"];
    
    [[AlibcTradeSDK sharedInstance].tradeService openByBizCode:bizCode page:page webView:nil parentController:[UIApplication sharedApplication].delegate.window.rootViewController showParams:showParams taoKeParams:taokeParams trackParam:trackParam tradeProcessSuccessCallback:^(AlibcTradeResult * _Nullable result) {
        //成功回调
        NSArray *orderId=@[];
        if(result.result == AlibcTradeResultTypePaySuccess){
            orderId=result.payResult.paySuccessOrders;
        }
        NSDictionary *ret = @{@"code" : @"0",@"message":@"success",@"orderid":orderId};
        callback(@[ret]);
    } tradeProcessFailedCallback:^(NSError * _Nullable error) {
        //失败回调
        NSDictionary *ret = @{@"code":[NSString stringWithFormat:@"%ld", (long)[error code]],@"message":[[error userInfo] objectForKey:NSLocalizedDescriptionKey]};
        callback(@[ret]);
    }];
}

- (void)_showUrl: (NSString *)url param:(NSDictionary *)param callback: (RCTResponseSenderBlock)callback
{
    //处理参数
    NSDictionary* result = [self dealParam:param];
    AlibcTradeTaokeParams *taokeParams = [[AlibcTradeTaokeParams alloc] init];
    taokeParams = result[@"taokeParams"];
    AlibcTradeShowParams* showParams = [[AlibcTradeShowParams alloc] init];
    showParams = result[@"showParams"];
    NSDictionary *trackParam = result[@"trackParam"];
    
    [[AlibcTradeSDK sharedInstance].tradeService openByUrl:url identity:@"trade" webView:nil parentController:[UIApplication sharedApplication].delegate.window.rootViewController showParams:showParams taoKeParams:taokeParams trackParam:trackParam tradeProcessSuccessCallback:^(AlibcTradeResult * _Nullable result) {
        //成功回调
        NSArray *orderId=@[];
        if(result.result == AlibcTradeResultTypePaySuccess){
            orderId=result.payResult.paySuccessOrders;
        }
        NSDictionary *ret = @{@"code" : @"0",@"message":@"success",@"orderid":orderId};
        callback(@[ret]);
    } tradeProcessFailedCallback:^(NSError * _Nullable error) {
        //失败回调
        NSDictionary *ret = @{@"code":[NSString stringWithFormat:@"%ld", (long)[error code]],@"message":[[error userInfo] objectForKey:NSLocalizedDescriptionKey]};
        callback(@[ret]);
    }];
}

- (void)showInWebView: (BCWebView *)webView param:(NSDictionary *)param
{
    NSString *type = param[@"type"];
    NSDictionary *payload = (NSDictionary *)param[@"payload"];
    
    id<AlibcTradePage> page;
    if ([type isEqualToString:@"detail"]) {
        page = [AlibcTradePageFactory itemDetailPage:(NSString *)payload[@"itemid"]];
        [self _showInWebView:webView page:page param:param bizCode:@"detail"];
    } else if ([type isEqualToString:@"url"]) {
        NSString* url = payload[@"url"];
        [self _showUrlInWebView:webView url:url param:param];
    } else if ([type isEqualToString:@"shop"]) {
        page = [AlibcTradePageFactory shopPage:(NSString *)payload[@"shopid"]];
        [self _showInWebView:webView page:page param:param bizCode:@"shop"];
    } else if ([type isEqualToString:@"orders"]) {
        page = [AlibcTradePageFactory myOrdersPage:[payload[@"orderStatus"] integerValue] isAllOrder:[payload[@"allOrder"] boolValue]];
        [self _showInWebView:webView page:page param:param bizCode:@"orders"];
    } else if ([type isEqualToString:@"addCard"]) {
        page = [AlibcTradePageFactory addCartPage:(NSString *)payload[@"itemid"]];
        [self _showInWebView:webView page:page param:param bizCode:@"addCart"];
    } else if ([type isEqualToString:@"mycard"]) {
        page = [AlibcTradePageFactory myCartsPage];
        [self _showInWebView:webView page:page param:param bizCode:@"cart"];
    } else {
        RCTLog(@"not implement");
        return;
    }
}

- (void)_showInWebView: (UIWebView *)webView page:(id<AlibcTradePage>)page param:(NSDictionary *)param bizCode: (NSString *)bizCode
{
    //处理参数
    NSDictionary* result = [self dealParam:param];
    AlibcTradeTaokeParams *taokeParams = [[AlibcTradeTaokeParams alloc] init];
    taokeParams = result[@"taokeParams"];
    AlibcTradeShowParams* showParams = [[AlibcTradeShowParams alloc] init];
    showParams = result[@"showParams"];
    NSDictionary *trackParam = result[@"trackParam"];
    
    [[AlibcTradeSDK sharedInstance].tradeService openByBizCode:bizCode page:page webView: webView parentController:[UIApplication sharedApplication].delegate.window.rootViewController showParams:showParams taoKeParams:taokeParams trackParam:trackParam tradeProcessSuccessCallback:^(AlibcTradeResult * _Nullable result) {
        //成功回调
        NSArray *orderId=@[];
        if(result.result == AlibcTradeResultTypePaySuccess){
            orderId=result.payResult.paySuccessOrders;
        }
        NSDictionary *ret = @{@"code" : @"0",@"message":@"success",@"orderid":orderId};
        ((BCWebView *)webView).onTradeResult(ret);
    } tradeProcessFailedCallback:^(NSError * _Nullable error) {
        //失败回调
        NSDictionary *ret = @{@"code":[NSString stringWithFormat:@"%ld", (long)[error code]],@"message":[[error userInfo] objectForKey:NSLocalizedDescriptionKey]};
        ((BCWebView *)webView).onTradeResult(ret);
    }];
}

- (void)_showUrlInWebView: (UIWebView *)webView url:(NSString *)url param:(NSDictionary *)param
{
    //处理参数
    NSDictionary* result = [self dealParam:param];
    AlibcTradeTaokeParams *taokeParams = [[AlibcTradeTaokeParams alloc] init];
    taokeParams = result[@"taokeParams"];
    AlibcTradeShowParams* showParams = [[AlibcTradeShowParams alloc] init];
    showParams = result[@"showParams"];
    NSDictionary *trackParam = result[@"trackParam"];
    
    [[AlibcTradeSDK sharedInstance].tradeService openByUrl:url identity:@"trade" webView:webView parentController:[UIApplication sharedApplication].delegate.window.rootViewController showParams:showParams taoKeParams:taokeParams trackParam:trackParam tradeProcessSuccessCallback:^(AlibcTradeResult * _Nullable result) {
        //成功回调
        NSArray *orderId=@[];
        if(result.result == AlibcTradeResultTypePaySuccess){
            orderId=result.payResult.paySuccessOrders;
        }
        NSDictionary *ret = @{@"code" : @"0",@"message":@"success",@"orderid":orderId};
        ((BCWebView *)webView).onTradeResult(ret);
    } tradeProcessFailedCallback:^(NSError * _Nullable error) {
        //失败回调
        NSDictionary *ret = @{@"code":[NSString stringWithFormat:@"%ld", (long)[error code]],@"message":[[error userInfo] objectForKey:NSLocalizedDescriptionKey]};
        ((BCWebView *)webView).onTradeResult(ret);
    }];
}

/****---------------以下是公用方法----------------**/
//公用参数处理
- (NSDictionary *)dealParam:(NSDictionary *)param
{
    NSDictionary *payload = (NSDictionary *)param[@"payload"];
    
    NSString *mmPid = @"mm_1539480047_2240900183_111206300166";
    NSString *isvcode=@"app";
    NSString *adzoneid=@"111206300166";
    NSString *tkkey=@"1539480047";
    
    AlibcTradeTaokeParams *taokeParam = [[AlibcTradeTaokeParams alloc] init];
    if ((NSString *)payload[@"mmpid"]!=nil) {
        mmPid=(NSString *)payload[@"mmpid"];
    }
    
    if ((NSString *)payload[@"adzoneid"]!=nil) {
        adzoneid=(NSString *)payload[@"adzoneid"];
    }
    
    if ((NSString *)payload[@"tkkey"]!=nil) {
        tkkey=(NSString *)payload[@"tkkey"];
    }
    
    taokeParam.pid = mmPid;
    taokeParam.adzoneId = adzoneid;
    taokeParam.extParams=@{@"taokeAppkey":tkkey};
    
    AlibcTradeShowParams* showParam = [[AlibcTradeShowParams alloc] init];
    if ((NSString *)payload[@"opentype"]!=nil) {
        if([(NSString *)payload[@"opentype"] isEqual:@"html5"]){
            showParam.openType = AlibcOpenTypeAuto;
        }else{
            showParam.openType = AlibcOpenTypeNative;
        }
    }else{
        showParam.openType = AlibcOpenTypeAuto;
    }
    //showParam.nativeFailMode = AlibcNativeFailModeJumpH5;
    //新版加入，防止唤醒手淘app的时候打开h5
    showParam.linkKey=@"taobao";
    
    if ((NSString *)payload[@"isvcode"]!=nil) {
        isvcode=(NSString *)payload[@"isvcode"];
    }
    NSDictionary *trackParam=@{@"isv_code":isvcode};
    //返回处理后的参数
    return @{@"showParams":showParam,@"taokeParams":taokeParam,@"trackParam":trackParam};
}


@end
