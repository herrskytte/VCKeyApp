package com.vingcard.vingcardkeyapp.util;

public class AppConstants {

    public static final class Uris {

        public static final String BASE_URI_LOGOS = "https://vingcardportalweb.apphb.com";

        // Url to AppHarbor demo server
        //public static final String BASE_URL = "http://vingcardportal.apphb.com/PhoneAppKeysService.svc/";
        //Url to AppHarbor secure development server
        public static final String BASE_URI_REST = "https://vingcardportalapi.apphb.com/PhoneAppKeysService.svc/";
    }

    public static final class KeySync {

        public static final String BROADCAST_ACTION =
                "com.vingcard.vingcardkeyapp.BROADCAST";

        public static final String DATA_KEY =
                "com.vingcard.vingcardkeyapp.DATA_KEY";

        public static final String DATA_ACTION =
                "com.vingcard.vingcardkeyapp.DATA_ACTION";

        public static final String ACTION_NEW_KEY = "NewKey";
        public static final String ACTION_UPDATED_KEY = "UpdatedKey";
        public static final String ACTION_REVOKED_KEY = "RevokedKey";
    }
}
