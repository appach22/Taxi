package com.taxisoft.taxiorder;

import java.io.ByteArrayInputStream;
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
    XmlPullParser mXmlResponseParser = null;
    
    private int mID = 0;
    private int mState = -1;
    
    public static final int STATE_UNDEFINED 		= -1;
    public static final int STATE_NO_SUCH_ORDER 	= 0;
    public static final int STATE_ACCEPTED			= 1;
    public static final int STATE_LOOKING_FOR_CAR	= 2;
    public static final int STATE_CAR_ASSIGNED 		= 3;
    public static final int STATE_CAR_APPROACHES	= 4;
    public static final int STATE_CAR_ARRIVED 		= 5;
    public static final int STATE_ON_THE_WAY 		= 6;
    public static final int STATE_CLOSED 			= 7;
    public static final int STATE_CAR_NOT_FOUND		= 8;
    public static final int STATE_CANCELLED			= 9;
    public static final int STATE_EDITED	 		= 10;
    public static final int STATE_DEBUG_REMOVE 		= 11;

    public static final int STATE_NAMES[] = {
    	R.string.state_no_such_order,
    	R.string.state_accepted,
    	R.string.state_looking_for_car,
    	R.string.state_car_assigned,
    	R.string.state_car_approaches,
    	R.string.state_car_arrived,
    	R.string.state_on_the_way,
    	R.string.state_closed,
    	R.string.state_car_not_found,
    	R.string.state_cancelled,
    	R.string.state_edited
   	}; 
    
	public Order()
	{
		// TODO: инициализировать все строки
		
		mID = 0;
		mTime = Calendar.getInstance();
		mPointFrom = new GeoPoint(0f, 0f);
		mPointTo = new GeoPoint(0f, 0f);
        XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        mXmlResponseParser = factory.newPullParser();
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
			URL geoCodeUrl = new URL(MapActivity.URL_YANDEX_GEOCODER + "?geocode=" + URLEncoder.encode(requestString, "UTF-8"));
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
    		mXmlResponseParser.setInput(response, "utf-8");
	        int eventType = mXmlResponseParser.getEventType();
	        while (eventType != XmlPullParser.END_DOCUMENT) 
	        {
	        	if(eventType == XmlPullParser.START_TAG) 
	        	{
	        		String name = mXmlResponseParser.getName();
	        		if (name.equalsIgnoreCase("found"))
	        		{
	        			String sFound = mXmlResponseParser.nextText();
	        			found = Integer.parseInt(sFound);
	        			kinds = new String[found];
	        			precisions = new String[found];
	        			positions = new String[found];
	        		}
	        		else if (name.equalsIgnoreCase("featureMember"))
	        			inMember = true;
	        		else if (inMember && name.equalsIgnoreCase("kind"))
	        			kinds[index] = mXmlResponseParser.nextText();
	        		else if (inMember && name.equalsIgnoreCase("precision"))
	        			precisions[index] = mXmlResponseParser.nextText();
	        		else if (inMember && name.equalsIgnoreCase("pos"))
	        			positions[index] = mXmlResponseParser.nextText();
	        	}
	        	else if(eventType == XmlPullParser.END_TAG) 
	        		if (mXmlResponseParser.getName().equalsIgnoreCase("featureMember"))
	        		{
	        			inMember = false;
	        			if (index < found - 1) ++index;
	        		}
	        	
	        	eventType = mXmlResponseParser.next();
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

	public boolean submit()
	{
		boolean result = false;
		
		if (mStreetFrom.length() != 0 && mHouseFrom.length() != 0)
			mPointFrom = tryGeocode(mCity, mStreetFrom, mHouseFrom);
		if (mStreetTo.length() != 0 && mHouseTo.length() != 0)
			mPointTo = tryGeocode(mCity, mStreetTo, mHouseTo);

		String requestString = String.format("?phone_number=%s&customer_name=%s&"+
											 "street_from=%s&house_from=%s&"+
											 "entrance_from=%s&landmark_from=%s&"+
											 "street_to=%s&house_to=%s&"+
											 "entrance_to=%s&landmark_to=%s&"+
											 "time=%s&lat_from=%s&"+
											 "lon_from=%s&lat_to=%s&lon_to=%s",
											 mPhoneNumber, mCustomerName,
											 mStreetFrom, mHouseFrom,
											 mEntranceFrom, mLandmarkFrom,
											 mStreetTo, mHouseTo,
											 mEntranceTo, mLandmarkTo,
											 mTime, mPointFrom.getLat(),
											 mPointFrom.getLon(), mPointTo.getLat(),
											 mPointTo.getLon());

		try
		{
			URL placeOrderUrl = new URL(MapActivity.URL_PLACE_ORDER + URLEncoder.encode(requestString, "UTF-8"));
			result = parsePlaceOrderResponse(new ByteArrayInputStream("<?xml version=\"1.0\" standalone=\"yes\"?><result error=\"0\">777</result>".getBytes())/*placeOrderUrl.openStream()*/);
/*		} catch (MalformedURLException e) {
			e.printStackTrace();
			return false;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} catch (XmlPullParserException e) {
			e.printStackTrace();
			return false;*/
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		return result;
	}
	
	private boolean parsePlaceOrderResponse(InputStream response) throws XmlPullParserException, IOException
	{
		mXmlResponseParser.setInput(response, "utf-8");
        int eventType = mXmlResponseParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) 
        {
        	if(eventType == XmlPullParser.START_TAG) 
        	{
        		String name = mXmlResponseParser.getName();
        		if (name.equalsIgnoreCase("result"))
        		{
        			for (int i = 0; i < mXmlResponseParser.getAttributeCount(); ++i)
        				if (mXmlResponseParser.getAttributeName(i).equalsIgnoreCase("error"))
        				{
        					int code = Integer.parseInt(mXmlResponseParser.getAttributeValue(i));
        					// TODO: проверять и возвращать код ошибки
        					if (code != 0)
        						return false;
        					mID = Integer.parseInt(mXmlResponseParser.nextText());
        				}
        		}
        	}
        	eventType = mXmlResponseParser.next();
        }
		return true;
	}
	
	public int getID() { return mID; }

	public int getState() { return mState; }
	
	@Override
	public boolean equals(Object to)
	{
		if (mID == ((Order)to).getID())
			return true;
		return false;
	}

	@Override
	public String toString()
	{
		return mStreetFrom + " -> " + mStreetTo;
	}
	
	private static final String fakeResponse = "<?xml version=\"1.0\" standalone=\"yes\"?>" +
			"<order error=\"0\">" + 
			"<id>%s</id>" +
			"<state>10</state>" +
			"<phone_number>1234567</phone_number>" +
			"<customer_name>Сергей</customer_name>" +
			"<street_from>Семеновская</street_from>" +
			"<house_from >21</house_from >" +
			"<entrance_from></entrance_from>" +
			"<landmark_from></landmark_from>" +
			"<street_to>Ленина</street_to>" +
			"<house_to >74</house_to >" +
			"<entrance_to></entrance_to>" +
			"<landmark_to></landmark_to>" +
			"<time>0000-00-00 00:00:00</time>" +
			"<lat_from>51.735621</lat_from>" +
			"<lon_from>36.185361</lon_from>" +
			"<lat_to>51.743976</lat_to>" +
			"<lon_to>36.193302</lon_to>" +
			"</order>"; 

	final static String LOG_TAG = "OrderStateService";
	private static Order parseOrderDataResponse(InputStream response) throws XmlPullParserException, IOException
	{
		XmlPullParser xmlResponseParser = null;
        XmlPullParserFactory factory;
        Order order = new Order();
		try {
			factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        xmlResponseParser = factory.newPullParser();
		} catch (XmlPullParserException e) {
			return null;
		}
		xmlResponseParser.setInput(response, "utf-8");
        int eventType = xmlResponseParser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) 
        {
        	if(eventType == XmlPullParser.START_TAG) 
        	{
        		String name = xmlResponseParser.getName();
        		if (name.equalsIgnoreCase("order"))
        		{
        			for (int i = 0; i < xmlResponseParser.getAttributeCount(); ++i)
        				if (xmlResponseParser.getAttributeName(i).equalsIgnoreCase("error"))
        				{
        					int code = Integer.parseInt(xmlResponseParser.getAttributeValue(i));
        					// TODO: проверять и возвращать код ошибки
        					if (code != 0)
        						return order;
        				}
        		}
        		else if (name.equalsIgnoreCase("id"))
        			order.mID = Integer.parseInt(xmlResponseParser.nextText());
        		else if (name.equalsIgnoreCase("state"))
        			order.mState = Integer.parseInt(xmlResponseParser.nextText());
        		else if (name.equalsIgnoreCase("street_from"))
        			order.mStreetFrom = xmlResponseParser.nextText();
        		else if (name.equalsIgnoreCase("house_from"))
        			order.mHouseFrom = xmlResponseParser.nextText();
        		else if (name.equalsIgnoreCase("entrance_from"))
        			order.mEntranceFrom = xmlResponseParser.nextText();
        		else if (name.equalsIgnoreCase("landmark_from"))
        			order.mLandmarkFrom = xmlResponseParser.nextText();
        		else if (name.equalsIgnoreCase("lat_from"))
        			order.mPointFrom.setLat(Double.parseDouble(xmlResponseParser.nextText()));
        		else if (name.equalsIgnoreCase("lon_from"))
        			order.mPointFrom.setLon(Double.parseDouble(xmlResponseParser.nextText()));
        		else if (name.equalsIgnoreCase("street_to"))
        			order.mStreetTo = xmlResponseParser.nextText();
        		else if (name.equalsIgnoreCase("house_to"))
        			order.mHouseTo = xmlResponseParser.nextText();
        		else if (name.equalsIgnoreCase("entrance_to"))
        			order.mEntranceTo = xmlResponseParser.nextText();
        		else if (name.equalsIgnoreCase("landmark_to"))
        			order.mLandmarkTo = xmlResponseParser.nextText();
        		else if (name.equalsIgnoreCase("lat_to"))
        			order.mPointTo.setLat(Double.parseDouble(xmlResponseParser.nextText()));
        		else if (name.equalsIgnoreCase("lon_to"))
        			order.mPointTo.setLon(Double.parseDouble(xmlResponseParser.nextText()));
        		else if (name.equalsIgnoreCase("phone_number"))
        			order.mPhoneNumber = xmlResponseParser.nextText();
        		else if (name.equalsIgnoreCase("customer_name"))
        			order.mCustomerName = xmlResponseParser.nextText();
        	}
        	eventType = xmlResponseParser.next();
        }
        return order;
	}

	public static Order getFromServer(String id)
	{
		Order order = null;
		try
		{
			URL orderDataUrl = new URL(MapActivity.URL_ORDER_DATA + "?id=" + id);
			order = parseOrderDataResponse(new ByteArrayInputStream(String.format(fakeResponse, id).getBytes())/*orderDataUrl.openStream()*/);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	
		return order;
	}
	
	public static Order getFromServer(int id)
	{
		return getFromServer(String.valueOf(id));
	}
}
