package com.taxisoft.taxiorder;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.taxisoft.taxiorder.R;

import ru.yandex.yandexmapkit.MapController;
import ru.yandex.yandexmapkit.MapView;
import ru.yandex.yandexmapkit.OverlayManager;
import ru.yandex.yandexmapkit.map.GeoCode;
import ru.yandex.yandexmapkit.map.GeoCodeListener;
import ru.yandex.yandexmapkit.map.MapEvent;
import ru.yandex.yandexmapkit.map.OnMapListener;
import ru.yandex.yandexmapkit.overlay.Overlay;
import ru.yandex.yandexmapkit.overlay.OverlayItem;
import ru.yandex.yandexmapkit.overlay.balloon.BalloonItem;
import ru.yandex.yandexmapkit.overlay.balloon.OnBalloonListener;
import ru.yandex.yandexmapkit.overlay.location.MyLocationItem;
import ru.yandex.yandexmapkit.overlay.location.OnMyLocationListener;
import ru.yandex.yandexmapkit.utils.GeoPoint;
import ru.yandex.yandexmapkit.utils.ScreenPoint;
import android.os.Bundle;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageButton;

public class MapActivity extends Activity implements OnClickListener, OnMapListener, GeoCodeListener, OnBalloonListener, OnMyLocationListener{

	MapView mMapView;
    MapController mMapController;
    OverlayManager mOverlayManager;
    Overlay mOverlay;
    OverlayItem mMeItem = null;
    Drawable mMePic;
    MyLocationBalloonItem mMeBalloon;
    static GeoPoint mMyLocation = null;
    GeoCode mMyGeoCode;
    Timer mTaxiesCoordsTimer;
    XmlPullParser mPositionsResponseParser;
    boolean mIsLoggedIn = false;
    ImageButton mBtnOrder, mBtnSettings;

    public static final int INTENT_SHOW_ALL_TAXIES = 0;
    public static final int INTENT_SHOW_ON_THE_MAP = 1;
    public static final int INTENT_CURRENT_LOCATION = 2;
    
    private static final int LOGIN_DIALOG_ID = 1;
    
    class TaxiData
    {
    	public GeoPoint coords;
    	public int id;
    	public TaxiData()
    	{
    		coords = new GeoPoint();
    	}
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_map);
		
        mMapView = (MapView)findViewById(R.id.map);

        mMapController = mMapView.getMapController();
        
        mMapView.showBuiltInScreenButtons(true);
        
        mOverlayManager = mMapController.getOverlayManager();
        //mOverlayManager.getMyLocation().setEnabled(true);
        //mOverlayManager.getMyLocation().addMyLocationListener(this);

        mOverlay = new Overlay(mMapController);
        // Add the layer to the map
        mOverlayManager.addOverlay(mOverlay);
		
        XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        mPositionsResponseParser = factory.newPullParser();
		} catch (XmlPullParserException e) {
		}
		
		mBtnOrder = (ImageButton)findViewById(R.id.btnOrder); 
		mBtnOrder.setOnClickListener(this);
		mBtnSettings = (ImageButton)findViewById(R.id.btnSettings); 
		mBtnSettings.setOnClickListener(this);

        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
        Drawable dr = getResources().getDrawable(R.drawable.passenger);
        Bitmap bitmap = ((BitmapDrawable)dr).getBitmap();
        mMePic = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, (int)px, (int)px, true));
        
        MyLocationUtils.prepareLocationListener(this);
    }
    
    public void createTaxi(Overlay overlay, TaxiData taxiData)
    {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 28, getResources().getDisplayMetrics());
        BitmapDrawable dr = (BitmapDrawable)getResources().getDrawable(R.drawable.balloon);
        BitmapDrawable taxiPic = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(dr.getBitmap(), (int)px, (int)px, true));
        Paint paint = new Paint(); 
        paint.setColor(Color.WHITE);
        paint.setTextSize(12f);
        paint.setAntiAlias(true);
        paint.setFakeBoldText(true);
        paint.setShadowLayer(2f, 0, 0, Color.BLACK);
        paint.setStyle(Paint.Style.FILL);
        paint.setTextAlign(Paint.Align.CENTER);	
        Canvas canvas = new Canvas(taxiPic.getBitmap());
        canvas.drawText(Integer.toString(taxiData.id), taxiPic.getIntrinsicWidth()/2, taxiPic.getIntrinsicHeight()/2, paint);
        // Create an object for the layer
        TaxiOverlayItem taxi = new TaxiOverlayItem(taxiData.coords, taxiPic, taxiData.id);
        taxi.setOffsetY(taxiPic.getIntrinsicHeight() / 2);
        // Create a balloon model for the object
        //TaxiBalloonItem balloonTaxi = new TaxiBalloonItem(this, taxi.getGeoPoint());
        //balloonTaxi.setTaxiData(Integer.toString(taxiData.id));        
        //
        //balloonTaxi.setOnBalloonListener(this);
        //        // Add the balloon model to the object
        //taxi.setBalloonItem(balloonTaxi);
        // Add the object to the layer
        overlay.addOverlayItem(taxi);
    }
    
    public void updateTaxi(TaxiOverlayItem taxi, TaxiData newTaxiData)
    {
    	// Sanity check
    	if (taxi.getID() == newTaxiData.id)
    	{
    		taxi.setGeoPoint(newTaxiData.coords);
    	}
    }

    public void removeTaxi(Overlay overlay, TaxiOverlayItem taxi)
    {
    	overlay.removeOverlayItem(taxi);
    }
    
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_map, menu);
		return true;
	}

    @Override
    protected void onPause() 
    {
    	super.onPause();
    	GeoPoint center = mMapController.getMapCenter(); 
    	SharedPreferences settings = getSharedPreferences("TaxiPrefs", MODE_PRIVATE);
    	SharedPreferences.Editor prefEditor = settings.edit();
    	prefEditor.putFloat("Latitude", (float)center.getLat());
    	prefEditor.putFloat("Longitude", (float)center.getLon());
    	prefEditor.putFloat("Scale", mMapController.getZoomCurrent());
    	prefEditor.commit();
    };
    
    @Override
    protected void onResume() 
    {
    	super.onResume();

		int reason = getIntent().getIntExtra("Reason", INTENT_SHOW_ALL_TAXIES);
    	SharedPreferences settings = getSharedPreferences("TaxiPrefs", MODE_PRIVATE);
		mMapView.setVisibility(View.VISIBLE);
		if (reason == INTENT_SHOW_ALL_TAXIES)
		{
			mBtnOrder.setVisibility(View.VISIBLE);
			mBtnSettings.setVisibility(View.VISIBLE);
	    	mTaxiesCoordsTimer = new Timer();
	    	mTaxiesCoordsTimer.scheduleAtFixedRate(new TimerTask() {
	            @Override
	            public void run() {
	            	updateTaxiesPositions();
	            }
	    	}, 0, 5000);
	    	mMapController.setPositionNoAnimationTo(new GeoPoint(settings.getFloat("Latitude", 51.735262f), settings.getFloat("Longitude", 36.185569f)), settings.getFloat("Scale", 0.0f));
		}
		else if (reason == INTENT_SHOW_ON_THE_MAP)
		{
			mBtnOrder.setVisibility(View.GONE);
			mBtnSettings.setVisibility(View.GONE);
			mMapController.addMapListener(this);
	    	mMapController.setPositionNoAnimationTo(new GeoPoint(settings.getFloat("Latitude", 51.735262f), settings.getFloat("Longitude", 36.185569f)), settings.getFloat("Scale", 0.0f));
		}
		else if (reason == INTENT_CURRENT_LOCATION)
		{
			mBtnOrder.setVisibility(View.GONE);
			mBtnSettings.setVisibility(View.GONE);
			mMapView.setVisibility(View.GONE);
			if (MyLocationUtils.storedLocation != null)
			{
				mMapController.getDownloader().getGeoCode(this, new GeoPoint(MyLocationUtils.storedLocation.getLatitude(), MyLocationUtils.storedLocation.getLongitude()));
			}
			else
			{
				//TODO
			}
		}
    };
    
    private List<TaxiData> parsePositionsResponse(InputStream response)
    {
    	boolean inDrivers = false;
    	List<TaxiData> taxies = new ArrayList<TaxiData>();
    	try {
    		mPositionsResponseParser.setInput(response, "utf-8");
	        int eventType = mPositionsResponseParser.getEventType();
	        while (eventType != XmlPullParser.END_DOCUMENT) {
	        	if(eventType == XmlPullParser.START_TAG) {
	        		if (mPositionsResponseParser.getName().equalsIgnoreCase("drivers"))
	        			inDrivers = true;
	        		else if (inDrivers && mPositionsResponseParser.getName().equalsIgnoreCase("driver"))
	        		{
	        			TaxiData taxiData = new TaxiData(); 
	        			for (int i = 0; i < mPositionsResponseParser.getAttributeCount(); ++i)
	        				if (mPositionsResponseParser.getAttributeName(i).equalsIgnoreCase("gpslat"))
	        					taxiData.coords.setLat(Double.parseDouble(mPositionsResponseParser.getAttributeValue(i)));
	        				else if (mPositionsResponseParser.getAttributeName(i).equalsIgnoreCase("gpslon"))
	        					taxiData.coords.setLon(Double.parseDouble(mPositionsResponseParser.getAttributeValue(i)));
	        			taxiData.id = Integer.parseInt(mPositionsResponseParser.nextText());
	        			taxies.add(taxiData);
	        		}
	        	}
	        	// FIXME: здесь почему-то валится иногда ArrayIndexOutOfBoundsException
	        	eventType = mPositionsResponseParser.nextTag();
	        }
						
		} catch (XmlPullParserException e) {
		} catch (IOException e) {
		}
    	if (!inDrivers)
    		return null;
    	return taxies;
    }
    
    private void updateTaxiesPositions()
    {
    	List<TaxiData> newTaxies;
    	try {
			URL taxiesUrl = new URL("http://79.175.38.54:4481/get_gps.php");
			newTaxies = parsePositionsResponse(taxiesUrl.openStream());
			if (newTaxies == null)
				return;
			ListIterator<TaxiData> newIt = newTaxies.listIterator();

			@SuppressWarnings("unchecked")
			List<TaxiOverlayItem> oldTaxies = mOverlay.getOverlayItems();
			ListIterator<TaxiOverlayItem> oldIt = oldTaxies.listIterator();

			while(newIt.hasNext())
	        {
	        	TaxiData taxiData = newIt.next();
	        	while(oldIt.hasNext())
	        	{
	        		TaxiOverlayItem oldTaxi = oldIt.next();
	        		if (taxiData.id == oldTaxi.getID())
	        		{
	        			updateTaxi(oldTaxi, taxiData);
	        			oldIt.remove();
	        			newIt.remove();
	        		}
	        	}
	        	while(oldIt.hasPrevious())
	        		oldIt.previous();
	        }
        	while(newIt.hasPrevious())
        		newIt.previous();
        	
        	while(oldIt.hasNext())
        		removeTaxi(mOverlay, oldIt.next());

        	while(newIt.hasNext())
        		createTaxi(mOverlay, newIt.next());
        	
	        mMapController.notifyRepaint();
	    } catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
	
    @Override
    protected Dialog onCreateDialog(int id) {
    	AlertDialog.Builder builder = null;
        switch (id) {
        case LOGIN_DIALOG_ID:
        	LayoutInflater li = LayoutInflater.from(this);
        	View view = li.inflate(R.layout.login, null);
        	builder = new AlertDialog.Builder(this);
        	builder.setView(view);
        	builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                    // sign in the user ...
                }
        	});
            builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    dialog.cancel();
                }
            }); 
        	break;
        }
        if (builder != null)
        	return builder.create();
        else
        	return null;
    }

	@Override
	public void onClick(View v) {
		if (v == mBtnOrder)
		{
			Intent intent = new Intent(this, OrderActivity.class);
			startActivity(intent);
		}
	}

	@Override
	public void onMapActionEvent(MapEvent event) {
		if (event.getMsg() == MapEvent.MSG_LONG_PRESS)
		{
			ScreenPoint sPoint = new ScreenPoint(event.getX(), event.getY());
			mMapController.getDownloader().getGeoCode(this, mMapController.getGeoPoint(sPoint));
		}
	}
	
