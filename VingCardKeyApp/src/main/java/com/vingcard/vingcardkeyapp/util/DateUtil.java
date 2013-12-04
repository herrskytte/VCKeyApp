package com.vingcard.vingcardkeyapp.util;

import java.util.Locale;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DateUtil {
	
	public static DateTime deserializeDateTime(String input){
		DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();
		return fmt.parseDateTime(input);
	}

	public static String serializeDateTime(DateTime date) {
		DateTimeFormatter fmt = ISODateTimeFormat.dateTimeNoMillis();
        return date.toString(fmt);
	}
	
	//Test to determine if date is printed with month first in current locale
	private static final boolean IS_DATE_FIRST = DateTimeFormat.mediumDate().print(new DateTime(864000000)).startsWith("1");

	/**
	 * Prints the given date as date and month in local version
	 * Example: NO - "07. aug."	US - "Aug 07"
	 */
	public static String getFormattedDate(LocalDate date) {		
		DateTimeFormatter fmt;
		if(IS_DATE_FIRST){
			fmt = DateTimeFormat.forPattern("dd. MMM");
		}else{
			fmt = DateTimeFormat.forPattern("MMM dd");
		}
		return fmt.print(date);
	}
	
	public static String getFormattedDateLong(LocalDate date) {
		DateTimeFormatter fmt = DateTimeFormat.forPattern("EEE dd. MMMM").withLocale(Locale.getDefault());
		String formatted = date.toString(fmt);
		return new String(formatted.substring(0,1).toUpperCase(Locale.getDefault()) + formatted.substring(1));
	}
	
	public static String getFormattedTime(LocalTime time) {
		DateTimeFormatter fmt = DateTimeFormat.shortTime().withLocale(Locale.getDefault());
		return time.toString(fmt);
	}
}
