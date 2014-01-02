package com.vingcard.vingcardkeyapp.model;

import java.util.Locale;

import org.joda.time.DateTime;

import com.google.gson.annotations.SerializedName;
import com.vingcard.vingcardkeyapp.storage.VingCardContract.KeyCardDB;

import android.content.ContentValues;
import android.os.Parcel;
import android.os.Parcelable;

public class KeyCard implements Parcelable{
	
	@SerializedName("KeyId")
	public String id;
	
	@SerializedName("SiteId")
	public String hotelId;
	
	@SerializedName("LockId")
	public String roomId;
	
	@SerializedName("KeyData")
	public byte[] key;
	
	@SerializedName("RoomNumber")
	public String roomNumber;
	
	@SerializedName("ValidFrom")
	public DateTime validFrom;
	
	@SerializedName("ValidTo")
	public DateTime validTo;
	
	@SerializedName("Revoked")
	public Boolean revoked;

    @SerializedName("PersonalUrl")
    public String personalUrl;
	
	@SerializedName("Hotel")
	public Hotel hotel;
	
	public transient Boolean checkedIn;
	public transient Boolean hidden;

    /**
	 * @return true if the card is not revoked and is valid at the current time
	 */
	public boolean isActive(){
        return !(revoked ||
                validFrom == null || validTo == null ||
                validFrom.toDateMidnight().isAfterNow() || validTo.plusDays(1).toDateMidnight().isBeforeNow());
	}
	
	/**
	 * @return true if the card is not revoked and will be valid later
	 */
	public boolean isWaitingToBeActive(){
        return !revoked &&
                validFrom != null &&
                validTo != null &&
                validFrom.toDateMidnight().isAfterNow();
	}
	
	/**
	 * @return true if the card is is not valid at the current time or will be valid later
	 */
	public boolean isExpired(){
        return validTo == null ||
               validTo.plusDays(1).toDateMidnight().isBeforeNow();
	}

	public ContentValues getContentValuesForModel(){
		ContentValues values = new ContentValues();
		if (id != null) {
			values.put(KeyCardDB.KEYCARD_ID, id);
		}
		if (hotelId != null) {
			values.put(KeyCardDB.KEYCARD_HOTEL_ID, hotelId.toUpperCase(Locale.getDefault()));
		}
		if (roomId != null) {
			values.put(KeyCardDB.KEYCARD_ROOM_ID, roomId.toUpperCase(Locale.getDefault()));
		}
		if (key != null) {
			values.put(KeyCardDB.KEYCARD_KEY, key);
		}
		if (roomNumber != null) {
			values.put(KeyCardDB.KEYCARD_LABEL, roomNumber);
		}
		if(validFrom != null){
			values.put(KeyCardDB.KEYCARD_VALID_FROM, validFrom.getMillis());	    	
		}
		if(validTo != null){
			values.put(KeyCardDB.KEYCARD_VALID_TO, validTo.getMillis());	    	
		}
		if(revoked != null){
			values.put(KeyCardDB.KEYCARD_REVOKED, revoked);	    	
		}
        if(personalUrl != null){
            values.put(KeyCardDB.KEYCARD_PERSONAL_URL, personalUrl);
        }
		if(checkedIn != null){
			values.put(KeyCardDB.KEYCARD_CHECKED_IN, checkedIn);			
		}
		if(hidden != null){
			values.put(KeyCardDB.KEYCARD_HIDDEN, hidden);			
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
		dest.writeString(id);
		dest.writeString(hotelId);
		dest.writeString(roomId);
		dest.writeByteArray(key == null ? new byte[0] : key);
		dest.writeString(roomNumber);
		dest.writeLong(validFrom.getMillis());
		dest.writeLong(validTo.getMillis());	
		dest.writeValue(revoked);
        dest.writeParcelable(hotel, flags);
	}

	public static final Parcelable.Creator<KeyCard> CREATOR = new Parcelable.Creator<KeyCard>() {
		public KeyCard createFromParcel(Parcel in) {
			KeyCard kc = new KeyCard();
			kc.id = in.readString();
			kc.hotelId = in.readString();
			kc.roomId = in.readString();
            kc.key = in.createByteArray();
			if(kc.key.length == 0){
                kc.key = null;
            }
			kc.roomNumber = in.readString();
			kc.validFrom = new DateTime(in.readLong());
			kc.validTo = new DateTime(in.readLong());
			kc.revoked = (Boolean) in.readValue(null);
            kc.hotel = in.readParcelable(Hotel.class.getClassLoader());
            return kc;
		}

		public KeyCard[] newArray(int size) {
			return new KeyCard[size];
		}
	};
}
