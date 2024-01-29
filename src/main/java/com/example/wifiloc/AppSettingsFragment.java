package com.example.wifiloc;


import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class AppSettingsFragment extends PreferenceFragment{ //  TODO implements listener for settings changed action
	private Resources resources;
	private SharedPreferences prefs;
	private PreferenceManager pmManager;
	private PreferenceChangeListener preferenceChangeListener;
	private PreferenceClickListener preferenceClickListener;
	Preference prefDefault;
	public int locDistanceInterval = 2;
	public int locInterval = 1000;
	public String locMode = "locCont"; //getString(R.string.locModeContinousAlias);
	public String locProvider = "gps";//etString(R.string.locProviderGpsAlias);
	public String wifiMode = "locDemand"; //getString(R.string.locModeOnDemandAlias);
	public int wifiInterval = 1000;
	public int wifiMinLoc = 10;
	public int wifiMinMeasure = 10;
	public float wifiPropagationFactor = 3;
	public boolean saveData = false;
	public float mapBearingInterval = 0;
	public float deviceFloatingCriteria = 0;
    public float wifiAttenuationFactor = 0;
	
	

	   @Override
	   public void onCreate(Bundle savedInstanceState) {
		   	
	        super.onCreate(savedInstanceState);
	        addPreferencesFromResource(R.xml.application_settings);
	        prefs = getPreferenceManager().getSharedPreferences();
	        //PreferenceManager.setSharedPreferencesName("myPrefs");
	        pmManager = getPreferenceManager();
	        
	        
	        resources = this.getResources();
	        prefDefault = (Preference) findPreference(getString(R.string.prefRestoreDefault));
	        PreferenceManager.setDefaultValues(getActivity(), R.xml.application_settings, false);
	        
	        
	   }
	   
	   
	    @Override
		public void onResume() {
	        super.onResume();
	        // Set up a listener whenever a key changes
	        getPreferenceScreen().getSharedPreferences()
	                .registerOnSharedPreferenceChangeListener((preferenceChangeListener = new PreferenceChangeListener()));
	        prefDefault.setOnPreferenceClickListener(preferenceClickListener = new PreferenceClickListener());
	        
	        
	    }

	    @Override
		public void onStop() {
	        super.onPause();
	        // Unregister the listener whenever a key changes
	        try{
	        getPreferenceScreen().getSharedPreferences()
	                .unregisterOnSharedPreferenceChangeListener(preferenceChangeListener);
	        prefDefault.setOnPreferenceClickListener(null);
	        }
	        catch(Exception ex)
	        {
	        	Log.e("Preference error",ex.getMessage());
	        }
	    }
	    
	   
	    class PreferenceClickListener implements OnPreferenceClickListener
	    {
	    	
			@Override
			public boolean onPreferenceClick(Preference preference) {
				PreferenceManager.setDefaultValues(getActivity(), R.xml.application_settings, false);
				return false;
			}
	    }
	
	    class PreferenceChangeListener implements OnSharedPreferenceChangeListener
	    {

			@Override
			public void onSharedPreferenceChanged(
					SharedPreferences sharedPreferences, String key) {
				EditTextPreference tempPref;
				String cc = key.trim();
				String cos = getResources().getString(R.string.prefLocDistanceInterval);
							if(key.compareToIgnoreCase(getResources().getString(R.string.prefLocDistanceInterval)) == 0 ){
								tempPref = (EditTextPreference) findPreference(key);
								try
								{
									locDistanceInterval = Integer.parseInt(sharedPreferences.getString(key, "2"));
								}
								catch(Exception exc){ 
									locDistanceInterval = 2;
									Toast.makeText(getActivity(), getString(R.string.toastBadInputData), Toast.LENGTH_LONG).show();
									tempPref.setText("2");
									return;
								}							
								if((locDistanceInterval < 1) || (locDistanceInterval > 10))
								{
									locDistanceInterval = 2;
									tempPref.setText("2");
									Toast.makeText(getActivity(), getString(R.string.toastLocDistanceInterval), Toast.LENGTH_LONG).show();
								}
								return;}
							
							if(key.compareToIgnoreCase(getResources().getString(R.string.prefLocInterval)) == 0){
								
								tempPref = (EditTextPreference) findPreference(key);
								try
								{
									locInterval = Integer.parseInt(sharedPreferences.getString(key, "1000"));
								}
								catch(Exception ex){ 
									locInterval = 1000;
									tempPref.setText("1000");
									Toast.makeText(getActivity(), getString(R.string.toastBadInputData), Toast.LENGTH_LONG).show();
									return;
								}
								
								if((locInterval < 1000 )|| (locInterval > 10000))
								{
									locInterval = 1000;
									tempPref.setText("1000");
									Toast.makeText(getActivity(), getString(R.string.toastLocInterval), Toast.LENGTH_LONG).show();
								}
								return;}
					
							if(key.compareToIgnoreCase(getResources().getString(R.string.prefLocMode)) == 0){
								locMode = sharedPreferences.getString(key, getString(R.string.locModeContinousAlias));
							
								return;}
							if(key.compareToIgnoreCase(getResources().getString(R.string.prefLocProvider))==0)
								{
									locProvider = sharedPreferences.getString(key,getString(R.string.locProviderGpsAlias));
									
								return;}
							
							if(key.compareToIgnoreCase(getResources().getString(R.string.prefWifiInterval)) == 0 )
							{
								tempPref = (EditTextPreference) findPreference(key);
								try{
									wifiInterval = Integer.parseInt(sharedPreferences.getString(key, "1000"));
								}
								catch(Exception ex)
								{
									wifiInterval = 1000;
									tempPref.setText("1000");
									Toast.makeText(getActivity(), getString(R.string.toastBadInputData), Toast.LENGTH_LONG).show();
									return;
								}
								if((wifiInterval < 200) || (wifiInterval > 10000))
								{
									wifiInterval = 2000;
									tempPref.setText("2000");
									Toast.makeText(getActivity(), getString(R.string.toastWifiInterval), Toast.LENGTH_LONG).show();
								}
								return;}
							
							if(key.compareToIgnoreCase(getResources().getString(R.string.prefWifiMinLoc)) == 0)
									{
								tempPref = (EditTextPreference) findPreference(key);
								try{
									wifiMinLoc = Integer.parseInt(sharedPreferences.getString(key, "10"));
								}
								catch(Exception ex)
								{
									wifiMinLoc = 10;
									tempPref.setText("10");
									Toast.makeText(getActivity(), getString(R.string.toastBadInputData), Toast.LENGTH_LONG).show();
									return;
								}
								if((wifiMinLoc < 3) || (wifiMinLoc > 100))
								{
									wifiInterval = 10;
									tempPref.setText("10");
									Toast.makeText(getActivity(), getString(R.string.toastWifiMinLoc), Toast.LENGTH_LONG).show();
								}
								return;}
							
							if(key.compareToIgnoreCase(getResources().getString(R.string.prefWifiMinMeasure)) == 0)
							{
								
								tempPref = (EditTextPreference) findPreference(key);
								try{
									wifiMinMeasure = Integer.parseInt(sharedPreferences.getString(key, "10"));
								}
								catch(Exception ex)
								{
									wifiMinMeasure = 10;
									tempPref.setText("10");
									Toast.makeText(getActivity(), getString(R.string.toastBadInputData), Toast.LENGTH_LONG).show();
									return;
								}
								// dla testow
								if((wifiMinMeasure < 1) || (wifiMinMeasure > 100))
								{
									wifiInterval = 10;
									tempPref.setText("10");
									Toast.makeText(getActivity(), getString(R.string.toastWifiMinMeasure), Toast.LENGTH_LONG).show();
								}
								return;}
							if(key.compareToIgnoreCase(getResources().getString(R.string.prefWifiMode)) == 0)
							{
								wifiMode = sharedPreferences.getString(key, getString(R.string.locModeOnDemandAlias));
							
								return;}
							if(key.compareToIgnoreCase(getResources().getString(R.string.prefWifiPropagationFactor)) == 0)
							{
								
								tempPref = (EditTextPreference) findPreference(key);
								try{
									wifiPropagationFactor= Float.parseFloat(sharedPreferences.getString(key, "3"));
								}
								catch(Exception ex){
									wifiPropagationFactor = 3;
									tempPref.setText("3");
									Toast.makeText(getActivity(), getString(R.string.toastBadInputData), Toast.LENGTH_LONG).show();
									return;
								}
								 
								if((wifiPropagationFactor < 1) || (wifiPropagationFactor > 6))
								{
									wifiInterval = 3;
									tempPref.setText("3");
									Toast.makeText(getActivity(), getString(R.string.toastWifiPropagationFactor), Toast.LENGTH_LONG).show();
								}
								return;}
							if(key.compareToIgnoreCase(getResources().getString(R.string.prefMapBearingInterval)) == 0)
							{
							tempPref = (EditTextPreference) findPreference(key);
							try{
								mapBearingInterval = Float.parseFloat(sharedPreferences.getString(key, "1"));
							}
							catch(Exception ex)
							{
								mapBearingInterval = 1;
								tempPref.setText("1");
								Toast.makeText(getActivity(), getString(R.string.toastBadInputData), Toast.LENGTH_LONG).show();
								return;
							}
							if((wifiMinLoc < 0.01) || (wifiMinLoc > 10))
							{
								wifiInterval = 1;
								tempPref.setText("1");
								Toast.makeText(getActivity(), getString(R.string.toastMapBearingInterval), Toast.LENGTH_LONG).show();
							}
							return;}
							
							if(key.compareToIgnoreCase(getResources().getString(R.string.prefDeviceOptionsFloatingCriteria)) == 0 ){
								tempPref = (EditTextPreference) findPreference(key);
								try
								{
									deviceFloatingCriteria = Float.parseFloat(sharedPreferences.getString(key, "6"));
								}
								catch(Exception exc){ 
									deviceFloatingCriteria = 6;
									Toast.makeText(getActivity(), getString(R.string.toastBadInputData), Toast.LENGTH_LONG).show();
									tempPref.setText("6");
									return;
								}							
								if((locDistanceInterval < 2) || (locDistanceInterval > 50))
								{
									locDistanceInterval = 6;
									tempPref.setText("6");
									Toast.makeText(getActivity(), getString(R.string.toastLocDistanceInterval), Toast.LENGTH_LONG).show();
								}
								return;}

                if(key.compareToIgnoreCase(getResources().getString(R.string.prefWifiAttenuationFactor)) == 0 ){
                    tempPref = (EditTextPreference) findPreference(key);
                    try
                    {
                        wifiAttenuationFactor = Float.parseFloat(sharedPreferences.getString(key, "0"));
                    }
                    catch(Exception exc){
                        wifiAttenuationFactor = 0;
                        Toast.makeText(getActivity(), getString(R.string.toastBadInputData), Toast.LENGTH_LONG).show();
                        tempPref.setText("0");
                        return;
                    }
                    if(( wifiAttenuationFactor < 0) || ( wifiAttenuationFactor > 15))
                    {
                        wifiAttenuationFactor = 0;
                        tempPref.setText("0");
                        Toast.makeText(getActivity(), getString(R.string.toastWifiAttenuationFactor), Toast.LENGTH_LONG).show();
                    }
                    return;}
			}
	    	
	    }


}
	        