//	public static void getGeoCode(GeoCodeListener listener, double lat, double lon)
//	{
//		mMapController.getDownloader().getGeoCode(listener, new GeoPoint(lat, lon));
//	}
	
	@Override
	public boolean onFinishGeoCode(GeoCode geoCode) {
		int reason = getIntent().getIntExtra("Reason", INTENT_SHOW_ALL_TAXIES);

		if (reason == INTENT_SHOW_ON_THE_MAP ||
			reason == INTENT_CURRENT_LOCATION)
		{
			Intent resultIntent = new Intent();
			if (geoCode.getKind().equals(GeoCode.OBJECT_KIND_STREET) ||
				geoCode.getKind().equals(GeoCode.OBJECT_KIND_HOUSE))
			{
				resultIntent.putExtra("Street", geoCode.getTitle());
				resultIntent.putExtra("City", geoCode.getSubtitle());
				setResult(RESULT_OK, resultIntent);
				finish();
			}
		}
		return true;
	}	
	
    public void showMe(GeoPoint place)
    {
    	if (mMeItem == null)
    	{
    		// Create an object for the layer
    		mMeItem = new OverlayItem(place, mMePic);
            // Create a balloon model for the object
    		mMeBalloon = new MyLocationBalloonItem(this, mMeItem.getGeoPoint());
    		mMeBalloon.setOnBalloonListener(this);
            // Add the balloon model to the object
            mMeItem.setBalloonItem(mMeBalloon);
    	}
    	else
    	{
            mOverlay.removeOverlayItem(mMeItem);
            mMeItem.setGeoPoint(place);
    	}
        // Add the object to the layer
        mOverlay.addOverlayItem(mMeItem);
    }

	@Override
	public void onBalloonAnimationEnd(BalloonItem arg0) {
	}

	@Override
	public void onBalloonAnimationStart(BalloonItem arg0) {
	}

	@Override
	public void onBalloonHide(BalloonItem arg0) {
	}

	@Override
	public void onBalloonShow(BalloonItem arg0) {
	}

	@Override
	public void onBalloonViewClick(BalloonItem balloonItem, View view) {
		if (balloonItem == mMeBalloon)
		{
			if (view.getId() == R.id.btnYes)
			{
				Intent resultIntent = new Intent();
				if (mMyGeoCode.getKind().equals(GeoCode.OBJECT_KIND_STREET) ||
					mMyGeoCode.getKind().equals(GeoCode.OBJECT_KIND_HOUSE))
				{
					resultIntent.putExtra("Street", mMyGeoCode.getTitle());
					resultIntent.putExtra("City", mMyGeoCode.getSubtitle());
					setResult(RESULT_OK, resultIntent);
					finish();
				}
			}
			else if (view.getId() == R.id.btnNo)
			{
				setResult(RESULT_CANCELED);
				finish();
			}
		}
	}

	@Override
	public void onMyLocationChange(MyLocationItem myLocationItem) {
		mMyLocation = myLocationItem.getGeoPoint();
	}

}
