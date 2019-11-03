package com.divakrishnam.actspot.api;

import com.divakrishnam.actspot.model.Result;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface APIService {

    @FormUrlEncoded
    @POST("login.php")
    Call<Result> userLogin(
            @Field("username") String username,
            @Field("password") String password
    );

    @Multipart
    @POST("insert.php")
    Call<Result> insertData(
            @Part("id_user") RequestBody id_user,
            @Part("latitude") RequestBody latitude,
            @Part("longtitude") RequestBody longtitude,
            @Part("date_time") RequestBody date_time,
            @Part("imei") RequestBody imei,
            @Part MultipartBody.Part picture
    );
}
