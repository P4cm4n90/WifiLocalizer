package com.example.wifiloc;

import android.app.Service;
import android.content.Context;
import android.content.res.Configuration;
import android.location.GpsStatus.Listener;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;


public class FragmentMain extends Fragment {

    private static String scanInfo= "Scan status";
    private static String locInfo= "Localization status";
    private static String lastLocLat ="Latitude";
    private static String lastLocLong ="Longtitude";
    
    private Bundle lastSave;
	
	public boolean gpsStatus = false;
	public boolean ownLocStatus = false;
	public boolean scanStatus = false;

	private String textLocation = "nieznana";
	private String textProvider = "nieznany";
	
	private Listener gpsStatusListener;
	// true only for testing :-)
	public boolean firstFixAvailable = true;
	private String locationProvider = "GPS";
	LocationManager lmManager;
	TextView tLocation;
	TextView tLocationProvider;
	TextView tTransFound;
	TextView tLocStatus;
	TextView tErrors;
	TextView tScanStatus;
	TextView tDevFound;
	public Button bGpsChange;
	public Button bStartScan;

	

	

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
			//lmManager.addGpsStatusListener(listener)
        lmManager = (LocationManager) getActivity().getSystemService(Service.LOCATION_SERVICE);
		lastSave = savedInstanceState;
    }
	
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

		 if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
			 return  inflater.inflate(R.layout.fragment_main_landscape, container,false);

		    } else {
		    	return  inflater.inflate(R.layout.fragment_main, container,false);
		    }
		
	}
	
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
	    super.onConfigurationChanged(newConfig);
	    LayoutInflater inflater = LayoutInflater.from(getActivity());
	    ViewGroup viewGroup = (ViewGroup) getView();
	        
	        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
	            viewGroup.removeAllViewsInLayout();
	            inflater.inflate(R.layout.fragment_main_landscape, viewGroup);
	            initialize();

	        } else {
	            viewGroup.removeAllViewsInLayout();
	            inflater.inflate(R.layout.fragment_main, viewGroup);
	            initialize();
	        }
	    
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState){

		    outState.putDouble(lastLocLat, ((MainActivity)getActivity()).getLastKnownLocation().getLatitude());
		    outState.putDouble(lastLocLong, ((MainActivity)getActivity()).getLastKnownLocation().getLongitude());
	}
	
	@Override
    public void onStart() {
		super.onStart();
        initialize();
        /* lokalizacja pasywna, internet poki co dla testow
        bGpsChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if(gpsStatus == false){
            		
                ((MainActivity) getActivity()).startLocalizing();  
                gpsStatus = true;
                ownLocStatus = true;
                Toast.makeText(getActivity(),"Uruchomiono usługi lokalizacyjne",Toast.LENGTH_SHORT).show();
                tLocStatus.setText("Lokalizowanie w toku...");}
            	
        }}); */

        
        WifiManager wifi = (WifiManager)getActivity().getSystemService(Context.WIFI_SERVICE);
        if (!wifi.isWifiEnabled()){
        	Toast.makeText(getActivity(), "Wifi wyłączone. Aby umożliwić działanie aplikacji uruchom wifi. Uruchamiam wifi", Toast.LENGTH_LONG).show();
        	wifi.setWifiEnabled(true);
        }
        
        updateLocationProvider(PreferenceManager.getDefaultSharedPreferences
        		(getActivity()).getString(getString(R.string.locProvider), LocationManager.GPS_PROVIDER));
        Bundle savedInstanceState;
        
        if ((savedInstanceState=((MainActivity)getActivity()).lastSave) != null)
        {
              	
        	boolean tscanStatus = savedInstanceState.getBoolean(scanInfo);
        	boolean tlocStatus = savedInstanceState.getBoolean(locInfo);
        	double tlastLat = savedInstanceState.getDouble(lastLocLat);
        	double tlastLong = savedInstanceState.getDouble(lastLocLong);
        	
        	if(tscanStatus)
        	{
        		startScan();
        	}
        	if(tlocStatus)
        	{
        		startLoc();
        	}
        	if(tlastLat != 0 || tlastLong != 0)
        	{
        		Location temploc = new Location(locationProvider);
        		temploc.setLatitude(tlastLat);
        		temploc.setLongitude(tlastLong);
        		updateLocationInfo(temploc);
        	}
        	
        	((MainActivity)getActivity()).lastSave = null;
        }
        if(lastSave != null)
        {
        	double tlastLat = savedInstanceState.getDouble(lastLocLat);
        	double tlastLong = savedInstanceState.getDouble(lastLocLong);
        	
        	if(tlastLat != 0 || tlastLong != 0)
        	{

        		Location temploc = new Location(locationProvider);
        		temploc.setLatitude(tlastLat);
        		temploc.setLongitude(tlastLong);
        		updateLocationInfo(temploc);
        	}
        	lastSave = null;
        }
        }
	
	
	public void startLoc()
	{
            ((MainActivity) getActivity()).startLocalizing(); 
            updateLocationProvider(PreferenceManager.getDefaultSharedPreferences
            		(getActivity()).getString(getString(R.string.locProvider), LocationManager.GPS_PROVIDER));
            Toast.makeText(getActivity(),"Uruchomiono usługi lokalizacyjne",Toast.LENGTH_SHORT).show();
            tLocStatus.setText("Lokalizowanie w toku...");
	}
	
	public void stopLoc()
	{
			//ownLocStatus = false;
			Toast.makeText(getActivity(),"Wyłączono usługi lokalizacyjne",Toast.LENGTH_SHORT).show();
			tLocStatus.setText("");
			((MainActivity) getActivity()).stopLocalising();
	}
	
	public void startScan()
	{
		if(firstFixAvailable){
			((MainActivity)getActivity()).startScanning();
			tScanStatus.setText("Skanowanie w toku...");
			scanStatus = true;}
		else{
			Toast.makeText(getActivity(),"Wyłączone skanowanie dla nieznanej lokalizacji",Toast.LENGTH_SHORT).show();}
	}
	
	public void stopScan()
	{
		scanStatus = false;
		tScanStatus.setText("");
		((MainActivity)getActivity()).stopScanning();
	}
	public void initialize()
	{
		tLocation = (TextView) getView().findViewById(R.id.tSelfLoc);
		tLocationProvider = (TextView) getView().findViewById(R.id.tLocProvider);
		//tTransFound = (TextView) getView().findViewById(R.id.tLocStatus);
		bGpsChange = (Button) getView().findViewById(R.id.bSetGps);
		bStartScan = (Button) getView().findViewById(R.id.bStartScan);
		tScanStatus = (TextView) getView().findViewById(R.id.tScanStatus);
		tLocStatus = (TextView)getView().findViewById(R.id.tLocStatus);
		tErrors = (TextView) getView().findViewById(R.id.tErrors);
		tDevFound= (TextView) getView().findViewById(R.id.tDevFound);
		if(ownLocStatus == true)
			tLocStatus.setText("Lokalizowanie w toku...");
		if(scanStatus == true)
			tScanStatus.setText("Skanowanie w toku...");
		tLocationProvider.setText(textProvider);
		tLocation.setText(textLocation);

        if(ownLocStatus == false){
            bGpsChange.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_localize));}
        else{
            bGpsChange.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_localize_end));
        }

        if(scanStatus == false){
            bStartScan.setText(getString(R.string.startScan));
        }
        else{
            bStartScan.setText(getString(R.string.stopScan));
        }

        bGpsChange.setOnClickListener(new View.OnClickListener() {
            @SuppressWarnings("deprecation")
			@Override
            public void onClick(View v) {
        		if(ownLocStatus == false){
        			startLoc();
        			ownLocStatus = true;
        			bGpsChange.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_localize_end));}
        		else{
        			bGpsChange.setBackgroundDrawable(getResources().getDrawable(R.drawable.button_localize));
        			stopLoc();
        			ownLocStatus = false;
        			}
            	
        }});
        bStartScan.setOnClickListener(new View.OnClickListener() {
        	@Override
        	public void onClick(View v) {
        		if(scanStatus == false){
        			startScan();
        			bStartScan.setText(getString(R.string.stopScan));
        			scanStatus = true;
        		}
        		else{
        			stopScan();
        			bStartScan.setText(getString(R.string.startScan));
        			scanStatus = false;
        		}
        		
        		
        		
        	}
        });
		//tLocationProvider.setText("text");
	}
	

	
	public void updateLocationProvider(String provider)
	{
			if((provider.compareToIgnoreCase(LocationManager.GPS_PROVIDER)) == 0){
				tLocationProvider.setText("GPS");
				textProvider = "GPS";
				locationProvider = LocationManager.GPS_PROVIDER;
				return;}
			if((provider.compareToIgnoreCase(LocationManager.NETWORK_PROVIDER))==0){
				tLocationProvider.setText("Internet");
				textProvider="Internet";
				locationProvider = LocationManager.NETWORK_PROVIDER;
				return;}
			if((provider.compareToIgnoreCase(LocationManager.PASSIVE_PROVIDER))==0){
					tLocationProvider.setText("Pasywna");
					textProvider = "Pasywna";
					locationProvider = LocationManager.PASSIVE_PROVIDER;
					return;}
        if((provider.compareToIgnoreCase("fuse"))==0){
            tLocationProvider.setText("Fuzja danych lokalizacyjnych");
            textProvider = "Fuzja danych lokalizacyjnych";
            locationProvider = "fuse";
            return;}
	}
	
//	public void 

	
	// to check
	public boolean updateLocationInfo(Location location)
	{
		if(this.isVisible() == true)
		{
			int lat = (int) ((location.getLatitude()) * 1E6);
			int longt = (int) ((location.getLongitude()) * 1E6);
			@SuppressWarnings("static-access")
			String strLongitude = location.convert(location.getLongitude(), location.FORMAT_SECONDS);
			@SuppressWarnings("static-access")
			String strLatitude = location.convert(location.getLatitude(), location.FORMAT_SECONDS);
			tLocation.setText(strLatitude +" N "+strLongitude+" E ");;
			textLocation= strLatitude +" N "+strLongitude+" E ";
			updateLocationProvider(textProvider = location.getProvider());
			return true;
			
		}
		else
		{
			return false;
		}
		
	}
	

	

	
}

