package com.vingcard.vingcardkeyapp.storage;

import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Contract class for interacting with Provider. Unless
 * otherwise noted, all time-based fields are milliseconds since epoch and can
 * be compared against {@link System#currentTimeMillis()}.
 * <p>
 * The backing {@link android.content.ContentProvider} assumes that {@link Uri}
 * are generated using stronger {@link String} identifiers, instead of
 * {@code int} {@link BaseColumns#_ID} values, which are prone to shuffle during
 * sync.
 */
public class VingCardContract {

    public static final String CONTENT_AUTHORITY = "com.vingcard.vingcardkeyapp";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    
    private static final String PATH_KEYCARD = "keycard";
    private static final String PATH_WITH_HOTEL = "with_hotel";
    private static final String PATH_DOOREVENT = "doorevent";
    private static final String PATH_HOTEL = "hotel";

    interface KeyCardColumns {
    	String KEYCARD_ID = "keycard_id";
    	String KEYCARD_HOTEL_ID = "hotel_id";
    	String KEYCARD_ROOM_ID = "room_id";
    	String KEYCARD_KEY = "key";
    	String KEYCARD_LABEL = "label";
    	String KEYCARD_VALID_FROM = "valid_from";
    	String KEYCARD_VALID_TO = "valid_to";
    	String KEYCARD_REVOKED = "revoked";
    	String KEYCARD_PERSONAL_URL = "personal_url";
    	String KEYCARD_CHECKED_IN = "checked_in";
    	String KEYCARD_HIDDEN = "hidden";
    }

    public static class KeyCardDB implements KeyCardColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_KEYCARD).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.vingcard.keycard";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.vingcard.keycard";
        
        public static final String WHERE_CORRECT_KEY = KEYCARD_HOTEL_ID + "=? AND " + 
        											   KEYCARD_ROOM_ID + "=? AND " + 
        											   KEYCARD_REVOKED + "!=1 AND " +
        											   KEYCARD_VALID_FROM +	"<? AND " +
        											   KEYCARD_VALID_TO + ">?";

        /** Default "ORDER BY" clause. */
        public static final String DEFAULT_SORT = KeyCardColumns.KEYCARD_VALID_FROM + " DESC";
        
        /** Bygg Uri for ønsket keycard */
        public static Uri buildKeyCardUri(String keycardId) {
            return CONTENT_URI.buildUpon().appendPath(keycardId).build();
        }

        /** Uri for all keycards including hotel data */
        public static Uri buildKeyCardsWithHotelUri() {
            return CONTENT_URI.buildUpon().appendPath(PATH_WITH_HOTEL).build();
        }

        public static String getKeyCardId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }    

    interface DoorEventColumns {
    	String EVENT_HOTEL_ID = "hotel_id";
    	String EVENT_ROOM_ID = "room_id";
    	String EVENT_CARD_ID = "keycard_id";
    	String EVENT_TIMESTAMP = "timestamp";
    	String EVENT_DATA = "data";
    	String EVENT_TYPE = "type";
    }
    
    public static class DoorEventDB implements DoorEventColumns, BaseColumns {
    	public static final Uri CONTENT_URI =
    			BASE_CONTENT_URI.buildUpon().appendPath(PATH_DOOREVENT).build();
    	
    	public static final String CONTENT_TYPE =
    			"vnd.android.cursor.dir/vnd.vingcard.doorevent";
    	public static final String CONTENT_ITEM_TYPE =
    			"vnd.android.cursor.item/vnd.vingcard.doorevent";
    	
    	/** Default "ORDER BY" clause. */
    	public static final String DEFAULT_SORT = DoorEventColumns.EVENT_TIMESTAMP + " ASC";
    	
    	/** Bygg Uri for �nsket keycard */
    	public static Uri buildDoorEventUri(String doorEventIndex) {
    		return CONTENT_URI.buildUpon().appendPath(doorEventIndex).build();
    	}
    	
    	public static String getDoorEventIndex(Uri uri) {
    		return uri.getPathSegments().get(1);
    	}
    }    
    
    interface HotelColumns {
    	String HOTEL_ID = "hotelId";
    	String HOTEL_NAME = "name";
    	String HOTEL_ADDRESS = "address";
    	String HOTEL_PHONE = "phone";
    	String HOTEL_EMAIL = "email";
    	String HOTEL_WEBSITE = "website";
    	String HOTEL_LOGO_URL = "logoUrl";
    }

    public static class HotelDB implements HotelColumns, BaseColumns {
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_HOTEL).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.vingcard.hotel";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.vingcard.hotel";
        
        /** Bygg Uri for �nsket hotell */
        public static Uri buildHotelUri(String hotelId) {
            return CONTENT_URI.buildUpon().appendPath(hotelId).build();
        }

        public static String getHotelId(Uri uri) {
            return uri.getPathSegments().get(1);
        }
    }

    private VingCardContract() {
    }
}
