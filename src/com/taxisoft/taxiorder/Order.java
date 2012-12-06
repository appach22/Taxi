package com.taxisoft.taxiorder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Calendar;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.util.Log;

//import com.taxisoft.taxiorder.MapActivity.TaxiData;

import ru.yandex.yandexmapkit.utils.GeoPoint;

public class Order {

	public String mCustomerName;
	public String mPhoneNumber;
	public String mStreetFrom;
	public String mHouseFrom;
	public String mEntranceFrom;
	public String mLandmarkFrom;
	public String mStreetTo;
	public String mHouseTo;
	public String mEntranceTo;
	public String mLandmarkTo;
	public String mCity;
	public Calendar mTime;
	public GeoPoint mPointFrom;
	public GeoPoint mPointTo;
    XmlPullParser mGeoCodeResponseParser = null;

	public Order()
	{
		// TODO: инициализировать все строки
		
		mTime = Calendar.getInstance();
        XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        mGeoCodeResponseParser = factory.newPullParser();
		} catch (XmlPullParserException e) {
		}
	}
	
	public boolean validate()
	{
		if (!checkAddressIsPreciseEnough(mStreetFrom, mHouseFrom, mLandmarkFrom))
			return false;
		if (!checkAddressIsPreciseEnough(mStreetTo, mHouseTo, mLandmarkTo))
			return false;
		// TODO: в будущем валидация будет не нужна, 
		// т.к. все номера клиента будут на сервере хранится
		if (mPhoneNumber.length() == 0)
			return false;
		
		// TODO: проверка времени заказа
		
	    Thread geoCodeThread = new Thread(new Runnable() {
	        public void run() 
	        {
				if (mStreetFrom.length() != 0 && mHouseFrom.length() != 0)
					mPointFrom = tryGeocode(mCity, mStreetFrom, mHouseFrom);
				if (mStreetTo.length() != 0 && mHouseTo.length() != 0)
					mPointTo = tryGeocode(mCity, mStreetFrom, mHouseFrom);
	        }
	    });
	    geoCodeThread.start();
	    try {
			geoCodeThread.join(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	    
		return true;
	}
	
	private boolean checkAddressIsPreciseEnough(String street, String house, String landmark)
	{
		if (street.length() != 0 && house.length() != 0 || landmark.length() != 0)
			return true;
		return false;
	}
	
	private GeoPoint tryGeocode(String city, String street, String house)
	{
		GeoPoint result = new GeoPoint(0f, 0f);
		String requestString = String.format("%s %s %s", city, street, house);
		try {
			URL geoCodeUrl = new URL("http://geocode-maps.yandex.ru/1.x/?geocode=" + URLEncoder.encode(requestString, "UTF-8"));
			result = parseGeoCodeResponse(geoCodeUrl.openStream());
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	private GeoPoint parseGeoCodeResponse(InputStream response)
	{
		boolean inMember = false;
		int index = 0;
		int found = 0;
		String kinds[] = null;
		String precisions[] = null;
		String positions[] = null;
		GeoPoint result = new GeoPoint(0f, 0f);
		
    	try {
    		mGeoCodeResponseParser.setInput(response, "utf-8");
	        int eventType = mGeoCodeResponseParser.getEventType();
	        while (eventType != XmlPullParser.END_DOCUMENT) 
	        {
	        	if(eventType == XmlPullParser.START_TAG) 
	        	{
	        		String name = mGeoCodeResponseParser.getName();
	        		if (name.equalsIgnoreCase("found"))
	        		{
	        			String sFound = mGeoCodeResponseParser.nextText();
	        			found = Integer.parseInt(sFound);
	        			kinds = new String[found];
	        			precisions = new String[found];
	        			positions = new String[found];
	        		}
	        		else if (name.equalsIgnoreCase("featureMember"))
	        			inMember = true;
	        		else if (inMember && name.equalsIgnoreCase("kind"))
	        			kinds[index] = mGeoCodeResponseParser.nextText();
	        		else if (inMember && name.equalsIgnoreCase("precision"))
	        			precisions[index] = mGeoCodeResponseParser.nextText();
	        		else if (inMember && name.equalsIgnoreCase("pos"))
	        			positions[index] = mGeoCodeResponseParser.nextText();
	        	}
	        	else if(eventType == XmlPullParser.END_TAG) 
	        		if (mGeoCodeResponseParser.getName().equalsIgnoreCase("featureMember"))
	        		{
	        			inMember = false;
	        			if (index < found - 1) ++index;
	        		}
	        	
	        	eventType = mGeoCodeResponseParser.next();
	        }
						
		} catch (XmlPullParserException e) {
			Log.e("xml", e.toString());
		} catch (IOException e) {
			Log.e("xml", e.toString());
		}
		
    	GeoPoint tmpResult = null;
    	for (int i = 0; i < found; ++i)
    	{
        	// Пробегаем по всем объектам типа "house" с точностью "exact"
    		if (kinds[i].equalsIgnoreCase("house") && precisions[i].equalsIgnoreCase("exact"))
    		{
    			// Если такой объект уже был найден ранее, то вываливаемся,
    			// т.к. в ответе присутствует неоднозначность
    			if (tmpResult != null)
    				return result;
    			// Разделяем на широту/долготу
    			String point[] = positions[i].split(" ");
    			// Если успешно разделили - сохраняем во временном результате
    			if (point.length == 2)
    				tmpResult = new GeoPoint(Double.parseDouble(point[1]), Double.parseDouble(point[0]));
    		}
    	}
    	// Если оказались здесь, то либо ничего не нашли, 
    	// либо нашли единственный верный объект
    	if (tmpResult != null)
    		result = tmpResult;
    	
    	return result;
	}

}
