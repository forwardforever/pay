package com.bw.movie.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.bw.movie.R;
import com.bw.movie.utils.MD5Utils;

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

public class MainActivity extends AppCompatActivity {
    private String path = "http://172.17.8.100/movieApi/movie/v1/verify/buyMovieTicket";

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == 0) {
                String json = (String) msg.obj;
                try {
                    JSONObject jsonObject = new JSONObject(json);
                    String orderId = jsonObject.getString("orderId");
                    Intent intent = new Intent(MainActivity.this, PayActivity.class);
                    intent.putExtra("orderId", orderId);
                    //正常是把登录成功后的用户信息保存在SharedPreferences需要的时候取
                    intent.putExtra("userId", userId);
                    intent.putExtra("sessionId", sessionId);
                    startActivity(intent);

                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }
        }
    };
    private int userId;
    private String sessionId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //-----------------------------提交订单----------------------------------
        Intent intent = getIntent();
        userId = intent.getIntExtra("userId", 0);
        sessionId = intent.getStringExtra("sessionId");
        Button bt_buy = (Button) findViewById(R.id.bt_buy);
        bt_buy.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                buy();
            }
        });

    }

    //下单
    private void buy() {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5000, TimeUnit.SECONDS)
                .writeTimeout(5000, TimeUnit.SECONDS)
                .readTimeout(5000, TimeUnit.SECONDS)
                .build();
        String scheduleId = "1";
        String amount = "3";
        //对这个字符串进行MD5运算
        String sign = MD5Utils.getSign(userId + scheduleId + amount + "movie");
        Log.i("xxx", "进行MD5运算sign:"+sign);
        //3.x版本post请求换成FormBody 封装键值对参数
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("scheduleId", scheduleId);
        builder.add("amount", amount);
        builder.add("sign", sign);


        Request request = new Request.Builder()
                .url(path)
                .addHeader("userId", userId + "")
                .addHeader("sessionId", sessionId)
                .addHeader("ak", "0110010010001")
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .post(builder.build())
                .build();
        Call call = client.newCall(request);
        call.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.i("xxx", "onFailure:" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //成功的回调方法里返回预支付订单信息
                String json = response.body().string();
                Log.i("xxx", "下单成功:" + json.toString());
                Message message = new Message();
                message.what = 0;
                message.obj = json;
                handler.sendMessage(message);

            }
        });

    }
}
