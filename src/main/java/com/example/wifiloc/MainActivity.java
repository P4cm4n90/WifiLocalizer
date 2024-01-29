package com.example.wifiloc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;
import java.util.Locale;

public class MainActivity extends ActionBarActivity {

    private static final String scanInfo= "Scan status";
    private static final String locInfo= "Localization status";
    private static final String lastLocLat ="Latitude";
    private static final String lastLocLong ="Longitude";
    private PopupMenu menuAction;
    private PopupMenu menuEnviroment;

    public Bundle lastSave;

	private SubMenu tempMenu;
    private SectionsPagerAdapter mSectionsPagerAdapter;
    private FragmentMain fragmentMain;
    private FragmentMap fragmentMap;
    private FragmentWifiList fragmentWifiList;
    private NavigationManager nvManager;
    private AppSettingsFragment settings;
    private AlertDialog guideModeDialog;
    private AlertDialog guideModeDialogChangeLoc;
    private AlertDialog guideModeDialogLocNotChanged;
    private boolean guideMode = false;
    private String guideModeTag = "";
    private int guideModeIterator = 0;
    private int guideModeStepCount = 0;
    private boolean isReadyToLoc = false;
    ViewPager mViewPager;
    private boolean samplingNow = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	super.onCreate(savedInstanceState);
	       setContentView(R.layout.activity_main);
	  

	      
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the activity.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        settings = new AppSettingsFragment(){
        	@Override
        	public void onStop()
        	{
        		setContentView(R.layout.activity_main);
        	
        	}
        };
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        fragmentMain = new FragmentMain();
        fragmentMap = new FragmentMap(){

            @Override
            public void onLocationChanged(Location location)
            {
                    fragmentMain.updateLocationInfo(location);
                    nvManager.locationChanged(location);

            }


        };
        fragmentWifiList = new FragmentWifiList(){
        	
        	@Override
        	public void onItemSelected(String bssid, int actionNumber)
        	{
        			switch(actionNumber)
        			{
        				case 0:
        					try{       
			        			if(nvManager.checkDeviceDataReady(bssid))
						        {
			        				nvManager.findDevice(bssid
						        	,Float.parseFloat(PreferenceManager.getDefaultSharedPreferences
						        	(getApplicationContext()).getString(getString(R.string.prefWifiPropagationFactor), "3"))
						        	, Float.parseFloat(PreferenceManager.getDefaultSharedPreferences
                                            (getApplicationContext()).getString(getString(R.string.prefWifiAttenuationFactor), "0")));

                                    if((PreferenceManager.getDefaultSharedPreferences
                                        (getApplicationContext()).getBoolean(getString(R.string.prefMarkSamplePoint), false)))
                                    {
                                        fragmentMap.drawMeasurementsPoints(nvManager.getLocPoints(bssid));
                                    }
						        }
			        						
							    else
							    {
							        	AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
				
							        				
							        	builder.setMessage("Nie zebrano jeszcze wystarczającej ilości danych do poprawnego zlokalizowania"
							        						+ " urządzenia, zmien lokalizacje i zbieraj dane")
							        				       .setTitle("Niepowodzenie");
				
							        				
							        	AlertDialog dialog = builder.create();
							        	dialog.show();
							      }
			        						
			        		}
			            	catch(NumberFormatException nexp)
			            	{
			            		Log.e("Localization failed ",nexp.getMessage());
			            	}
			            	catch(ClassCastException cexp)
			            	{
			            		Log.e("Localization failed ",cexp.getMessage());
			            	}
        					return;
          				case 1:

		     
        					return;
        				case 2:
        					if(nvManager.isLocFound(bssid))
        					{
        						mViewPager.setCurrentItem(1);
        	          
    							fragmentMap.showMarker(bssid);
        					}
        					else
        					{
			        				AlertDialog.Builder abuilder = new AlertDialog.Builder(getActivity());

			        				abuilder.setMessage("Nie wykryto jeszcze tego urządzenia")
			        				       .setTitle("Niepowodzenie")
			        				       .setNeutralButton("ok", new OnClickListener(){
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												dialog.dismiss();												
											}		        				    	   
			        				       });
			        				AlertDialog dialog = abuilder.create();
			        				dialog.show();
        							
        						}       					
        					return;
        				case 3:
        					if(nvManager.isLocFound(bssid))
        					{
        						mViewPager.setCurrentItem(1);
        						fragmentMap.startNavigation(bssid);
        					}
        					return;
        	}
        	}
        };
        				
        	
        
        	
    
        	
        	
        
        
        WifiManager wmManager = (WifiManager) getSystemService(WIFI_SERVICE);
        LocationManager lmManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        nvManager = new NavigationManager(this,true){



        	@Override
        	public void onProviderChanged(String provider){
        		fragmentMain.updateLocationProvider(provider);
        		
        	}

        	@Override
        	public void trueLocationChanged(Location location)
        	{
                if(fragmentWifiList != null)
                {
                    if(fragmentWifiList.isVisible()){
        		    fragmentWifiList.setWifiRefreshLocationReadiness("all",false);}
                }
        	}
        	
        	@Override
        	public void onNewWifiInfo(List<ScanResult> scanres)
        	{
       	   	if(scanres != null)
            	{
                    ScanResult[] tempScanRes = scanres.toArray(new ScanResult[1]);

                    for (ScanResult scResult : tempScanRes)
                    {
                        if (scResult == null) { return; }
                        if(fragmentMap.checkMarkerExist(scResult.BSSID))
                        {
                            fragmentMap.updateMarkerData(new DeviceSettings(scResult)); //TODO sila sygnalu - nieznana :)
                        }
                    }

                    fragmentWifiList.updateWifiList(scanres);
            			
                }
        	}

        };

        
        if(savedInstanceState != null)
        {
        	lastSave = savedInstanceState;
        	       	
        }
     
    }

    @Override 
    public void onSaveInstanceState(Bundle outState)
    {
    	outState.putBoolean(scanInfo, fragmentMain.scanStatus);
    	outState.putBoolean(locInfo, fragmentMain.ownLocStatus);
    	try{
	    	outState.putDouble(lastLocLat, nvManager.getLastKnownLocation().getLatitude());
	    	outState.putDouble(lastLocLong, nvManager.getLastKnownLocation().getLongitude());
    	}
    	catch(Exception e){
    	return;}
    	
    }
    
    @Override
    public void onDestroy()
    {
       	stopLocalising();
    	stopScanning();
    	super.onDestroy();
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {





        return true;
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

    	getMenuInflater().inflate(R.menu.main, menu);

        // Inflate the menu; this adds items to the action bar if it is presen 
        return true;
    }

    @Override
    public void onBackPressed()
    {
    	if(settings.isVisible() == true)
    	{
    		//(hiddenFragment != null){
	    		getFragmentManager().beginTransaction().hide(settings).commit();
	    		 getSupportFragmentManager().beginTransaction()
	        	 .show(fragmentMain)
	        	 .show(fragmentMap)
	        	 .show(fragmentWifiList)
	        	 .commit();

    		//}
    	}
    	else{
    	super.onBackPressed();}
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        SubMenu actMenu;

        switch(id){
        

	        
	        case (R.id.action_settings):
	        	//Intent intent = new Intent();
	           // intent.setClass(this, AppSettings.class);
	           // startActivityForResult(intent, 0); 
	        	if(settings != null){
	        	getFragmentManager().beginTransaction()
	            .replace(android.R.id.content, (settings = new AppSettingsFragment())).commit();}
	        	else{
	        		if(settings.isHidden() == true)
	        		{
	        			getFragmentManager().beginTransaction().show(settings).commit();
	        		}
	        		else
	        		{
	        			getFragmentManager().beginTransaction()
	                    .replace(android.R.id.content, (settings = new AppSettingsFragment())).commit();
	        		}
	        	}
	        	
	        	// hiddenFragment = fragmentVisible();
	        	 getSupportFragmentManager().beginTransaction()
	        	 .hide(fragmentMain)
	        	 .hide(fragmentMap)
	        	 .hide(fragmentWifiList)
	        	 .commit();
	
	            return true;
	            
	            
	        case (R.string.menuStopNavigation):
	        	stopNavigation();
	        	return true;
	        	
	        case(R.string.menuSampleData):  // TODO upgrade
	            if(!nvManager.sampleData(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences
                    (getApplicationContext()).getString(getString(R.string.prefWifiMinMeasure), "1"))))
                {
                    Toast.makeText(this,"Zmien lokalizacje zanim pobierzesz kolejne probki",Toast.LENGTH_LONG).show();
                    return true;
                }
	            samplingNow = true;
	        	return true;

            case(R.string.menuWipeAll): // TODO upgrade
                nvManager.wipeData();
                samplingNow = false;
                return true;
	        	
	        case (R.string.menuSaveData):
                boolean saveMeasure = (PreferenceManager.getDefaultSharedPreferences
						(getApplicationContext()).getBoolean(getString(R.string.prefSaveData), false));
	        	String filename = PreferenceManager.getDefaultSharedPreferences
						(getApplicationContext()).getString(getString(R.string.prefSaveDataFileName), "StoredData");
	        	new SaveDataTask().execute(filename,String.valueOf(saveMeasure));
	        	return true;
	        	
	        case(R.string.menuLoadData):
                boolean loadMeasure = (PreferenceManager.getDefaultSharedPreferences
                        (getApplicationContext()).getBoolean(getString(R.string.prefSaveData), false));
	            	String lfilename = PreferenceManager.getDefaultSharedPreferences
	    					(getApplicationContext()).getString(getString(R.string.prefSaveDataFileName), "StoredData");
	            	new LoadDataTask().execute(lfilename,String.valueOf(loadMeasure));
	            	return true;
            case(R.string.menuLocaliseAll):
                float nsf = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences
                        (getApplicationContext()).getString(getString(R.string.prefWifiPropagationFactor), "3"));
                float FAF = Float.parseFloat(PreferenceManager.getDefaultSharedPreferences
                        (getApplicationContext()).getString(getString(R.string.prefWifiAttenuationFactor), "0"));
                int minLoc = Integer.parseInt(PreferenceManager.getDefaultSharedPreferences
                        (getApplicationContext()).getString(getString(R.string.prefWifiMinLoc), "4"));
                int minRss =  Integer.parseInt(PreferenceManager.getDefaultSharedPreferences
                        (getApplicationContext()).getString(getString(R.string.prefWifiMinMeasure), "2"));
                if(!nvManager.findAvailableDevices(nsf,FAF,minLoc,minRss))
                {
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);


                    builder.setMessage("Nie zebrano jeszcze wystarczającej ilości danych do poprawnego zlokalizowania"
                            + " żadnego z urządzeń")
                            .setTitle("Niepowodzenie");


                    AlertDialog dialog = builder.create();
                    dialog.show();
                }
                return true;
            case(R.string.menuHideMarker):
                fragmentMap.hideMarkers();
                return true;
            case(R.string.menuMarkRealApLoc):
                fragmentMap.markRealApLoc();
                LatLng _loc = fragmentMap.getAPLocation();
                Location _realLoc = new Location("gps");
                _realLoc.setLatitude(_loc.latitude);
                _realLoc.setLongitude(_loc.longitude);
                nvManager.setRealApLocation(_realLoc);
                return true;
            case(R.id.action_enviroment):
                tempMenu = item.getSubMenu();

                return true;
            case(R.id.action_act):
                SubMenu menu = item.getSubMenu();
                if(isReadyToLoc)
                {
                    if(!menu.findItem(R.string.menuLocaliseAll).isEnabled()){
                        menu.findItem(R.string.menuLocaliseAll).setEnabled(true);}
                }
                if(samplingNow == true)
                {

                    menu.findItem(R.string.menuSampleData).setEnabled(false);
                }
                else
                {
                    if(!menu.findItem(R.string.menuSampleData).isEnabled()){
                        menu.findItem(R.string.menuSampleData).setEnabled(true);
                    }
                }

                if(fragmentMap.isNagivationModeEnable())
                {
                    menu.findItem(R.string.menuStopNavigation).setEnabled(true);
                }
                else
                {
                    if(menu.findItem(R.string.menuStopNavigation).isEnabled()){
                        menu.findItem(R.string.menuStopNavigation).setEnabled(false);
                    }
                }
                if(fragmentMap.isAnyMarkerMaximalized())
                {
                    if(!menu.findItem(R.string.menuHideMarker).isEnabled()){
                        menu.findItem(R.string.menuHideMarker).setEnabled(true);
                    }
                }
                else
                {
                    if(menu.findItem(R.string.menuHideMarker).isEnabled()){
                        menu.findItem(R.string.menuHideMarker).setEnabled(false);
                    }
                }
                return true;
            case R.string.menuFreeSpace:
                if(!nvManager.getIndoorPropagation()){}
                else
                {
                    nvManager.setIndoorPropagation(false);
                }

                tempMenu.findItem(R.string.menuIndoor1).setChecked(false);
                tempMenu.findItem(R.string.menuIndoor2).setChecked(false);
                tempMenu.findItem(R.string.menuIndoor3).setChecked(false);
                tempMenu.findItem(R.string.menuFreeSpaceObject).setChecked(false);
                tempMenu.findItem(R.string.menuFreeSpace).setChecked(true);
            return true;

            case R.string.menuFreeSpaceObject:
                if(!nvManager.getIndoorPropagation()){}
                else
                {
                    nvManager.setIndoorPropagation(false);
                }

                tempMenu.findItem(R.string.menuIndoor1).setChecked(false);
                tempMenu.findItem(R.string.menuIndoor2).setChecked(false);
                tempMenu.findItem(R.string.menuIndoor3).setChecked(false);
                tempMenu.findItem(R.string.menuFreeSpace).setChecked(false);
                tempMenu.findItem(R.string.menuFreeSpaceObject).setChecked(true);
                return true;

            case R.string.menuIndoor1:
                if(nvManager.getIndoorPropagation()){}
                else
                {
                    nvManager.setIndoorPropagation(true);
                }

                tempMenu.findItem(R.string.menuFreeSpace).setChecked(false);
                tempMenu.findItem(R.string.menuIndoor2).setChecked(false);
                tempMenu.findItem(R.string.menuIndoor3).setChecked(false);
                tempMenu.findItem(R.string.menuFreeSpaceObject).setChecked(false);
                tempMenu.findItem(R.string.menuIndoor1).setChecked(true);
                return true;

            case R.string.menuIndoor2:
                if(nvManager.getIndoorPropagation()){}
                else
                {
                    nvManager.setIndoorPropagation(true);
                }

                tempMenu.findItem(R.string.menuIndoor1).setChecked(false);
                tempMenu.findItem(R.string.menuFreeSpace).setChecked(false);
                tempMenu.findItem(R.string.menuIndoor3).setChecked(false);
                tempMenu.findItem(R.string.menuFreeSpaceObject).setChecked(false);
                tempMenu.findItem(R.string.menuIndoor2).setChecked(true);
                return true;

            case R.string.menuIndoor3:
                if(nvManager.getIndoorPropagation()){}
                else
                {
                    nvManager.setIndoorPropagation(true);
                }

                tempMenu.findItem(R.string.menuIndoor1).setChecked(false);
                tempMenu.findItem(R.string.menuFreeSpace).setChecked(false);
                tempMenu.findItem(R.string.menuIndoor2).setChecked(false);
                tempMenu.findItem(R.string.menuFreeSpaceObject).setChecked(false);
                tempMenu.findItem(R.string.menuIndoor3).setChecked(true);
                return true;




      	
        }
        return super.onOptionsItemSelected(item);
    }

    public void initializeGuideModeDialogs()
    {


    }

    public Fragment fragmentVisible()
    {
    	
    	if(fragmentMain.isVisible())
    	{ return fragmentMain;}
    	if(fragmentMap.isVisible())
    	{ return fragmentMap;}
    	if(fragmentWifiList.isVisible())
    	{ return fragmentWifiList;}
    	return null;
    }
    public Location getLastKnownLocation(){
    	return nvManager.getLastKnownLocation();
    }
   
    public void startScanning()
    {
    	try{
    		nvManager.addListener(new DataObserverInstance());
    		nvManager.startScanning(Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.prefWifiInterval), "2000")));
    	}
    	catch(NumberFormatException nexp)
    	{
    		Log.e("cannot start scan",nexp.getMessage());
    	}
    	catch(ClassCastException cexp)
    	{
    		Log.e("cannot start scan",cexp.getMessage());
    	}
    }
    
    public void onFirstFixAvailable()
    {
    	fragmentMain.firstFixAvailable = true;
    }
    
    public boolean startLocalizing()
    {
    	try{
	    	if(nvManager.isWorking == false) //TODO update this
	    	{
               // fragmentMap.setLocalisation(true);
	    		nvManager.startPositioning(
	    				PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.prefLocProvider), LocationManager.GPS_PROVIDER)
	    				, Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.prefLocDistanceInterval), "1"))
	    				, Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(this).getString(getString(R.string.prefLocInterval), "500"))
	    				);
	    	}
    	}
    	catch(NumberFormatException nexp)
    	{
    		Log.e("cannot start scan",nexp.getMessage());
    		return false;
    	}
    	catch(ClassCastException cexp)
    	{
    		Log.e("cannot start scan",cexp.getMessage());
    		return false;
    	}
    		return true;   
    }

    ///FOR HANDLING SCREEN ROTATION
    public void stopLocalising()
    {
    		nvManager.stopPositioning();
    	
    }
  ///FOR HANDLING SCREEN ROTATION
    public void stopScanning()
    {
    	if(nvManager.isWorking == true)
    	{
    		nvManager.stopScanning();
    	}
    }

    
	public void startNavigation(String bssid)
	{
		fragmentMap.startNavigation(bssid);
	}
	
	public void stopNavigation()
	{
		fragmentMap.stopNavigation();
	}

	public boolean isNavigationModeEnabled()
	{
		return fragmentMap.isNagivationModeEnable();
	}
	
	public String getNavigationModeTag()
	{
		return fragmentMap.getNavigationTag();
	}



	@Override
    public void onConfigurationChanged(Configuration newConfig) {
      super.onConfigurationChanged(newConfig);
    }



    


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
            
        }
       
        @Override
        public Fragment getItem(int position) {
            switch (position) {
            case 0:
                return fragmentMain;
            case 1:
                return fragmentMap;
            case 2:
            	return fragmentWifiList;
            }
            return null;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
            }
            return null;
        }
    }

    /**
     * A placeholder fragment containing a simple view.
     */
    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        public PlaceholderFragment() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment_main, container, false);
            TextView textView = (TextView) rootView.findViewById(R.id.section_label);
            textView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }
    }

    class DataObserverInstance implements DataObserver
    {
   
		@Override
		public void onDataCollected(final String bssid) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fragmentWifiList.setWifiRefreshLocationReadiness(bssid,true);
                    fragmentWifiList.updateWifiListInfo();
                }
            });
			
		}

		@Override
		public void onResultsAvailable(DeviceLocation loc) {
			Location tempLoc = nvManager.getLastKnownLocation();
			Location refLocation = new Location("null");
			refLocation.setLatitude(loc.location.latitude);
			refLocation.setLongitude(loc.location.longitude);
			if(tempLoc.distanceTo(refLocation) > 1000)
			{
				Toast.makeText(getApplicationContext(), "Terminal o adresie "+loc.bssid+
						" został niepoprawnie zlokalizowany, czyszczenie danych pomiarowych", Toast.LENGTH_LONG).show();
				nvManager.wipeBadData(loc.bssid);
			}
			fragmentMap.addMarker(loc);
            fragmentMap.updateMarkerData(fragmentWifiList.getLastResult(loc.bssid));
			fragmentWifiList.addLocationToItem(loc.bssid, loc.location); //TODO add accuracy to item

			nvManager.addDeviceLocation(loc);

		}

		@Override
		public int[] getSettings() {
			// TODO Auto-generated method stub
			int[] result = {
			Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getString(R.string.prefWifiMinMeasure), "10")),
            Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(getApplicationContext()).getString(getString(R.string.prefWifiMinLoc), "7"))};

    return result;
}

        @Override
        public void onReadyToLoc(String bssid)
        {
            fragmentWifiList.setWifiLocalizationReadiness(bssid,true);
            isReadyToLoc = true;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fragmentWifiList.updateWifiListInfo();
                }
            });
        }

		@Override
		public void omGuideModeLocationChange() {
			// TODO Auto-generated method stub
			
		}

        @Override
        public void onGuideModeNewData() {
            samplingNow = false;

        }

		@Override
		public synchronized void foundFloatingDevice(String bssid,int floatingLocNumber) {
            fragmentWifiList.addFloatingDevice(bssid);
            fragmentMap.addMarker(nvManager.getDevLocationAtNumber(floatingLocNumber));
            Toast.makeText(getApplicationContext(),"Znaleziono ruchomy terminal o adresie "+bssid,Toast.LENGTH_LONG).show();
		}
		
		
    	
    }

    class SaveDataTask extends AsyncTask<String,Void,Void>
    {

		@Override
		protected Void doInBackground(String... params) {
			String filename = (String) params[0];
			boolean saveMeasurements = new Boolean( params[1]);
			nvManager.saveDataToFile(filename, saveMeasurements);
			return null;
		}
    	
    }

    class LoadDataTask extends AsyncTask<String,Void,Void>
    {

		@Override
		protected Void doInBackground(String... params) {
			String filename = params[0];
			boolean saveMeasurements = new Boolean(params[1]);
			nvManager.loadDataFromFile(filename);
			if(saveMeasurements)
			{
				nvManager.loadMeasurementsDataFromFile(filename);
			}
			return null;
		}
    	
    }

    class TCommandMenuListener implements CommandMenuListener
    {
        public void scanClicked()
        {
            nvManager.startScan(); //TODO ponder about changing scan on demand etc...
        }

        public void enterLocation(Location _location)
        {
            if(!fragmentMap.isCustomLocationTrace())
            {
                fragmentMap.setCustomLocationTrace();
            }
            fragmentMap.setCustomLocation(_location);

        }

    }
}

