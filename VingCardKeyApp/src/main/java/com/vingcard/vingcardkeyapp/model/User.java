package com.vingcard.vingcardkeyapp.model;

import com.google.gson.annotations.SerializedName;

public class User {

	@SerializedName("Id")
	public long id;

    @SerializedName("AppType")
    public final long appType = 4000;
	
	@SerializedName("PhoneNumber")
	public String phoneNumber;
	
	@SerializedName("RegistrationId")
	public String registrationId;
	
	@SerializedName("RegistrationCode")
	public String registrationCode;
}
