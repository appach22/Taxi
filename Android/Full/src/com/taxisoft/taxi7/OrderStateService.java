package com.taxisoft.taxi7;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

public class OrderStateService extends Service
{
	ArrayList<Order> mOrders;
    String mStoredOrdersIDs[];
    XmlPullParser mStateResponseParser;
    URL mStateUrl;
    int mFakeState;
    NotificationManager mNotificationManager;
    Timer mGetOrdersTimer; 
    boolean mPreloadingFinished;

    public final static String BROADCAST_ACTION = "com.taxisoft.taxiorder.OrderStateService"; 
    		
    		
	@Override
	public IBinder onBind(Intent arg0)
	{
		return null;
	}

	final String LOG_TAG = "OrderStateService";
	
	private int getOrderState(String id)
	{
		try
		{
			//mStateResponseParser.setInput(mStateUrl.openStream(), "UTF-8");
			mStateResponseParser.setInput(new ByteArrayInputStream(("<?xml version=\"1.0\" standalone=\"yes\"?><response><state>" + Integer.toString(mFakeState - 1) + "</state></response>").getBytes()), "UTF-8");
	        int eventType = mStateResponseParser.getEventType();
	        while (eventType != XmlPullParser.END_DOCUMENT) {
	        	if(eventType == XmlPullParser.START_TAG) {
	        		if (mStateResponseParser.getName().equalsIgnoreCase("state"))
	        			return Integer.parseInt(mStateResponseParser.nextText());
	        	}
	        	eventType = mStateResponseParser.nextTag();
	        }

		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (XmlPullParserException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
	}
	
	private void getAllOrdersState(boolean isFirstTime)
	{
		Intent intentForBroadcast = new Intent(BROADCAST_ACTION);
		for (int i = 0; i < mOrders.size(); ++i)
		{
			Order order = mOrders.get(i);
			int newState = getOrderState(String.valueOf(order.getID()));
			if (newState == order.getState() && !isFirstTime)
				continue;
			intentForBroadcast.putExtra("id", String.valueOf(order.getID()));
			intentForBroadcast.putExtra("state", newState);
			sendBroadcast(intentForBroadcast);
			Context ctx = getApplicationContext();
			Resources res = ctx.getResources();
			Intent intentForActivity = null;
			boolean doCancel = false;
			boolean doRemove = false;
			switch (newState)
			{
				case Order.STATE_UNDEFINED :
					doCancel = true;
					break;
				case Order.STATE_NO_SUCH_ORDER :
					doCancel = true;
					break;
				case Order.STATE_ACCEPTED :
					break;
				case Order.STATE_LOOKING_FOR_CAR :
					intentForActivity = new Intent(this, ServiceMapActivity_.class)
											.putExtra("Reason", MapActivity.INTENT_LOOKING_FOR_CAR)
											.putExtra("Latitude", order.mPointFrom.getLat())
											.putExtra("Longitude", order.mPointFrom.getLon());
					//intentForActivity.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
					Log.d(LOG_TAG, intentForActivity.toString());
					break;
				case Order.STATE_CAR_ASSIGNED :
					break;
				case Order.STATE_CAR_APPROACHES :
					break;
				case Order.STATE_CAR_ARRIVED :
					break;
				case Order.STATE_ON_THE_WAY :
					break;
				case Order.STATE_CLOSED :
					break;
				case Order.STATE_CAR_NOT_FOUND :
					break;
				case Order.STATE_CANCELLED :
					break;
				case Order.STATE_EDITED :
					order = Order.getFromServer(order.getID());
					// Блокировка захвачена вне метода
					mOrders.remove(i);
					mOrders.add(order);
					break;
				case Order.STATE_DEBUG_REMOVE :
					doRemove = true;
					break;
				default :
					break;
			}
			if (doCancel)
				mNotificationManager.cancel(order.getID());
			else if (doRemove)
				RemoveOrder(order);
			else if (intentForActivity != null)
			{
				Notification notification = new NotificationCompat.Builder(getApplicationContext())
		         .setContentTitle(res.getString(Order.STATE_NAMES[newState]))
		         .setContentText(order.toString())
		         .setSmallIcon(R.drawable.ic_launcher)
		         .setLargeIcon(BitmapFactory.decodeResource(res, R.drawable.ic_launcher))
		         .setContentIntent(PendingIntent.getActivity(this, 0, intentForActivity, 0/*PendingIntent.FLAG_CANCEL_CURRENT*/))
		         .build();	
				mNotificationManager.notify(order.getID(), notification);
			}
			
		}
	}
	
	private class GetOrdersTask extends TimerTask
	{
		@Override
		public void run()
		{
        	boolean needReschedule = false;
    		for (int i = 0; i < mStoredOrdersIDs.length; ++i)
    		{
    			if (!mStoredOrdersIDs[i].equals("null") && !mStoredOrdersIDs[i].equals(""))
    			{
    				Order order = Order.getFromServer(mStoredOrdersIDs[i]);
    				if (order != null)
    				{
    	    		    synchronized(mOrders)
    	    		    {
	    					if (!mOrders.contains(order))
	    						mOrders.add(order);
    	    		    }
    				}
    				else
    				{
    					needReschedule = true;
    					break;
    				}
    			}
    		}
    		if (needReschedule)
    		{
    			mGetOrdersTimer.schedule(new GetOrdersTask(), 5000);
    		}
    		else
    		{
    		    Log.d(LOG_TAG, "Preloading Finished");
    		    synchronized(mOrders)
    		    {
    		    	SaveCurrentOrders();
    		    	mPreloadingFinished = true;
    		    }
    		}
		}
		
	}

	public void onCreate() 
	{
	    super.onCreate();
	    Log.d(LOG_TAG, "OrderStateService onCreate");
		SharedPreferences settings = getSharedPreferences("TaxiPrefs", MODE_PRIVATE);
		mStoredOrdersIDs = settings.getString("CurrentOrders", "").split(",");
		mOrders = new ArrayList<Order>();
		// Здесь пытаемся загрузить с сервера данные об уже активных заказах
		// Если попытка не удалась - то перезапускаем таймер снова через 5 сек.
		mPreloadingFinished = false;
		mGetOrdersTimer = new Timer();
    	mGetOrdersTimer.schedule(new GetOrdersTask(), 0);
		mNotificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		
		//mFakeState = 0;
		mFakeState = Order.STATE_LOOKING_FOR_CAR + 1;
		
		try
		{
			mStateUrl = new URL(MapActivity.URL_ORDER_STATE);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}

		XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        mStateResponseParser = factory.newPullParser();
		} catch (XmlPullParserException e) {
		}

    	new Thread(new Runnable(){ 
    		public void run(){
    			synchronized(mOrders) 
    			{
    				getAllOrdersState(true);
    			}
    			while (true)
    			{
	    			try
					{
						TimeUnit.SECONDS.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
	    			//mFakeState = ++mFakeState % 13;
	    			synchronized(mOrders) 
	    			{
		    			getAllOrdersState(false);
		    			if (mPreloadingFinished && mOrders.size() == 0)
		    			{
		    				// Завершаем сервис, если не осталось заказов в списке
		    				Log.d(LOG_TAG, "Exiting!..");
		    				stopSelf();
		    				break;
		    			}
	    			}
    			}
    		} 
    	}).start();
	}

	public void onDestroy() 
	{
	    super.onDestroy();
	    Log.d(LOG_TAG, "OrderStateService onDestroy");
	}

	private void SaveCurrentOrders()
	{
    	SharedPreferences settings = getSharedPreferences("TaxiPrefs", MODE_PRIVATE);
    	SharedPreferences.Editor prefEditor = settings.edit();
    	String param = "";
		// Блокировка mOrders захвачена вне метода
    	for (int i = 0; i < mOrders.size(); ++i)
    	{
    		if (i > 0)
    			param += ",";
    		param += Integer.toString(mOrders.get(i).getID());
    	}
	    Log.d(LOG_TAG, String.format("SaveCurrentOrders raw=%s", param));
    	prefEditor.putString("CurrentOrders", param);
    	prefEditor.commit();
	}
	
	void RemoveOrder(Order order)
	{
		mNotificationManager.cancel(order.getID());
	    synchronized(mOrders)
	    {
			mOrders.remove(order);
			SaveCurrentOrders();
	    }
	}
	
	public int onStartCommand(Intent intent, int flags, int startId) 
	{
		if (intent != null)
		{
			String newID = intent.getStringExtra("id");
			Log.d(LOG_TAG, String.format("OrderStateService onStartCommand, id=%s intent=%s", newID, intent.toString()));
			Order newOrder = Order.getFromServer(newID);
			if (newOrder != null)
			{
				synchronized(mOrders) {
					if (!mOrders.contains(newOrder))
					{
						mOrders.add(newOrder);
				    	if (mPreloadingFinished)
				    		SaveCurrentOrders();
					}
				}
			}
		}
		
		// FIXME
		return START_STICKY;
	}
	
}
