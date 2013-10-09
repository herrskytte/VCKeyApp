package com.vingcard.vingcardkeyapp.model;

import com.google.gson.annotations.SerializedName;

public class DoorEvent {
	public static final String TYPE_LOCK = "LOCKEVENT";
	public static final String TYPE_CHECKIN = "CHECKIN";
	public static final String TYPE_CHECKOUT = "CHECKOUT";
	
	public transient String eventIndex;

	@SerializedName("SiteId")
	public String hotelId;
	
	@SerializedName("LockId")
	public String roomId;
	
	@SerializedName("CardId")
	public String cardId;
	
	@SerializedName("DeltaTime")
	public long deltaTime;
	
	@SerializedName("StatusData")
	public String statusData;
	
	@SerializedName("EventType")
	public String eventType;

	public boolean isHighPriority() {
        return eventType != null &&
                (eventType.equals(TYPE_CHECKIN) ||
                        eventType.equals(TYPE_CHECKOUT));
    }

}
