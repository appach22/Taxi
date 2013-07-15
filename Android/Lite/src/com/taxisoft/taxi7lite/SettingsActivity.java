package com.taxisoft.taxi7lite;

import com.taxisoft.taxi7lite.R;

import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;

public class SettingsActivity extends Activity implements OnClickListener{
	
	String mName;
	String mNumber;
	SharedPreferences mSettings;
	SharedPreferences.Editor mSettingsEditor;
	
	EditText edtName;
	EditText edtNumber;
	Button btnContinueOrder;
	Button btnCancel;
	LinearLayout twoButtonsLayout;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		btnContinueOrder = (Button) findViewById(R.id.btnContinueOrder);
		btnContinueOrder.setOnClickListener(this);
		btnCancel = (Button) findViewById(R.id.btnCancel);
		btnCancel.setOnClickListener(this);
		twoButtonsLayout = (LinearLayout) findViewById(R.id.twoButtonsLayout);
		edtNumber = (EditText) findViewById(R.id.edtNumber);
		edtNumber.addTextChangedListener(new MaskedWatcher("(###)###-##-##"));
		edtName = (EditText) findViewById(R.id.edtName);
		edtName.addTextChangedListener(new TextWatcher(){
	        public void afterTextChanged(Editable s) {
	        	if (!mName.equals(s.toString()))
	        	{
	        		mSettingsEditor.putString("name", s.toString());
	        		mSettingsEditor.commit();
	        		mName = s.toString();
	        	}
	        }
	        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
	    });
		edtNumber.addTextChangedListener(new TextWatcher(){
	        public void afterTextChanged(Editable s) {
	        	if (!mNumber.equals(s.toString()))
	        	{
	        		mSettingsEditor.putString("number", s.toString());
	                mSettingsEditor.putBoolean("numberIsVerified", false);
	        		mSettingsEditor.commit();
	        		mNumber = s.toString();
	        	}
	        }
	        public void beforeTextChanged(CharSequence s, int start, int count, int after){}
	        public void onTextChanged(CharSequence s, int start, int before, int count){}
	    });
    	mSettings = getSharedPreferences("TaxiLitePrefs", MODE_PRIVATE);
    	mSettingsEditor = mSettings.edit();
	}

    @Override
    protected void onResume() 
    {
    	super.onResume();
    	
    	Intent intent = getIntent();
    	
    	if (intent.getBooleanExtra("startedForResult", false))
    		twoButtonsLayout.setVisibility(View.VISIBLE);
    	else
    		twoButtonsLayout.setVisibility(View.INVISIBLE);

    	boolean isFirstTime = intent.getBooleanExtra("isFirstTime", false);

    	mName = mSettings.getString("name", "");
    	mNumber = mSettings.getString("number", "");
    	if (mNumber.length() == 0)
    	{
    		TelephonyManager tm = (TelephonyManager)getSystemService(TELEPHONY_SERVICE); 
    		if (tm != null)
    		{
    			mNumber = tm.getLine1Number();
    			if (mNumber == null)
    				mNumber = "";
				mNumber = mNumber.replaceAll("[^0-9]", "");
				if (mNumber.length() > 10)
					mNumber = mNumber.substring(mNumber.length() - 10, mNumber.length());
    		}
    	}

    	edtName.setText(mName);
    	edtNumber.setText(mNumber);
    	
    	if (isFirstTime)
    		Toast.makeText(this, R.string.first_time_warning, Toast.LENGTH_LONG).show();
    }

	@Override
	public void onClick(View source) 
	{
		if (source.equals(btnContinueOrder))
			setResult(RESULT_OK);
		else if (source.equals(btnCancel))
			setResult(RESULT_CANCELED);
		finish();
	}

}
