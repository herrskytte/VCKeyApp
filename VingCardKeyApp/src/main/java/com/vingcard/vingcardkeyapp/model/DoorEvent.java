package com.vingcard.vingcardkeyapp.model;

import com.google.gson.annotations.SerializedName;
import com.vingcard.vingcardkeyapp.util.AppConstants;

public class DoorEvent {
	
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
                (eventType.equals(AppConstants.EventTypes.TYPE_CHECKIN) ||
                        eventType.equals(AppConstants.EventTypes.TYPE_CHECKOUT));
    }

}
