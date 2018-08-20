package com.bw.movie.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.alipay.sdk.app.PayTask;
import com.bw.movie.R;
import com.bw.movie.bean.PayResult;
import com.bw.movie.utils.Constants;
import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class PayActivity extends AppCompatActivity {
    private static final int WX_PAY_OK = 0;
    private static final int ALI_PAY_OK = 1;
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                //微信
                case WX_PAY_OK:
                    String json = (String) msg.obj;
                    Toast.makeText(PayActivity.this, "支付信息返回", Toast.LENGTH_SHORT).show();
                    try {
                        JSONObject jsonObject = new JSONObject(json);
                        //拿预支付结果信息给微信
                        PayReq request = new PayReq();
                        request.appId = jsonObject.getString("appId");
                        request.partnerId = jsonObject.getString("partnerId");
                        request.prepayId = jsonObject.getString("prepayId");
                        request.packageValue = jsonObject.getString("packageValue");
                        request.nonceStr = jsonObject.getString("nonceStr");
                        request.timeStamp = jsonObject.getString("timeStamp");
                        request.sign = jsonObject.getString("sign");
                        api.sendReq(request);

                        Toast.makeText(PayActivity.this, "支付成功", Toast.LENGTH_SHORT).show();
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.i("xxx", e.getMessage());
                    }

                    break;
                case ALI_PAY_OK:
                    //支付宝
                    PayResult payResult = new PayResult((String) msg.obj);
                    /**
                     * 同步返回的结果必须放置到服务端进行验证（验证的规则请看https://doc.open.alipay.com/doc2/
                     * detail.htm?spm=0.0.0.0.xdvAU6&treeId=59&articleId=103665&
                     * docType=1) 建议商户依赖异步通知
                     */
                    String resultInfo = payResult.getResult();// 同步返回需要验证的信息
                    Log.i("TAG", resultInfo);
                    String resultStatus = payResult.getResultStatus();
                    Log.i("TAG", resultStatus);
                    // 判断resultStatus 为“9000”则代表支付成功，具体状态码代表含义可参考接口文档
                    if (TextUtils.equals(resultStatus, "9000")) {
                        Toast.makeText(PayActivity.this, "支付成功", Toast.LENGTH_SHORT).show();
                    } else {
                        // 判断resultStatus 为非"9000"则代表可能支付失败
                        // "8000"代表支付结果因为支付渠道原因或者系统原因还在等待支付结果确认，最终交易是否成功以服务端异步通知为准（小概率状态）
                        if (TextUtils.equals(resultStatus, "8000")) {
                            Toast.makeText(PayActivity.this, "支付结果确认中", Toast.LENGTH_SHORT).show();

                        } else {
                            // 其他值就可以判断为支付失败，包括用户主动取消支付，或者系统返回的错误
                            Toast.makeText(PayActivity.this, "支付失败", Toast.LENGTH_SHORT).show();

                        }
                    }
                    break;
            }

        }
    };
    private IWXAPI api;
    private int userId;
    private String sessionId;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pay);
//  商户APP工程中引入微信JAR包，调用API前，需要先向微信注册您的APPID
        api = WXAPIFactory.createWXAPI(this, null);
        // 将该app注册到微信
        api.registerApp(Constants.APP_ID);//wxb3852e6a6b7d9516

        Intent intent = getIntent();
        final String orderId = intent.getStringExtra("orderId");
        userId = intent.getIntExtra("userId",0);
        sessionId = intent.getStringExtra("sessionId");
        Button bt_wxpay = (Button) findViewById(R.id.bt_wxpay);
        Button bt_alipay = (Button) findViewById(R.id.bt_alipay);

        bt_wxpay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //微信支付 传入订单号
                wechatPay(orderId);

            }
        });
        bt_alipay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //支付宝支付
                aliPay(orderId);
            }
        });

    }

    //支付宝支付
    private void aliPay(String orderId) {
        String path = "http://172.17.8.100/movieApi/movie/v1/verify/pay";
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5000, TimeUnit.SECONDS)
                .writeTimeout(5000, TimeUnit.SECONDS)
                .readTimeout(5000, TimeUnit.SECONDS)
                .build();

        //3.x版本post请求换成FormBody 封装键值对参数
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("payType", "2");
        builder.add("orderId", orderId);
        Request request = new Request.Builder()
                .url(path)
                .addHeader("userId", userId+"")
                .addHeader("sessionId", sessionId)
                .addHeader("ak", "0110010010001")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(builder.build())
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("xxx", "PayActivity onFailure:" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                // 获取必须来自服务端 预支付结果订单信息
                String signResult = response.body().string();
                Log.i("xxx", "PayActivity onResponse:" + signResult.toString());
                // 构造PayTask 支付宝对象
                PayTask alipay = new PayTask(PayActivity.this);
                // 调用支付接口，获取支付结果
                String result = alipay.pay(signResult, true);

                Message msg = new Message();
                msg.what = ALI_PAY_OK;
                msg.obj = result;
                handler.sendMessage(msg);

            }
        });
    }

    //微信支付
    private void wechatPay(String orderId) {
        String path = "http://172.17.8.100/movieApi/movie/v1/verify/pay";
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5000, TimeUnit.SECONDS)
                .writeTimeout(5000, TimeUnit.SECONDS)
                .readTimeout(5000, TimeUnit.SECONDS)
                .build();

        //3.x版本post请求换成FormBody 封装键值对参数
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("payType", "1");
        builder.add("orderId", orderId);


        Request request = new Request.Builder()
                .url(path)
                .addHeader("userId", userId+"")
                .addHeader("sessionId", sessionId)
                .addHeader("ak", "0110010010001")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(builder.build())
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("xxx", "PayActivity onFailure:" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {

                //预支付结果信息
                String json = response.body().string();
                Log.i("xxx", "PayActivity onResponse:" + json.toString());
                Message message = new Message();
                message.what = WX_PAY_OK;
                message.obj = json;
                handler.sendMessage(message);

            }
        });


    }
}
