package com.bw.movie.api;

import com.bw.movie.bean.LoginInfo;
import com.bw.movie.bean.UserRegister;

import io.reactivex.Flowable;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.POST;

/**
 * 1. 类的用途
 * 2. @author forever
 * 3. @date 2018/8/17 14:20
 */


public interface ApiService {
    //注册
    @POST("registerUser")
    @FormUrlEncoded
    Flowable<UserRegister> register(@Field("nickName") String nickName,
                                    @Field("phone") String phone,
                                    @Field("pwd") String pwd,
                                    @Field("pwd2") String pwd2,
                                    @Field("sex") int sex,
                                    @Field("birthday") String birthday,
                                    @Field("email") String email);

    //登录
    @POST("login")
    @FormUrlEncoded
    Flowable<LoginInfo> login(@Field("phone") String phone,
                              @Field("pwd") String pwd);
}
