package com.alibaichuan;

import android.graphics.Bitmap;
import android.util.Log;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.common.ReactConstants;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.facebook.react.uimanager.events.RCTEventEmitter;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class AlibcTradeWebViewManager extends SimpleViewManager<WebView> {
	private final static String REACT_CLASS = "AlibcTradeWebView";

	public static final int COMMAND_GO_BACK = 1;
	public static final int COMMAND_GO_FORWARD = 2;
	public static final int COMMAND_RELOAD = 3;

	private RNAlibcSdkModule mModule;

	private class AlibcWebView extends WebView {
		private ReactContext mContext;
		AlibcWebView(ReactContext context){
			super(context.getCurrentActivity());
			mContext = context;
		}

		public ReactContext getReactContext(){
			return mContext;
		}
	}

	private class AlibcWebViewClient extends WebViewClient {

		@Override
		public void onPageFinished(WebView webView, String url) {
			super.onPageFinished(webView, url);
			WritableMap event = Arguments.createMap();
			event.putBoolean("loading", false);
			event.putBoolean("canGoBack", webView.canGoBack());
			event.putString("title", webView.getTitle());
			ReactContext reactContext = ((AlibcWebView)webView).getReactContext();
			reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(webView.getId(), "onStateChange", event);
		}

		@Override
		public void onPageStarted(WebView webView, String url, Bitmap favicon) {
			super.onPageStarted(webView, url, favicon);
			WritableMap event = Arguments.createMap();
			event.putBoolean("loading", true);
			event.putBoolean("canGoBack", webView.canGoBack());
			ReactContext reactContext = ((AlibcWebView)webView).getReactContext();
			reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(webView.getId(), "onStateChange", event);
		}

		@Override
		public boolean shouldOverrideUrlLoading(WebView webView, String url) {
			Log.v(ReactConstants.TAG, REACT_CLASS + " shouldOverrideUrlLoading:" + url);
			if (url.startsWith("http://") || url.startsWith("https://") ||
				url.startsWith("file://")) {
				if(url.indexOf("code=") != -1){
					String code = cutString(url, "code=", "&");
					Log.v(ReactConstants.TAG, REACT_CLASS + "code: " + code);
					WritableMap event = Arguments.createMap();
					event.putString("code", code);
					ReactContext reactContext = ((AlibcWebView)webView).getReactContext();
					reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(webView.getId(), "onTradeResult", event);					
					return true;
				}
				return false;
			} else {
				return true;
			}
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
	}

	AlibcTradeWebViewManager(RNAlibcSdkModule module) {
		mModule = module;
	}

	@Override
	public String getName() {
		return REACT_CLASS;
	}

	@Override
	protected WebView createViewInstance(ThemedReactContext themedReactContext) {
		WebView webView = new AlibcWebView(themedReactContext);
		webView.getSettings().setJavaScriptEnabled(true);

		/*webView.getSettings().setBuiltInZoomControls(true);
		webView.getSettings().setDisplayZoomControls(false);
		webView.getSettings().setDomStorageEnabled(true);*/

		// Fixes broken full-screen modals/galleries due to body height being 0.
		webView.setLayoutParams(
				new LayoutParams(LayoutParams.MATCH_PARENT,
					LayoutParams.MATCH_PARENT));
		
		return webView;
	}

	@Override
	public Map getExportedCustomDirectEventTypeConstants() {
		return MapBuilder.of(
				"onStateChange", MapBuilder.of("registrationName", "onStateChange"),
				"onTradeResult", MapBuilder.of("registrationName", "onTradeResult"));
	}

	@Override
	public @Nullable Map<String, Integer> getCommandsMap() {
		return MapBuilder.of(
			"goBack", COMMAND_GO_BACK,
			"goForward", COMMAND_GO_FORWARD,
			"reload", COMMAND_RELOAD
		);
	}

	@Override
    public void receiveCommand(WebView root, int commandId, @Nullable ReadableArray args) {
        switch (commandId) {
            case COMMAND_GO_BACK:
				root.goBack();
				break;
			case COMMAND_GO_FORWARD:
				root.goForward();
				break;
			case COMMAND_RELOAD:
				root.reload();
				break;
			default:
            //do nothing!!!!
        }
    }

	@ReactProp(name = "param")
	public void propSetParam(WebView view, ReadableMap param) {
		mModule.showInWebView(view, new AlibcWebViewClient(), param);
	}
}