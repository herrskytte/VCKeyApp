package com.vingcard.vingcardkeyapp.model;

import java.lang.reflect.Field;
import java.util.Locale;

import com.vingcard.vingcardkeyapp.R;
import com.vingcard.vingcardkeyapp.R.drawable;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

/**
 * POJO
 *
 */
public class Country implements Parcelable{

	private String code;
	private String name;
	private String phoneCode;

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public String getPhoneCode() {
		return phoneCode;
	}
	
	public void setPhoneCode(String phoneCode) {
		this.phoneCode = phoneCode;
	}	
	
	/**
	 * The drawable image name has the format "flag_$countryCode". We need to
	 * load the drawable dynamically from country code. Code from
	 * http://stackoverflow.com/questions/3042961/how-can-i-get-the-resource-id-of-an-image-if-i-know-its-name
	 */
	public int getImageResId() {
		String drawableName = "flag_" + getCode().toLowerCase(Locale.ENGLISH);
		try {
			Class<drawable> res = R.drawable.class;
			Field field = res.getField(drawableName);
            return field.getInt(null);
		} catch (Exception e) {
			Log.e("COUNTRYPICKER", "Failure to get drawable id.", e);
		}
		return -1;
	}
	
	//PARCELABLE METHODS
	public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel out, int flags) {
        out.writeString(code);
        out.writeString(name);
        out.writeString(phoneCode);
    }

    public static final Parcelable.Creator<Country> CREATOR
            = new Parcelable.Creator<Country>() {
        public Country createFromParcel(Parcel in) {
            return new Country(in);
        }

        public Country[] newArray(int size) {
            return new Country[size];
        }
    };
    
    private Country(Parcel in) {
        code = in.readString();
        name = in.readString();
        phoneCode = in.readString();
    }


}