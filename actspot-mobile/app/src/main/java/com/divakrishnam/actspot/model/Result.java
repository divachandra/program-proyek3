package com.divakrishnam.actspot.model;

import com.divakrishnam.actspot.model.User;
import com.google.gson.annotations.SerializedName;

public class Result {
    @SerializedName("status")
    private String status;

    @SerializedName("message")
    private String message;

    @SerializedName("data")
    private User user;

    public Result(String status, String message, User user) {
        this.status = status;
        this.message = message;
        this.user = user;
    }

    public String getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public User getUser() {
        return user;
    }
}
