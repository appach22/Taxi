package com.taxisoft.taxiorder;

import android.graphics.drawable.Drawable;
import ru.yandex.yandexmapkit.overlay.OverlayItem;
import ru.yandex.yandexmapkit.utils.GeoPoint;

public class TaxiOverlayItem extends OverlayItem {

	private int mID;
	
	public TaxiOverlayItem(GeoPoint geoPoint, Drawable drawable, int id) {
		super(geoPoint, drawable);
		mID = id;
	}
	
	public int getID()
	{
		return mID;
	}
}
