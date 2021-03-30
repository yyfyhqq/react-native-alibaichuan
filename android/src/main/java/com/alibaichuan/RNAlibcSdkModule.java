
package com.alibaichuan;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

import com.ali.auth.third.core.model.Session;
import com.ali.auth.third.ui.context.CallbackContext;
import com.alibaba.baichuan.android.trade.AlibcTrade;
import com.alibaba.baichuan.android.trade.AlibcTradeSDK;
import com.alibaba.baichuan.android.trade.callback.AlibcTradeCallback;
import com.alibaba.baichuan.android.trade.callback.AlibcTradeInitCallback;
import com.alibaba.baichuan.android.trade.model.AlibcShowParams;
import com.alibaba.baichuan.android.trade.model.OpenType;
import com.alibaba.baichuan.android.trade.page.AlibcAddCartPage;
import com.alibaba.baichuan.android.trade.page.AlibcBasePage;
import com.alibaba.baichuan.android.trade.page.AlibcDetailPage;
import com.alibaba.baichuan.android.trade.page.AlibcMyCartsPage;
import com.alibaba.baichuan.android.trade.page.AlibcMyOrdersPage;
import com.alibaba.baichuan.android.trade.page.AlibcShopPage;
import com.alibaba.baichuan.trade.biz.AlibcConstants;
import com.alibaba.baichuan.trade.biz.applink.adapter.AlibcFailModeType;
import com.alibaba.baichuan.trade.biz.context.AlibcResultType;
import com.alibaba.baichuan.trade.biz.context.AlibcTradeResult;
import com.alibaba.baichuan.trade.biz.core.taoke.AlibcTaokeParams;
import com.alibaba.baichuan.trade.biz.login.AlibcLogin;
import com.alibaba.baichuan.trade.biz.login.AlibcLoginCallback;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RNAlibcSdkModule extends ReactContextBaseJavaModule {

    private final ReactApplicationContext reactContext;
    private static final String TAG = "RNAlibcSdkModule";
    private final static String NOT_LOGIN = "not login";
    private final static String INVALID_TRADE_RESULT = "invalid trade result";
    private final static String INVALID_PARAM = "invalid";

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent intent) {
            CallbackContext.onActivityResult(requestCode, resultCode, intent);
        }
    };

    static private RNAlibcSdkModule mRNAlibcSdkModule = null;

    static public RNAlibcSdkModule sharedInstance(ReactApplicationContext context) {
        if (mRNAlibcSdkModule == null) {
            return new RNAlibcSdkModule(context);
        } else {
            return mRNAlibcSdkModule;
        }
    }

    public RNAlibcSdkModule(ReactApplicationContext reactContext) {
        super(reactContext);
        this.reactContext = reactContext;
        reactContext.addActivityEventListener(mActivityEventListener);
    }

    @Override
    public String getName() {
        return "RNAlibcSdk";
    }
    
   /**
    * 初始化SDK---无参数传入
    */
   @ReactMethod
   public void initSDK(final Callback callback) {
       AlibcTradeSDK.asyncInit((Application) reactContext.getApplicationContext(), new AlibcTradeInitCallback() {
           @Override
           public void onSuccess() {
               callback.invoke(null, "init success");
           }

           @Override
           public void onFailure(int code, String msg) {
               WritableMap map = Arguments.createMap();
               map.putInt("code", code);
               map.putString("msg", msg);
               callback.invoke(map);
           }
       });
   }

   /**
    * 登录
    * @param callback
    */
   @ReactMethod
   public void login(final Callback callback) {
       AlibcLogin alibcLogin = AlibcLogin.getInstance();
       alibcLogin.showLogin(new AlibcLoginCallback() {
           @Override
           public void onSuccess(int loginResult, String openId, String userNick) {
               // 参数说明：
               // loginResult(0--登录初始化成功；1--登录初始化完成；2--登录成功)
               // openId：用户id
               // userNick: 用户昵称
               // Log.i(TAG, "获取淘宝用户信息: " + AlibcLogin.getInstance().getSession());
               Session session = AlibcLogin.getInstance().getSession();
               WritableMap map = Arguments.createMap();
               map.putString("userNick", userNick);
               // map.putString("loginResult", loginResult);
               map.putString("avatarUrl", session.avatarUrl);
               map.putString("openId", openId);
               // map.putString("openSid", session.openSid);
               // map.putString("topAccessToken", session.topAccessToken);
               // map.putString("topAuthCode", session.topAuthCode);
               callback.invoke(null, map);
           }

           @Override
           public void onFailure(int code, String msg) {
               // code：错误码  msg： 错误信息
               WritableMap map = Arguments.createMap();
               map.putInt("code", code);
               map.putString("msg", msg);
               callback.invoke(map);
           }
       });
   }

  @ReactMethod
  public void isLogin(final Callback callback) {
      callback.invoke(null, AlibcLogin.getInstance().isLogin());
  }

  @ReactMethod
  public void getUser(final Callback callback) {
      if (AlibcLogin.getInstance().isLogin()) {
        Session session = AlibcLogin.getInstance().getSession();
        WritableMap map = Arguments.createMap();
        map.putString("nick", session.nick);
        map.putString("avatarUrl", session.avatarUrl);
        map.putString("openId", session.openId);
        map.putString("openSid", session.openSid);
        callback.invoke(null, map);
      } else {
        callback.invoke(NOT_LOGIN);
      }
  }

   /**
    * 退出登录---无参数传入
    */
   @ReactMethod
   public void logout(final Callback callback) {
       if (AlibcLogin.getInstance().getSession() != null
               && AlibcLogin.getInstance().isLogin()) {
           AlibcLogin alibcLogin = AlibcLogin.getInstance();

           alibcLogin.logout(new AlibcLoginCallback() {
               @Override
               public void onSuccess(int i, String s, String s1) {
                   WritableMap map = Arguments.createMap();
                   map.putString("code", "0");
                   map.putString("message", "success");
                   callback.invoke(null, map);
               }

               @Override
               public void onFailure(int i, String s) {
                   WritableMap map = Arguments.createMap();
                   map.putString("code", Integer.toString(i));
                   map.putString("message", s);
                   callback.invoke(map);
               }
           });
       } else {
           WritableMap map = Arguments.createMap();
           map.putString("code", "90000");
           map.putString("message", "Not logged in");
           callback.invoke(map);
       }
   }

    /**
     * 展示
     */
    @ReactMethod
    public void show(final ReadableMap param, final Callback callback) {
        String type = param.getString("type");
        ReadableMap payload = param.getMap("payload");
        switch (type) {
            case "detail":
                this._show(new AlibcDetailPage(payload.getString("itemid")), param, callback);
                break;
            case "url":
                this._openUrl(payload.getString("url"), param, callback);
                break;
            case "shop":
                this._show(new AlibcShopPage(payload.getString("shopid")), param, callback);
                break;
            case "orders":
                this._show(new AlibcMyOrdersPage(payload.getInt("orderStatus"), payload.getBoolean("allOrder")), param, callback);
                break;
            case "addCard":
                this._show(new AlibcAddCartPage(param.getString("itemid")), param, callback);
                break;
            case "mycard":
                this._show(new AlibcMyCartsPage(), param, callback);
                break;
            case "auth":
                this._openAuth(payload.getString("url"), param, callback);
                break;
            default:
                callback.invoke(INVALID_PARAM);
                break;
        }
    }

    private void _openUrl(final String url, final ReadableMap param, final Callback callback) {

        AlibcShowParams showParams = new AlibcShowParams();
        showParams = this.dealShowParams(param);
        AlibcTaokeParams taokeParams = new AlibcTaokeParams("", "", "");
        taokeParams = this.dealTaokeParams(param);
        Map<String, String> exParams = new HashMap<String, String>();
        exParams = this.dealExParams(param);

        AlibcTrade.openByUrl(getCurrentActivity(),
                "",
                url,
                null,
                new WebViewClient(),
                new WebChromeClient(),
                showParams,
                taokeParams,
                exParams,
                new AlibcTradeCallback() {
                    public void onTradeSuccess(AlibcTradeResult tradeResult) {
                        Log.v("ReactNative", TAG + ":onTradeSuccess");
                        //打开电商组件，用户操作中成功信息回调。tradeResult：成功信息（结果类型：加购，支付；支付结果）
                        if (tradeResult.resultType.equals(AlibcResultType.TYPECART)) {
                            //加购成功
                            WritableMap map = Arguments.createMap();
                            map.putString("type", "card");
                            callback.invoke(null, map);
                        } else if (tradeResult.resultType.equals(AlibcResultType.TYPEPAY)) {
                            //支付成功
                            WritableMap map = Arguments.createMap();
                            map.putString("type", "pay");
                            map.putString("orders", "" + tradeResult.payResult.paySuccessOrders);
                            callback.invoke(null, map);
                        } else {
                            callback.invoke(INVALID_TRADE_RESULT);
                        }
                    }

                    @Override
                    public void onFailure(int code, String msg) {
                        WritableMap map = Arguments.createMap();
                        map.putString("type", "error");
                        map.putInt("code", code);
                        map.putString("msg", msg);
                        callback.invoke(msg);
                    }
                });
    }

    @SuppressLint("JavascriptInterface")
    private void _openAuth(final String url, final ReadableMap param, final Callback callback) {

//        final AlibcShowParams showParams = new AlibcShowParams();
//        showParams.setOpenType(OpenType.Native);
//        showParams.setBackUrl("");
//        final AlibcTaokeParams taokeParams = new AlibcTaokeParams("", "", "");
//        final Map<String, String> trackParams = new HashMap<>();

        final AlibcShowParams showParams = this.dealShowParams(param);
        final AlibcTaokeParams taokeParams = this.dealTaokeParams(param);
        final Map<String, String> exParams = this.dealExParams(param);

        Log.i(TAG, "假设已在子线程");
        Log.i(TAG, "线程1： " + android.os.Process.myTid());

        getCurrentActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "已在主线程中，可以更新UI");
                Log.i(TAG, "线程2： " + android.os.Process.myTid());

                Context mContext = getCurrentActivity().getApplicationContext();
                final WebView mWebView = new WebView(mContext);

                FrameLayout view =  (FrameLayout)getCurrentActivity().getWindow().getDecorView();
                final FrameLayout frame = new FrameLayout(mContext);
                FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                params.gravity = Gravity.CENTER;
                frame.setLayoutParams(params);

                FrameLayout.LayoutParams mWebViewParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT);
                mWebViewParams.gravity = Gravity.CENTER_HORIZONTAL;
                mWebView.setLayoutParams(mWebViewParams);
                mWebView.setBackgroundColor(Color.BLUE); // 设置背景色
                frame.addView(mWebView);
                view.addView(frame, params);
                Log.i(TAG, "线程5： " + android.os.Process.myTid());

                WebViewClient mWebViewClient = new WebViewClient() {
                    @Override
                    public void onPageStarted(WebView view, String url, Bitmap favicon) {
                        Log.i(TAG, "打印AlibcTrade.openByUrl：" + "onPageStarted: " + url);
                        super.onPageStarted(view, url, favicon);
                    }

                    @Override
                    public void onPageFinished(WebView view, String url) {
                        Log.i(TAG, "打印AlibcTrade.openByUrl：" + "onPageFinished: " + url);
                        super.onPageFinished(view, url);
                    }

                    @Override
                    public boolean shouldOverrideUrlLoading(WebView view, String url) {
                        Log.i(TAG, "打印AlibcTrade.openByUrl：" + "shouldOverrideUrlLoading: " + url);
                        if(url.indexOf("code=") != -1){
                            String code = cutString(url, "code=", "&");
                            Log.i(TAG, "code: " + code);
                            WritableMap map = Arguments.createMap();
                            map.putString("code", code);
                            callback.invoke(null, map);
                            frame.removeView(mWebView);
                            mWebView.destroy();
                            return true;
                        }
                        return false;
                    }
                };
                final WebChromeClient mWebChromeClient = new WebChromeClient() {
                    public void onProgressChanged(WebView view, int newProgress) {
                        if (newProgress == 100) {
                            Log.i(TAG, "打印AlibcTrade.openByUrl：" + "WebChromeClient onProgressChanged");
                        }
                        super.onProgressChanged(view, newProgress);
                    }
                };
