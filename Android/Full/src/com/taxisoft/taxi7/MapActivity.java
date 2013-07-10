package com.taxisoft.taxi7;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.ListIterator;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.Click;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ViewById;
import com.taxisoft.taxi7.R;

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
import android.os.AsyncTask;
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
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

@EActivity(R.layout.activity_map)
public class MapActivity extends Activity implements OnMapListener, GeoCodeListener, OnBalloonListener, OnMyLocationListener{

	@ViewById(R.id.map)
	MapView mMapView;
    @ViewById
    ImageButton btnOrder;
    @ViewById
    ImageButton btnSettings;
	
    MapController mMapController;
    OverlayManager mOverlayManager;
    Overlay mTaxiesOverlay;
    Overlay mCommonOverlay;
    OverlayItem mMeItem = null;
    Drawable mMePic;
    MyLocationBalloonItem mMeBalloon;
    static GeoPoint mMyLocation = null;
    GeoCode mMyGeoCode;
    Timer mUpdatePositionsTimer;
    UpdatePositionsTask mUpdatePositionsTask;
    XmlPullParser mPositionsResponseParser;
    boolean mIsLoggedIn = false;

    public static final int INTENT_UNKNOWN = -1;
    public static final int INTENT_SHOW_ALL_TAXIES = 0;
    public static final int INTENT_SHOW_ON_THE_MAP = 1;
    public static final int INTENT_CURRENT_LOCATION = 2;
    public static final int INTENT_LOOKING_FOR_CAR = 3;
    public static final int INTENT_DEFAULT = INTENT_SHOW_ALL_TAXIES;
    
    private static final int LOGIN_DIALOG_ID = 1;
    
    private static final String SERVER = "79.175.38.54";
    private static final String PORT = "80"; //"4481";
    private static final String URL_ROOT = "http://" + SERVER + ":" + PORT;
    
    public static final String URL_TAXIES_POSITIONS = URL_ROOT + "/get_gps.php";
    public static final String URL_STREETS 			= URL_ROOT + "/get_streets.php";
    public static final String URL_PLACE_ORDER		= URL_ROOT + "/place_order.php";
    public static final String URL_ORDER_STATE 		= URL_ROOT + "/get_order_state.php";
    public static final String URL_ORDER_DATA 		= URL_ROOT + "/get_order_data.php";
    
    public static final String URL_YANDEX_GEOCODER  = "http://geocode-maps.yandex.ru/1.x/";
    
    class TaxiData
    {
    	public GeoPoint coords;
    	public int id;
    	public TaxiData()
    	{
    		coords = new GeoPoint();
    	}
    }

