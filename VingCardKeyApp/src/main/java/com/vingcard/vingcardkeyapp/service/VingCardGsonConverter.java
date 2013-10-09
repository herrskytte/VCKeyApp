package com.vingcard.vingcardkeyapp.service;

import java.lang.reflect.Type;

import org.joda.time.DateTime;
import org.springframework.http.converter.json.GsonHttpMessageConverter;

import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.vingcard.vingcardkeyapp.util.DateUtil;

public class VingCardGsonConverter extends GsonHttpMessageConverter {

	public VingCardGsonConverter(){
		super();
		setGson(createGson());
	}
	
	private class DateTimeSerializer implements JsonSerializer<DateTime> {
		@Override
		public JsonElement serialize(DateTime src, Type typeOfSrc, JsonSerializationContext context) {
			return new JsonPrimitive(DateUtil.serializeDateTime(src));
		}
	}

	private class DateTimeDeserializer implements JsonDeserializer<DateTime> {
		public DateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			return DateUtil.deserializeDateTime(json.getAsString());
		}
	}
	
	private class Base64Deserializer implements JsonDeserializer<byte[]> {
		public byte[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context)
				throws JsonParseException {
			return Base64.decode(json.getAsString(), Base64.DEFAULT);
		}
	}
	
	public Gson createGson(){
		GsonBuilder gson = new GsonBuilder();		
		gson.registerTypeAdapter(DateTime.class, new DateTimeSerializer());
		gson.registerTypeAdapter(DateTime.class, new DateTimeDeserializer());
		gson.registerTypeAdapter(byte[].class, new Base64Deserializer());
		return gson.create();
	}
}