//                mWebView.addJavascriptInterface(this,"android");//添加js监听 这样html就能调用客户端
                mWebView.setWebViewClient(mWebViewClient);
                mWebView.setWebChromeClient(mWebChromeClient);
                WebSettings mWebSetting = mWebView.getSettings();
                mWebSetting.setJavaScriptEnabled(true);//支持javascript
                mWebSetting.setUseWideViewPort(true);// 设置可以支持缩放
                mWebSetting.setLoadWithOverviewMode(true);
                Log.i(TAG, "线程6： " + android.os.Process.myTid());

                AlibcTrade.openByUrl(getCurrentActivity(),
                "",
                url,
                mWebView,
                mWebViewClient,
                mWebChromeClient,
                showParams, taokeParams, exParams,
                new AlibcTradeCallback() {
                    @Override
                    public void onTradeSuccess(AlibcTradeResult tradeResult) {
                        Log.i("WebViewActivity", "request success");
                        callback.invoke(null, "auth success");
                    }
                    @Override
                    public void onFailure(int code, String msg) {
                        Log.e("WebViewActivity", "code=" + code + ", msg=" + msg);
                        WritableMap map = Arguments.createMap();
                        map.putString("type", "error");
                        map.putInt("code", code);
                        map.putString("msg", msg);
                        callback.invoke(map);
                    }
                });

                Log.i(TAG, "线程7： " + android.os.Process.myTid());
            }
        });

        Log.i(TAG, "线程8： " + android.os.Process.myTid());

    }

    private String cutString(String str, String start, String end) {
        /*if (isBlank(str)) {
            return str;
        }*/
        String reg = start + "(.*)" + end;
        Pattern pattern = Pattern.compile(reg);
        Matcher matcher = pattern.matcher(str);
        while (matcher.find()) {
            str = matcher.group(1);
        }
        return str;
    }

    public void showInWebView(final WebView webview, WebViewClient webViewClient, final ReadableMap param) {
        String type = param.getString("type");
        ReadableMap payload = param.getMap("payload");
        switch(type){
            case "detail":
                this._showInWebView(webview, webViewClient, new AlibcDetailPage(param.getString("payload")), param);
                break;
            case "url":
                this._urlShowInWebView(webview, webViewClient,payload.getString("url"), param);
                break;
            case "shop":
                this._showInWebView(webview, webViewClient, new AlibcShopPage(param.getString("payload")), param);
                break;
            case "orders":
                this._showInWebView(webview, webViewClient, new AlibcMyOrdersPage(payload.getInt("orderType"), payload.getBoolean("isAllOrder")), param);
                break;
            case "addCard":
                this._showInWebView(webview, webViewClient, new AlibcAddCartPage(param.getString("payload")), param);
                break;
            case "mycard":
                this._showInWebView(webview, webViewClient, new AlibcMyCartsPage(), param);
                break;
            default:
                WritableMap event = Arguments.createMap();
                event.putString("type", INVALID_PARAM);
                reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                        webview.getId(),
                        "onTradeResult",
                        event);
                break;
        }
    }

    private void _urlShowInWebView(final WebView webview, WebViewClient webViewClient, String url, final ReadableMap param) {
        // 处理参数
        AlibcShowParams showParams = new AlibcShowParams();
        showParams = this.dealShowParams(param);
        AlibcTaokeParams taokeParams = new AlibcTaokeParams("", "", "");
        taokeParams = this.dealTaokeParams(param);
        Map<String, String> exParams = new HashMap<String, String>();
        exParams = this.dealExParams(param);

        AlibcTrade.openByUrl(getCurrentActivity(),
                "",
                url,
                webview,
                webViewClient,
                new WebChromeClient(),
                showParams,
                taokeParams,
                exParams,
                new AlibcTradeCallback() {
          @Override
          public void onTradeSuccess(AlibcTradeResult tradeResult) {
            Log.v("ReactNative", TAG + ":onTradeSuccess");
            WritableMap event = Arguments.createMap();
            //打开电商组件，用户操作中成功信息回调。tradeResult：成功信息（结果类型：加购，支付；支付结果）
            if(tradeResult.resultType.equals(AlibcResultType.TYPECART)){
                event.putString("type", "card");
            }else if (tradeResult.resultType.equals(AlibcResultType.TYPECART)){
                event.putString("type", "pay");
                event.putString("orders", "" + tradeResult.payResult.paySuccessOrders);
            }else { 
                event.putString("type", INVALID_PARAM);
            }
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                    webview.getId(),
                    "onTradeResult",
                    event);
            }
          @Override
          public void onFailure(int code, String msg) {
            WritableMap event = Arguments.createMap();
            event.putString("type", "error");
            event.putInt("code", code);
            event.putString("msg", msg);
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                    webview.getId(),
                    "onTradeResult",
                    event);
          }
      });
    }

    private void _showInWebView(final WebView webview, WebViewClient webViewClient, final AlibcBasePage page, final ReadableMap param) {
        // 处理参数
        AlibcShowParams showParams = new AlibcShowParams();
        showParams = this.dealShowParams(param);
        AlibcTaokeParams taokeParams = new AlibcTaokeParams("", "", "");
        taokeParams = this.dealTaokeParams(param);
        Map<String, String> exParams = new HashMap<String, String>();
        exParams = this.dealExParams(param);

        AlibcTrade.openByBizCode(getCurrentActivity(),
                page,
                webview,
                webViewClient,
                new WebChromeClient(),
                "detail",
                showParams,
                taokeParams,
                exParams,
                new AlibcTradeCallback() {
          @Override
          public void onTradeSuccess(AlibcTradeResult tradeResult) {
            Log.v("ReactNative", TAG + ":onTradeSuccess");
            WritableMap event = Arguments.createMap();
            //打开电商组件，用户操作中成功信息回调。tradeResult：成功信息（结果类型：加购，支付；支付结果）
            if(tradeResult.resultType.equals(AlibcResultType.TYPECART)){
                event.putString("type", "card");
            }else if (tradeResult.resultType.equals(AlibcResultType.TYPECART)){
                event.putString("type", "pay");
                event.putString("orders", "" + tradeResult.payResult.paySuccessOrders);
            }else { 
                event.putString("type", INVALID_PARAM);
            }
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                    webview.getId(),
                    "onTradeResult",
                    event);
            }
          @Override
          public void onFailure(int code, String msg) {
            WritableMap event = Arguments.createMap();
            event.putString("type", "error");
            event.putInt("code", code);
            event.putString("msg", msg);
            reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                    webview.getId(),
                    "onTradeResult",
                    event);
          }
      });
    }

    private void _show(AlibcBasePage page, final ReadableMap param, final Callback callback) {
        // 处理参数
        AlibcShowParams showParams = new AlibcShowParams();
        showParams = this.dealShowParams(param);
        AlibcTaokeParams taokeParams = new AlibcTaokeParams("", "", "");
        taokeParams = this.dealTaokeParams(param);
        Map<String, String> exParams = new HashMap<String, String>();
        exParams = this.dealExParams(param);

        AlibcTrade.openByBizCode(getCurrentActivity(),
                page,
                null,
                new WebViewClient(),
                new WebChromeClient(),
                "detail",
                showParams,
                taokeParams,
                exParams,
                new AlibcTradeCallback() {
                    public void onTradeSuccess(AlibcTradeResult tradeResult) {
                        Log.v("ReactNative", TAG + ":onTradeSuccess");
                        //打开电商组件，用户操作中成功信息回调。tradeResult：成功信息（结果类型：加购，支付；支付结果）
                        if (tradeResult.resultType.equals(AlibcResultType.TYPECART)) {
                            //加购成功
                            WritableMap map = Arguments.createMap();
                            map.putString("type", "card");
                            callback.invoke(null, map);
                        } else if (tradeResult.resultType.equals(AlibcResultType.TYPEPAY)) {
                            //支付成功
                            WritableMap map = Arguments.createMap();
                            map.putString("type", "pay");
                            map.putString("orders", "" + tradeResult.payResult.paySuccessOrders);
                            callback.invoke(null, map);
                        } else {
                            callback.invoke(INVALID_TRADE_RESULT);
                        }
                    }

                    @Override
                    public void onFailure(int code, String msg) {
                        WritableMap map = Arguments.createMap();
                        map.putString("type", "error");
                        map.putInt("code", code);
                        map.putString("msg", msg);
                        callback.invoke(msg);
                    }
                });
    }

    /**
     * 处理showParams公用参数
     */
    private AlibcShowParams dealShowParams(final ReadableMap param) {
        ReadableMap payload = param.getMap("payload");
        // 初始化参数
        String opentype = "html5";

        AlibcShowParams showParams = new AlibcShowParams();

        if (payload.getString("opentype") != null
                || !payload.getString("opentype").equals("")) {
            opentype = payload.getString("opentype");
        }

        if (opentype.equals("auto")) {
            showParams.setOpenType(OpenType.Auto);
        } else {
            showParams.setOpenType(OpenType.Native);
        }
        showParams.setClientType("taobao");
        showParams.setBackUrl("alisdk://");
        showParams.setNativeOpenFailedMode(AlibcFailModeType.AlibcNativeFailModeJumpH5);
        return showParams;
    }

    /**
     * 处理taokeParams公用参数
     */
    private AlibcTaokeParams dealTaokeParams(final ReadableMap param) {
        ReadableMap payload = param.getMap("payload");
        // 初始化参数
        String mmpid = "mm_1539480047_2240900183_111206300166";
        String adzoneid = "111206300166";
        String tkkey = "1539480047";

        // 设置mmpid
        if (payload.getString("mmpid") != null
                || !payload.getString("mmpid").equals("")) {
            mmpid = payload.getString("mmpid");
        }

        // 设置adzoneid
        if (payload.getString("adzoneid") != null
                || !payload.getString("adzoneid").equals("")) {
            adzoneid = payload.getString("adzoneid");
        }

        // 设置tkkey
        if (payload.getString("tkkey") != null
                || !payload.getString("tkkey").equals("")) {
            tkkey = payload.getString("tkkey");
        }

        AlibcTaokeParams taokeParams = new AlibcTaokeParams("", "", "");
        taokeParams.setPid(mmpid);
        taokeParams.setAdzoneid(adzoneid);
        Map<String, String> taokeExParams = new HashMap<String, String>();
        taokeExParams.put("taokeAppkey", tkkey);
        taokeParams.extraParams = taokeExParams;
        return taokeParams;
    }

    /**
     * 处理exParams公用参数
     */
    private Map<String, String> dealExParams(final ReadableMap param) {
        ReadableMap payload = param.getMap("payload");
        // 初始化参数
        Map<String, String> exParams = new HashMap<String, String>();
        String isvcode = "app";
        // 设置tkkey
        if (payload.getString("isvcode") != null
                || !payload.getString("isvcode").equals("")) {
            isvcode = payload.getString("isvcode");
        }
        exParams.put(AlibcConstants.ISV_CODE, isvcode);
        return exParams;
    }
}