    @AfterViews
    void InitViews()
    {
        mMapController = mMapView.getMapController();
        
        mMapView.showBuiltInScreenButtons(true);
        
        mOverlayManager = mMapController.getOverlayManager();
        //mOverlayManager.getMyLocation().setEnabled(true);
        //mOverlayManager.getMyLocation().addMyLocationListener(this);

        mTaxiesOverlay = new Overlay(mMapController);
        // Add the layer to the map
        mOverlayManager.addOverlay(mTaxiesOverlay);
        mCommonOverlay = new Overlay(mMapController);
        // Add the layer to the map
        mOverlayManager.addOverlay(mCommonOverlay);
    }
    
    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		
        XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
	        factory.setNamespaceAware(true);
	        mPositionsResponseParser = factory.newPullParser();
		} catch (XmlPullParserException e) {
		}
		

        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 30, getResources().getDisplayMetrics());
        Drawable dr = getResources().getDrawable(R.drawable.passenger);
        Bitmap bitmap = ((BitmapDrawable)dr).getBitmap();
        mMePic = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(bitmap, (int)px, (int)px, true));
        
        MyLocationUtils.prepareLocationListener(this);
    }
    
    public void createTaxi(Overlay overlay, TaxiData taxiData)
    {
        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 32, getResources().getDisplayMetrics());
        BitmapDrawable dr = (BitmapDrawable)getResources().getDrawable(R.drawable.balloon);
        BitmapDrawable taxiPic = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(dr.getBitmap(), (int)px, (int)px, true));
        Paint paint = new Paint(); 
        paint.setColor(Color.WHITE);
        paint.setTextSize(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 12, getResources().getDisplayMetrics()));
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
    	if (getIntent().getIntExtra("Reason", INTENT_UNKNOWN) == INTENT_SHOW_ON_THE_MAP)
    		return;
    	if (mUpdatePositionsTask != null)
    		mUpdatePositionsTask.cancel(true);
    	mTaxiesOverlay.clearOverlayItems();
    	GeoPoint center = mMapController.getMapCenter(); 
    	SharedPreferences settings = getSharedPreferences("TaxiPrefs", MODE_PRIVATE);
    	SharedPreferences.Editor prefEditor = settings.edit();
    	prefEditor.putFloat("Latitude", (float)center.getLat());
    	prefEditor.putFloat("Longitude", (float)center.getLon());
    	prefEditor.putFloat("Scale", mMapController.getZoomCurrent());
    	prefEditor.commit();
    };
    
    private class UpdatePositionsTask extends AsyncTask<Void, Void, Void>
    {
    	List<TaxiOverlayItem> mOldTaxies;
    	List<TaxiData> mNewTaxies;
    	ListIterator<TaxiData> mNewIt;
    	ListIterator<TaxiOverlayItem> mOldIt;
    	
    	private void updateTaxi(TaxiOverlayItem taxi, TaxiData newTaxiData)
        {
        	// Sanity check
        	if (taxi.getID() == newTaxiData.id)
        	{
        		taxi.setGeoPoint(newTaxiData.coords);
        	}
        }

    	private void removeTaxi(Overlay overlay, TaxiOverlayItem taxi)
        {
        	overlay.removeOverlayItem(taxi);
        }
             
        private List<TaxiData> parsePositionsResponse(InputStream response)
        {
        	boolean inDrivers = false;
        	List<TaxiData> taxies = new ArrayList<TaxiData>();
        	try {
        		synchronized(mPositionsResponseParser)
        		{
	        		mPositionsResponseParser.setInput(response, "utf-8");
	    	        int eventType = mPositionsResponseParser.getEventType();
	    	        while (eventType != XmlPullParser.END_DOCUMENT) {
	    	        	if (isCancelled())
	    	        		return taxies;
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
	    	        	eventType = mPositionsResponseParser.nextTag();
	    	        }
        		}	    	       
    		} catch (XmlPullParserException e) {
    		} catch (IOException e) {
    		} catch (NumberFormatException e) {
    		} catch (Exception e) {
    			e.printStackTrace();
    		}
        	if (!inDrivers)
        		return null;
        	return taxies;
        }
        
        private void getNewTaxies()
        {
        	try {
    			URL taxiesUrl = new URL(URL_TAXIES_POSITIONS);
    			mNewTaxies = parsePositionsResponse(taxiesUrl.openStream());
    	    } catch (MalformedURLException e) {
    			e.printStackTrace();
    		} catch (IOException e) {
    			e.printStackTrace();
    		}
        }
        
        private void updateTaxiesPositions()
        {
			if (mNewTaxies == null)
				return;
			mNewIt = mNewTaxies.listIterator();
			mOldIt = mOldTaxies.listIterator();

			while(mNewIt.hasNext())
	        {
	        	TaxiData taxiData = mNewIt.next();
	        	while(mOldIt.hasNext())
	        	{
	        		TaxiOverlayItem oldTaxi = mOldIt.next();
	        		if (taxiData.id == oldTaxi.getID())
	        		{
	        			updateTaxi(oldTaxi, taxiData);
	        			mOldIt.remove();
	        			mNewIt.remove();
	        		}
	        	}
	        	while(mOldIt.hasPrevious())
	        		mOldIt.previous();
	        }
        	while(mNewIt.hasPrevious())
        		mNewIt.previous();
        	
        	while(mOldIt.hasNext())
        		removeTaxi(mTaxiesOverlay, mOldIt.next());

        	while(mNewIt.hasNext())
        		createTaxi(mTaxiesOverlay, mNewIt.next());
        	
	        mMapController.notifyRepaint();
        	
        }
        
    	@SuppressWarnings("unchecked")
		@Override
    	protected void onPreExecute()
    	{
			mOldTaxies = mTaxiesOverlay.getOverlayItems();
    	}
    	
		@Override
		protected Void doInBackground(Void... params)
		{
			getNewTaxies();
			return null;
		}
    	
		boolean mWasCancelled= false;
		@Override
		protected void onCancelled()
		{
			mWasCancelled = true;
			mUpdatePositionsTimer.cancel();
		}
		
		@Override
    	protected void onPostExecute(Void result)
		{
			if (mWasCancelled)
				return;
			updateTaxiesPositions();
			mUpdatePositionsTask = new UpdatePositionsTask();			
			if (mWasCancelled)
				return;
	    	mUpdatePositionsTimer.schedule(new TimerTask() {
	            @Override
	            public void run() {
	            	runOnUiThread(new Runnable() {public void run(){ mUpdatePositionsTask.execute(); }});
	            }
	    	}, 5000);
		}
    }
    
    @Override
    protected void onResume() 
    {
    	super.onResume();

    	Intent intent = getIntent();
		int reason = intent.getIntExtra("Reason", INTENT_SHOW_ALL_TAXIES);
    	SharedPreferences settings = getSharedPreferences("TaxiPrefs", MODE_PRIVATE);
		mMapView.setVisibility(View.VISIBLE);
		if (reason == INTENT_SHOW_ALL_TAXIES)
		{
			btnOrder.setVisibility(View.VISIBLE);
			btnSettings.setVisibility(View.VISIBLE);
	    	mUpdatePositionsTimer = new Timer();
	    	mUpdatePositionsTask = new UpdatePositionsTask();
	    	mUpdatePositionsTimer.schedule(new TimerTask() {
	            @Override
	            public void run() {
	            	runOnUiThread(new Runnable() {public void run(){mUpdatePositionsTask.execute();}});
	            }
	    	}, 0);
		}
		else if (reason == INTENT_SHOW_ON_THE_MAP)
		{
			setTitle(getResources().getString(R.string.place_choosing));
			btnOrder.setVisibility(View.GONE);
			btnSettings.setVisibility(View.GONE);
			mMapController.addMapListener(this);
		}
		else if (reason == INTENT_CURRENT_LOCATION)
		{
			btnOrder.setVisibility(View.GONE);
			btnSettings.setVisibility(View.GONE);
			mMapView.setVisibility(View.GONE);
			if (MyLocationUtils.storedLocation != null)
			{
				mMapController.getDownloader().getGeoCode(this, new GeoPoint(MyLocationUtils.storedLocation.getLatitude(), MyLocationUtils.storedLocation.getLongitude()));
			}
			else
			{
				Toast.makeText(this, R.string.location_is_unknown, Toast.LENGTH_SHORT).show();
			}
		}
		else if (reason == INTENT_LOOKING_FOR_CAR)
		{
			setTitle(getResources().getString(R.string.state_looking_for_car));
			btnOrder.setVisibility(View.GONE);
			btnSettings.setVisibility(View.GONE);
			GeoPoint start = new GeoPoint(intent.getDoubleExtra("Latitude", 51.730895), intent.getDoubleExtra("Longitude", 36.192779));
			mMapController.setPositionNoAnimationTo(start, 13.0f);
	        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
	        BitmapDrawable dr = (BitmapDrawable)getResources().getDrawable(R.drawable.radar);
	        BitmapDrawable radarPic = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(dr.getBitmap(), (int)px, (int)px, true));
	        OverlayItem radar = new OverlayItem(start, radarPic);
	        mCommonOverlay.addOverlayItem(radar);
	    	mUpdatePositionsTimer = new Timer();
	    	mUpdatePositionsTask = new UpdatePositionsTask();
	    	mUpdatePositionsTimer.schedule(new TimerTask() {
	            @Override
	            public void run() {
	            	runOnUiThread(new Runnable() {public void run(){mUpdatePositionsTask.execute();}});
	            }
	    	}, 0);
		}
		if (reason != INTENT_LOOKING_FOR_CAR)
			mMapController.setPositionNoAnimationTo(new GeoPoint(settings.getFloat("Latitude", 51.730895f), settings.getFloat("Longitude", 36.192779f)), settings.getFloat("Scale", 0.0f));
    };
    
    /*@Override
    protected void onNewIntent(Intent intent) 
    {
    	super.onNewIntent(intent);
		int reason = intent.getIntExtra("Reason", INTENT_SHOW_ALL_TAXIES);
		if (reason == INTENT_LOOKING_FOR_CAR)
		{
			GeoPoint start = new GeoPoint(intent.getDoubleExtra("Latitude", 51.730895), intent.getDoubleExtra("Longitude", 36.192779));
			mMapController.setPositionNoAnimationTo(start);
	        float px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 100, getResources().getDisplayMetrics());
	        BitmapDrawable dr = (BitmapDrawable)getResources().getDrawable(R.drawable.radar);
	        BitmapDrawable radarPic = new BitmapDrawable(getResources(), Bitmap.createScaledBitmap(dr.getBitmap(), (int)px, (int)px, true));
	        OverlayItem radar = new OverlayItem(start, radarPic);
	        mCommonOverlay.addOverlayItem(radar);
		}
    }*/
    
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

	@Click(R.id.btnOrder)
	public void placeOrder()
	{
		Intent intent = new Intent(this, OrderActivity_.class);
		startActivity(intent);
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
			if (geoCode != null && (geoCode.getKind().equals(GeoCode.OBJECT_KIND_STREET) ||
									geoCode.getKind().equals(GeoCode.OBJECT_KIND_HOUSE)))
			{
				resultIntent.putExtra("Street", geoCode.getTitle());
				resultIntent.putExtra("City", geoCode.getSubtitle());
				setResult(RESULT_OK, resultIntent);
				finish();
			}
			else
				// TODO: почему не отображается? Наверное, потому что в др. потоке находимся...
				Toast.makeText(this, R.string.be_more_precise, Toast.LENGTH_LONG).show();
				
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
            mTaxiesOverlay.removeOverlayItem(mMeItem);
            mMeItem.setGeoPoint(place);
    	}
        // Add the object to the layer
        mTaxiesOverlay.addOverlayItem(mMeItem);
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
