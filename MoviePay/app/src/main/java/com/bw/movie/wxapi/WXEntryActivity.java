package com.bw.movie.wxapi;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.bw.movie.utils.Constants;
import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

//微信登录授权回调
public class WXEntryActivity extends Activity implements IWXAPIEventHandler {
    String url = "http://172.17.8.100/movieApi/user/v1/weChatBindingLogin";

    // IWXAPI
    private IWXAPI api;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Toast.makeText(this, "走:", Toast.LENGTH_SHORT).show();

        // WXAPIFactory IWXAPI
        api = WXAPIFactory.createWXAPI(this, Constants.APP_ID, true);
        api.registerApp(Constants.APP_ID);
        api.handleIntent(getIntent(), this);
    }


    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        api.handleIntent(intent, this);
    }

    // ΢
    @Override
    public void onReq(BaseReq req) {

    }

    @Override
    public void onResp(BaseResp resp) {
        //发送授权成功
        SendAuth.Resp sendResp = (SendAuth.Resp) resp;
        if (sendResp != null) {
            String code = sendResp.code;//临时票据
            Toast.makeText(this, "code:" + code, Toast.LENGTH_SHORT).show();
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(5000, TimeUnit.SECONDS)
                    .writeTimeout(5000, TimeUnit.SECONDS)
                    .readTimeout(5000, TimeUnit.SECONDS)
                    .build();

            //3.x版本post请求换成FormBody 封装键值对参数
            FormBody.Builder builder = new FormBody.Builder();
            builder.add("code", code);
            Request request = new Request.Builder()
                    .url(url)
                    .post(builder.build())
                    .build();
            Call call = client.newCall(request);
            call.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {

                }

                @Override
                public void onResponse(Call call, final Response response) throws IOException {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            try {
                                String json = response.body().string();
                                Toast.makeText(WXEntryActivity.this, "传递code成功:"+json, Toast.LENGTH_SHORT).show();

                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                        }
                    });

                }
            });

        }


    }


}