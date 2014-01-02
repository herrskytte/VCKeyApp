package com.vingcard.vingcardkeyapp.model;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import com.google.gson.annotations.SerializedName;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.HotelDB;
import com.vingcard.vingcardkeyapp.util.AppConstants;

import java.util.Locale;

public class Hotel implements Parcelable{

	@SerializedName("SiteId")
	public String hotelId;
	
	@SerializedName("Name")
	public String name;
	
	@SerializedName("Address")
	public String address;
	
	@SerializedName("Phone")
	public String phone;
	
	@SerializedName("Email")
	public String email;
	
	@SerializedName("Website")
	public String website;
	
	@SerializedName("LogoUrl")
    public String logoUrl;

	public ContentValues getContentValuesForModel(){
		ContentValues values = new ContentValues();
		if (hotelId != null) {
			values.put(HotelDB.HOTEL_ID, hotelId.toUpperCase(Locale.getDefault()));
		}
		if (name != null) {
			values.put(HotelDB.HOTEL_NAME, name);
		}
		if (address != null) {
			values.put(HotelDB.HOTEL_ADDRESS, address);
		}
		if (phone != null) {
			values.put(HotelDB.HOTEL_PHONE, phone);
		}
		if (email != null) {
			values.put(HotelDB.HOTEL_EMAIL, email);
		}
		if (website != null) {
			values.put(HotelDB.HOTEL_WEBSITE, website);
		}
		if (logoUrl != null) {
			values.put(HotelDB.HOTEL_LOGO_URL, logoUrl);
		}
		
		return values;
	}

    //Parcelable methods

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(hotelId);
        dest.writeString(name);
        dest.writeString(address);
        dest.writeString(phone);
        dest.writeString(email);
        dest.writeString(website);
        dest.writeString(logoUrl);
    }

    public static final Parcelable.Creator<Hotel> CREATOR = new Parcelable.Creator<Hotel>() {
        public Hotel createFromParcel(Parcel in) {
            Hotel h = new Hotel();
            h.hotelId = in.readString();
            h.name = in.readString();
            h.address = in.readString();
            h.phone = in.readString();
            h.email = in.readString();
            h.website = in.readString();
            h.logoUrl = in.readString();
            return h;
        }

        public Hotel[] newArray(int size) {
            return new Hotel[size];
        }
    };

    public static String CreateFullLogoUrl(String relativeUrl){
        if(TextUtils.isEmpty(relativeUrl)){
            return null;
        }
        return AppConstants.Uris.BASE_URI_LOGOS + relativeUrl;
    }
}
