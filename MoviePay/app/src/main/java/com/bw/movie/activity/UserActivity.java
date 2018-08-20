package com.bw.movie.activity;

import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.bw.movie.R;
import com.bw.movie.api.Api;
import com.bw.movie.api.ApiService;
import com.bw.movie.bean.LoginInfo;
import com.bw.movie.bean.UserRegister;
import com.bw.movie.receiver.MessageReceiver;
import com.bw.movie.utils.AESEncryptUtil;
import com.bw.movie.utils.Constants;
import com.jakewharton.retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory;
import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGPushConfig;
import com.tencent.android.tpush.XGPushManager;
import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import io.reactivex.Flowable;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subscribers.DisposableSubscriber;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class UserActivity extends AppCompatActivity {
    private String pushUrl = "http://172.17.8.100/movieApi/tool/v1/verify/uploadPushToken";

    private IWXAPI api;
    private String token;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);
        //开启信鸽debug日志数据
        XGPushConfig.enableDebug(this, true);
        //注册 获取token
        XGPushManager.registerPush(this, new XGIOperateCallback() {
            @Override
            public void onSuccess(Object o, int i) {
                //token在设备卸载重装的时候有可能会变

                token = o.toString();
                Log.i("xxx", "信鸽推送注册成功，设备token为:" + token);


            }

            @Override
            public void onFail(Object o, int errCode, String msg) {
                Log.i("xxx", "注册失败，错误码：" + errCode + ",错误信息：" + msg);
            }
        });
        //注册广播 数据更新监听器
        MessageReceiver receiver = new MessageReceiver();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.bw.movie.push");
        registerReceiver(receiver, intentFilter);
//-----------------------------------登录注册---------------------------------------------------------

        //  商户APP工程中引入微信JAR包，调用API前，需要先向微信注册您的APPID
        api = WXAPIFactory.createWXAPI(this, null);
        // 将该app注册到微信
        api.registerApp(Constants.APP_ID);//wxb3852e6a6b7d9516


        Button bt_login = (Button) findViewById(R.id.bt_login);
        Button bt_register = (Button) findViewById(R.id.bt_register);
        Button wx_login = (Button) findViewById(R.id.wx_login);
        //登录
        bt_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                login();
            }
        });
        //注册
        bt_register.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                register();
            }
        });
        //微信登录
        wx_login.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // send oauth request
                com.tencent.mm.sdk.modelmsg.SendAuth.Req req = new SendAuth.Req();
                req.scope = "snsapi_userinfo";
                req.state = "wechat_sdk_微信登录";
                api.sendReq(req);

            }
        });


    }

    //注册
    private void register() {
        OkHttpClient client = genericClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Api.url)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)
                .build();
        ApiService apiService = retrofit.create(ApiService.class);
        String nickName = "forever";
        String phone = "18810609754";
        String pwd = "123456";
        String pwd2 = "123456";
        int sex = 1;
        String birthday = "1999-09-09";
        String email = "jnbfeng@163.com";
        //对称加密
        String pssword = AESEncryptUtil.encrypt(pwd);
        String pssword2 = AESEncryptUtil.encrypt(pwd2);
        Log.i("xxx", "注册对称加密pssword:" + pssword + ",注册对称加密pssword2:" + pssword2);
        Flowable<UserRegister> flowable = apiService.register(nickName, phone, pssword, pssword2, sex, birthday, email);
        flowable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSubscriber<UserRegister>() {
                    @Override
                    public void onNext(UserRegister userRegister) {
                        String message = userRegister.getMessage();
                        Log.i("xxx", "注册 message:" + message);
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });

    }

    //登录
    private void login() {
        OkHttpClient client = genericClient();
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(Api.url)
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .client(client)
                .build();
        ApiService apiService = retrofit.create(ApiService.class);

        String phone = "18810609754";
        String pwd = "123456";
        //对称加密
        String pssword = AESEncryptUtil.encrypt(pwd);
        Log.i("xxx", "对称加密pssword:" + pssword);
        Flowable<LoginInfo> flowable = apiService.login(phone, pssword);
        flowable.subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeWith(new DisposableSubscriber<LoginInfo>() {
                    @Override
                    public void onNext(LoginInfo loginInfo) {
                        LoginInfo.ResultBean result = loginInfo.getResult();
                        int userId = result.getUserId();
                        String sessionId = result.getSessionId();
                        Intent intent = new Intent(UserActivity.this, MainActivity.class);
                        //正常是把登录成功后的用户信息保存在SharedPreferences需要的时候取
                        intent.putExtra("userId", userId);
                        intent.putExtra("sessionId", sessionId);
                        startActivity(intent);
                        Log.i("xxx", "登录成功 userId:" + userId + ",登录成功sessionId:" + sessionId);


                        //放在这里纯粹为了拿到 userId,sessionId
                        push(token, userId, sessionId);
                    }

                    @Override
                    public void onError(Throwable t) {

                    }

                    @Override
                    public void onComplete() {

                    }
                });
    }

    //把请求头添加给OkHttpClient的拦截器
    public OkHttpClient genericClient() {
        OkHttpClient httpClient = new OkHttpClient.Builder()
                .addInterceptor(new Interceptor() {
                    @Override
                    public Response intercept(Chain chain) throws IOException {
                        //把Request给Chain
                        Request request = chain.request()
                                .newBuilder()
                                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                                .addHeader("ak", "0110010010001")
                                .build();
                        return chain.proceed(request);
                    }

                })
                .build();

        return httpClient;
    }

    //推送
    private void push(String token, int userId, String sessionId) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(5000, TimeUnit.SECONDS)
                .writeTimeout(5000, TimeUnit.SECONDS)
                .readTimeout(5000, TimeUnit.SECONDS)
                .build();

        //3.x版本post请求换成FormBody 封装键值对参数
        FormBody.Builder builder = new FormBody.Builder();
        builder.add("token", token);
        builder.add("os", "1");
        //添加请求头
        Request request = new Request.Builder()
                .url(pushUrl)
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
                Log.i("xxx", "推送请求 onFailure:" + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String json = response.body().string();
                //成功的回调方法里返回预支付订单信息
                Log.i("xxx", "推送请求成功:" + json);


            }
        });
    }
}
