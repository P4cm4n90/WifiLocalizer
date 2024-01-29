package com.example.wifiloc;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

public class NavigationManager  {

	
	private final static String measureSaveAddon = "measurements";
	private final static String measureSavePrefix = "[Measurement]";
	private final static String measureSavePrefixDevice ="[Device]";
	private final static String measureSavePrefixData = "[Signal]";
	private final static String measureSavePrefixLocation = "[Location]";
	private final static String measureSavePrefixTime = "[Time]";
	private final static String measureSavePrefixEnd = "[End]";
	private final static String measureSavePrefixAccuracy = "[Accuracy]";
	private final static String measureSavePrefixLocationPoints = "[Receiver location points]";
	private final static String measureSavePrefixSamplesCount = "[Samples per location point]";
    private final static String measureError = "[Error]";

	private final static String filePath = "/Android/data/com.example.wifiloc/files/";
	
	private LocationManager lmManager;
	private WifiManager wmManager;
	private Location lActualLocation;
	private Location tempLocation;
	private Location realApLocation;
	private Location prevSampleLoc;
	private Context context;
	private Map<String,NavigationData> data;
	private List<DeviceLocation> lDevLocation;
	private String locationProvider;
	private WifiReceiver wifiReceiver;
	private ArrayList<DataObserver> dataObservers = new ArrayList<DataObserver>();

	private Timer scanTimer;
	private TimerScanTask scanTimerTask;
	private LocationListener locationListener;
	private LocationListener gpsLocationListener;
	private gpsStatusListener gpsListener;

    private float minDistLocUpdate = 0;
    private float minTimeLocUpdate = 0;
    private int measurePointsDone = 0;
    private long tempTime = 0;
    private int scanMaxTime = 15;
	private int samplesQuantity;
	private int maxDevices;
	private long locationUpdateTime;
	private float locationUpdateDistance;
    private boolean indoorPropagation = false;
	private boolean gpsPositioning;
    public boolean isScanningNow = false;
	public boolean isWorking;

	public boolean scanLock = false;
    public boolean locLock = false;
    public int scanLockCounter = 0;
    public int scanLockMax = -1;



	
	public NavigationManager(Context context,boolean gpsPositioning) 
	{

		this.context = context;
		this.lmManager = (LocationManager) context.getSystemService(context.LOCATION_SERVICE);
		this.wmManager = (WifiManager) context.getSystemService(context.WIFI_SERVICE);
		this.locationUpdateDistance = 0;
		this.locationUpdateTime = 0;
		this.samplesQuantity = 1000;
		this.maxDevices = 100;
		this.isWorking = false;
		this.gpsPositioning = gpsPositioning;
		lDevLocation = new ArrayList<DeviceLocation>(maxDevices*10);
		lActualLocation = null;
		scanTimer = new Timer();
		scanTimerTask = new TimerScanTask();

    }



	

	// We assume that for us it's impossible to found two devices with the same mac adress 
	//



	// run on getting first gps fix
	public void onProviderChanged(String provider){};

	public void trueLocationChanged(Location location){};

	public void locationChanged(Location location)
    { //TODO add criteria


       if(!locLock){
            if(location != lActualLocation)
                if(tempTime == 0)
                {
                    lActualLocation = location;
                    tempTime = System.nanoTime();
                    trueLocationChanged(location);
                }
                else
                {
                long timedif = 0;
                    if( (timedif =(System.nanoTime() - tempTime) )> minTimeLocUpdate)
                    {
                        tempTime = System.nanoTime();
                        float dist = 0;
                        if( (dist = location.distanceTo(lActualLocation)) > minDistLocUpdate)
                        {
                            lActualLocation = location;
                            trueLocationChanged(location);
                        }
                    }
                }

        }
        {
            tempLocation = lActualLocation;
        }
    }

	
	public void onNewWifiInfo(List<ScanResult> scanRes){};

	/**
	Method used for starting self-localization
	@param locationProvider - String contains location provider name
	@param minDistance - float represents minimal distance value in metres between location updates
	@param minTime - long represnts minimum time interval (in seconds) between location updates
	@return true if positioning successfully stared or false if provider is not enabled or its null

	*/
	public boolean startPositioning(String locationProvider, float minDistance, long minTime)
	{
		if(gpsPositioning == true){
			onProviderChanged(locationProvider);
			this.locationUpdateDistance = minDistance;
			this.locationUpdateTime = minTime;
			this.locationProvider = locationProvider;
			locationListener = new PositionListener();
			gpsLocationListener = new PositionListener();
			gpsListener = new gpsStatusListener();

			if(lmManager.isProviderEnabled(locationProvider) == false){
				Toast.makeText(context, 
						"Dostawca lokalizacji "+locationProvider+" jest niedostępny, zmień dostawcę i spróbuj jeszcze raz",
						Toast.LENGTH_LONG);
				return false;
			}
			try{
			    minDistLocUpdate = minDistance;
			    minTimeLocUpdate = minTime*1000000;
			/*	if(locationProvider = LocationManager.GPS_PROVIDER){

					//lmManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, gpsLocationListener);
				}
				else
				{
					lmManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, minTime, minDistance, gpsLocationListener);
				}*/

				lmManager.addGpsStatusListener(gpsListener);
				return true;
			}
			catch(IllegalArgumentException exp){
				return false;
		}}
		return true;
	}

	//method run after the first gps fix :-) --- to consider in future;
	
	public void startScanning(int scanInterval)
	{
	
		try{/*
       		Date data = new Date(lmManager.getLastKnownLocation(LocationManager.GPS_PROVIDER).getTime());
		if(data.after(new Date(lmManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER).getTime())))
		{
			lActualLocation = lmManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			locationChanged(lActualLocation);
		}
		else
		{
			lActualLocation = lmManager.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			locationChanged(lActualLocation);
		}*/
		
		wifiReceiver = new WifiReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(WifiManager.EXTRA_NEW_RSSI);
		intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
		intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
		context.registerReceiver(wifiReceiver, intentFilter);
      // startScanActiveMethod.invoke(wmManager);

		startScan();
		isWorking = true;
		}
		catch(Exception ex)
		{
            wifiReceiver = new WifiReceiver();
            IntentFilter intentFilter = new IntentFilter();
            intentFilter.addAction(WifiManager.EXTRA_NEW_RSSI);
            intentFilter.addAction(WifiManager.RSSI_CHANGED_ACTION);
            intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
            context.registerReceiver(wifiReceiver, intentFilter);
            // startScanActiveMethod.invoke(wmManager);
            startScan();
			ex.printStackTrace();
		}
	}
	
	public void stopPositioning()
	{
		try{
			if(locationProvider == LocationManager.GPS_PROVIDER)// TODO locationProvider == null ?!
			{
				//lmManager.removeUpdates(locationListener);
				lmManager.removeGpsStatusListener(gpsListener);
			}
			else
			{
				//lmManager.removeUpdates(gpsLocationListener);// TODO why its null ?!
				//lmManager.removeUpdates(locationListener);
				lmManager.removeGpsStatusListener(gpsListener);
			}
		}
		catch(RuntimeException ex)
		{ 
			Log.e("Error stop scanning",ex.getMessage());
		}
	}
	
	public void stopScanning()
	{
		try{
			context.unregisterReceiver(wifiReceiver);

		}
		catch(RuntimeException ex)
		{ 
			Log.e("Error stop scanning",ex.getMessage());
		}
		isWorking = false;
	}
	
	public Location getLastKnownLocation()
	{
		return lActualLocation;
	}
	
	public void findDevice(String bssid, float nFactor, double FAF)
	{ 
		getDeviceData(bssid).findDevice(nFactor, FAF);
	}

    public boolean findAvailableDevices(float nFactor, double FAF,int minLoc,int minRss) // TODO implement
    {
        List<String> listDev = new ArrayList<String>();

        for( String s : data.keySet())
        {
            if(data.get(s).isDataCollected())
            {
                listDev.add(s);
            }
        }
        if (listDev.size() > 0)
        {
            for(String s : listDev.toArray(new String[0]))
            {
                getDeviceData(s).findDevice(nFactor, FAF);
            }
            return true;
        }
        else
        {
            return false;
        }
    }

	public boolean isLocFound(String bssid)
	{
		for(int i=0;i<lDevLocation.size();i++)
		{
			if(lDevLocation.get(i).bssid.compareToIgnoreCase(bssid) == 0)
			{
				return true;
			}
		}
		
		return false;
	}

    public boolean sampleData(int maxRss)
    {
        scanLockMax = maxRss;
                if(prevSampleLoc != null){
                    if(!sampleLocationUpdate()){
                         return false;}
                }
                scanLock = false;
                isScanningNow = true;
                locLock = true;
                measurePointsDone = measurePointsDone+1;
                return true;
    }

    public boolean sampleLocationUpdate()
    {
        float dist = lActualLocation.distanceTo(prevSampleLoc);
        if(dist < 0.5f)
        {
            return false;
        }
        else
        {
            return true;
        }
    }

    private void addData(List<ScanResult> scanResult,Location location)
    {
	new AsyncTask<Object, Void, List<ScanResult>>() {
        @Override
        protected List<ScanResult> doInBackground(Object... params) {

            List<ScanResult> scanResult = (List<ScanResult>) params[0];
            Location location = (Location) params[1];
            if(!scanLock){
            if(scanResult == null || scanResult.size() < 1){
                return null;}
            if(location == null){
                return null;}
            //to update wifilist

            if(data == null){
                //initial size 1000 for testing
                data = new HashMap<String,NavigationData>(1000);

                NavigationData nvData;

                for(int i=0; i<scanResult.size(); i++)
                {
                    nvData = new NavigationData(samplesQuantity,scanResult.get(i).BSSID);
                    if(indoorPropagation){ nvData.setIndoorLocation(true);}
                    if(!nvData.hasDataObserver){

                        @SuppressWarnings("rawtypes")
                        Iterator it = dataObservers.iterator();
                        while(it.hasNext())  {
                            nvData.addListener(((DataObserver) it.next()));
                        }
                    }
                    nvData.addItem(location, scanResult.get(i).level);

                    data.put(scanResult.get(i).BSSID , nvData);
                }

            }
            else
            {
                NavigationData nvData;

                for(int i=0; i<scanResult.size(); i++)
                {
                    String name = scanResult.get(i).BSSID;
                    if( data.get(name) != null)
                    {
                        data.get(name).addItem(location, scanResult.get(i).level);
                    }
                    else
                    {
                        nvData = new NavigationData(samplesQuantity,scanResult.get(i).BSSID);
                        if(indoorPropagation){ nvData.setIndoorLocation(true);}
                        if(!nvData.hasDataObserver){

                            Iterator it = dataObservers.iterator();
                            while(it.hasNext())  {
                                nvData.addListener(((DataObserver) it.next()));
                            }
                        }
                        nvData.addItem(location, scanResult.get(i).level);
                        data.put(scanResult.get(i).BSSID , nvData);
                    }

                }
            }
            if(isScanningNow){
            scanLockCounter=scanLockCounter+1;
            if (scanLockCounter >= scanLockMax)
            {
                scanLockCounter = 0;
                scanLock = true;


                locLock = false;
                if(tempLocation !=null){
                     lActualLocation = tempLocation;}
                for(int k = 0; k<dataObservers.size();k++)
                {
                    dataObservers.get(k).onGuideModeNewData();
                }
                prevSampleLoc = location;
            }}}

            return scanResult;
        }
        protected void onPostExecute(List<ScanResult> result) {
            onNewWifiInfo(result);
        }

    }.execute(scanResult,location);

	}
	
	private void addData(NavigationData tempNavigationData) // TODO uzywane?! bo nie widac
	{
		if(tempNavigationData == null) {throw new NullPointerException("Given object NavigationData is null!");}
		try{
			
			if(data.containsKey(tempNavigationData.tag))
			{
				NavigationData temp = data.get(tempNavigationData.tag);

				Map<Location, int[]> tempmap = tempNavigationData.getDataToSave();
				Set<Location> temploc = tempmap.keySet();
				Location[] locarray = temploc.toArray(new Location[2]);
				for(int i=0;i<locarray.length;i++)
				{
					temp.addItem(locarray[i], tempmap.get(locarray[i]));
				}
				data.put(temp.tag,temp);
			}
			else
			{
				data.put(tempNavigationData.tag,tempNavigationData);
			}
		}
		catch(Exception e)
		{
			Log.e("cannot add loaded data",e.getMessage());
		}
	}
	
	public void wipeBadData(String bssid)
	{
		data.remove(bssid);
	}
	
	public Map<String,NavigationData>  getData()
	{
		return data;
	}

	public NavigationData getDeviceData(String bssid)
	{
		try{
			return data.get(bssid);
			
		}
		catch(NullPointerException nexp)
		{
			return null;
		}
	}

	public boolean checkDeviceDataReady(String bssid)
	{
		return data.get(bssid).isDataCollected();
	}
		
	public void startScan()
	{
        try{
            new AsyncTask<Void,Void,Void>()
            {
                @Override
                protected Void doInBackground(Void... params) {
                    try{
                          //startScanActiveMethod.invoke(wmManager);
                        wmManager.startScan();
                    }
                    catch(Exception exp)
                    {
                        exp.printStackTrace();
                    }
                    return null;
                }


            };

            //wmManager.startScan();
        }
        catch(Exception ex)
        {
            Log.e("START SCAN EXCEPTION",ex.getMessage());
        }
	}

	public void setRealApLocation(Location _loc)
    {
        realApLocation = _loc;
    }

	public synchronized void addListener(DataObserver observer)
	{
		dataObservers.add(observer);
	}
	
	public synchronized void removeListener(DataObserver observer)
	{
		dataObservers.remove(observer);
	}
	
	public void addDeviceLocation(DeviceLocation devLoc)
	{	
        int lastnumber = 0;
        try{
        if(!lDevLocation.isEmpty()){

     /*   for( int i = 0;i<lDevLocation.size();i++) //TODO FOR EXPERIMENTS ONLY !!!
        {
            if(devLoc.bssid.equalsIgnoreCase(lDevLocation.get(i).bssid))
            {

                DeviceLocation tempDevLoc = lDevLocation.get(i);
                tempDevLoc.addData(devLoc);
                lDevLocation.set(i, tempDevLoc);
                floatingLocNumber = i;
                if(tempDevLoc.isMobile())
                {
                    Iterator<DataObserver> it = dataObservers.iterator();
                    while(it.hasNext())  {
                        it.next().foundFloatingDevice(devLoc.bssid,floatingLocNumber);
                    }
                }
                return;
            }
        }*/
            lDevLocation.add(devLoc);
        }
        else
        {
            lDevLocation.add(devLoc);
        }
        }
        catch(NullPointerException exp)
        {
            Log.e("add dev location",exp.getMessage());
        }
	}

    public DeviceLocation getDevLocationAtNumber(int number)
    {
        return lDevLocation.get(number);
    }

    public void wipeData()
    {
        data = new HashMap<String,NavigationData>();
        scanLock = true;
        scanLockCounter = 0;
        measurePointsDone = 0;
        isScanningNow = true;
    }


    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

	public void setIndoorPropagation(boolean enabled)
    {
        indoorPropagation = enabled;
        if(data == null){return;} if (data.size() <1){ return; }
        for(String s : data.keySet())
        {
            data.get(s).setIndoorLocation(enabled);
        }
    }

    public boolean getIndoorPropagation()
    {
        return indoorPropagation;
    }

	public Set<Location> getLocPoints(String bssid)
    {
         return getDeviceData(bssid).getUsedLocations();
    }
	public void saveDataToFile(String filename, boolean saveMeasurements)
	{

        if(!isExternalStorageWritable())
        {
            Log.w("STORAGE_ACCESS","Cannot access storage");
        }
		FileOutputStream outputStream;

		try {
            String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
            directory = directory + filePath;
            boolean dirCreate = new File(directory).mkdirs();
            File mDestFile = new File(directory+filename+measureSaveAddon+".txt");
            File destFile = new File(directory+filename+".txt");
            destFile.createNewFile();
            boolean canWrite = destFile.canWrite();
            boolean mFileExists = mDestFile.exists();
            boolean fileExists = destFile.exists();
            BufferedWriter out;

			if(saveMeasurements)
			        {

					  outputStream = new FileOutputStream(mDestFile,mFileExists);
				      out = new BufferedWriter(
				    	       new OutputStreamWriter(
				    	                  outputStream, "UTF16"));
					  
					  
					  String[] keys = Arrays.copyOf(data.keySet().toArray(),data.keySet().toArray().length,String[].class);
					  for(int i = 0; i<keys.length; i++)
					  {
						  NavigationData nvData = data.get(keys[i]);
						  
						  out.write(measureSavePrefix);
						  out.newLine();
						  out.write(nvData.tag);
						  out.newLine();
						  
						  HashMap<Location, int[]> tempData = nvData.getDataToSave();
						  Location[] tempLoc = Arrays.copyOf(tempData.keySet().toArray(),tempData.keySet().toArray().length,Location[].class);
						  
						  for(int k=0 ; k<tempData.size(); k++)
						  {
							  out.write(measureSavePrefixLocation);
							  out.newLine();
							  out.write(String.valueOf(tempLoc[k].getLatitude()));
							  out.newLine();
							  out.write(String.valueOf(tempLoc[k].getLongitude()));
							  out.newLine();
							  out.write(measureSavePrefixAccuracy);
							  out.newLine();
							  out.write(String.valueOf(tempLoc[k].getAccuracy()));
							  out.newLine();
							  out.write(measureSavePrefixData);
							  out.newLine();
							  int[] rssTempData = tempData.get(tempLoc[k]);
							  for(int z = 0; z< rssTempData.length; z++)
							  {		

								  out.write(String.valueOf(rssTempData[z]));
								  out.newLine();
							  }	
							  out.write(measureSavePrefixEnd);
                              out.flush();
                              out.close();
						  }	  
					  }
                        if(lDevLocation.size() > 0){

                            outputStream = new FileOutputStream(destFile,fileExists);
                            out = new BufferedWriter(
                                    new OutputStreamWriter(
                                            outputStream, "UTF16"));
                            Set<Location> tempLocList;
                            int count = lDevLocation.size();
                            for(int i =0 ; i <count ; i++)
                            {
                                tempLocList = getDeviceData(lDevLocation.get(i).bssid).getUsedLocations();
                                out.write(measureSavePrefixDevice);
                                out.newLine();
                                out.write(lDevLocation.get(i).bssid);
                                out.newLine();
                                out.write(measureSavePrefixLocation);
                                out.newLine();
                                out.write(String.valueOf(lDevLocation.get(i).location.latitude));
                                out.newLine();
                                out.write(String.valueOf(lDevLocation.get(i).location.longitude));
                                out.newLine();
                                out.write(measureSavePrefixAccuracy);
                                out.newLine();
                                out.write(String.valueOf(lDevLocation.get(i).accuracy));
                                out.newLine();
                                out.write(measureSavePrefixTime);
                                out.newLine();
                                out.write(lDevLocation.get(i).time.toString());
                                out.newLine();
                                out.write(measureSavePrefixLocationPoints);
                                out.newLine();
                                out.write(String.valueOf(lDevLocation.get(i).locCount));
                                out.newLine();
                                for( Location _loc : tempLocList)
                                {
                                    out.write(String.valueOf(_loc.getLatitude()));
                                    out.newLine();
                                    out.write(String.valueOf(_loc.getLongitude()));
                                    out.newLine();
                                    out.write(String.valueOf(_loc.getAccuracy()));
                                    out.newLine();
                                }
                                out.write(measureSavePrefixSamplesCount);
                                out.newLine();
                                out.write(String.valueOf(lDevLocation.get(i).rssCount));
                                out.newLine();
                                out.write(measureError);
                                out.newLine();
                                float _dist = 0;
                                Location _loc;
                                if(((_loc = lDevLocation.get(i).getLocation()) != null) && realApLocation != null)
                                {
                                    _dist = _loc.distanceTo(realApLocation);
                                }
                                out.write(String.valueOf(_dist));
                                out.newLine();

                            }

                            out.write(measureSavePrefixEnd);
                            out.flush();
                            out.close();
                            Log.v("SAVE_DATA","Data saved succesfully to "+filePath+filename);
                        }
			 }
			else
			{
				if(lDevLocation.size() > 0){

				outputStream = new FileOutputStream(destFile,fileExists);
			     out = new BufferedWriter(
			    	       new OutputStreamWriter(
			    	                  outputStream, "UTF16"));
			    Set<Location> tempLocList;
				int count = lDevLocation.size();
				for(int i =0 ; i <count ; i++)
				{
				    tempLocList = getDeviceData(lDevLocation.get(i).bssid).getUsedLocations();
					out.write(measureSavePrefixDevice);
					out.newLine();					
					out.write(lDevLocation.get(i).bssid);
					out.newLine();
					out.write(measureSavePrefixLocation);
					out.newLine();
					out.write(String.valueOf(lDevLocation.get(i).location.latitude));
					out.newLine();
					out.write(String.valueOf(lDevLocation.get(i).location.longitude));
					out.newLine();
					out.write(measureSavePrefixAccuracy);
					out.newLine();
                    out.write(String.valueOf(lDevLocation.get(i).accuracy));
                    out.newLine();
					out.write(measureSavePrefixTime);
					out.newLine();
					out.write(lDevLocation.get(i).time.toString());		
					out.newLine();
					out.write(measureSavePrefixLocationPoints);
					out.newLine();
					out.write(String.valueOf(lDevLocation.get(i).locCount));
                    out.newLine();
					for( Location _loc : tempLocList)
                    {
                        out.write(String.valueOf(_loc.getLatitude()));
                        out.newLine();
                        out.write(String.valueOf(_loc.getLongitude()));
                        out.newLine();
                        out.write(String.valueOf(_loc.getAccuracy()));
                        out.newLine();
                    }
                    out.write(measureSavePrefixSamplesCount);
                    out.newLine();
                    out.write(String.valueOf(lDevLocation.get(i).rssCount));
                    out.newLine();
                    out.write(measureError);
                    out.newLine();
                    float _dist = 0;
                    Location _loc;
                    if(((_loc = lDevLocation.get(i).getLocation()) != null) && realApLocation != null)
                    {
                        _dist = _loc.distanceTo(realApLocation);
                    }
                    out.write(String.valueOf(_dist));
                    out.newLine();

				}

				out.write(measureSavePrefixEnd);
                out.flush();
                out.close();
                Log.v("SAVE_DATA","Data saved succesfully to "+filePath+filename);
				}
				else
				{
					//Toast.makeText(context,"Brak danych do zapisania",Toast.LENGTH_LONG).show();
				}
			}
		 
		}
		
		 catch (Exception e) {
		  e.printStackTrace(); //TODO implements postExecute information about save
		}

		
		
	
	}

	
	public void loadMeasurementsDataFromFile(String filename)
	{

		try {
			
		
			if(Boolean.parseBoolean((PreferenceManager.getDefaultSharedPreferences
			  (context).getString(context.getString(R.string.prefWifiMinLoc), "false"))))
			  {
				BufferedReader input = new BufferedReader(new FileReader(filePath+filename + measureSaveAddon));
				String readDataName = "null";
				String tempStream;
				Location readDataLoc = new Location("gps");
				Map<Location,int[]> tempMeasurements = new HashMap<Location,int[]>();
				List<NavigationData> tempNavDataList = new ArrayList<NavigationData>();
				NavigationData tempNavData = new NavigationData(100,readDataName);
				while((tempStream = input.readLine()) != null)
				{
					
					
					if(tempStream == measureSavePrefix) // TODO replace == with comparewithignorecase :)
					{
						readDataName = input.readLine();
						tempNavData = new NavigationData(100,readDataName);
					}
					if(tempStream == measureSavePrefixLocation)
					{
						String lat = input.readLine();
						String longi = input.readLine();
						double latitude = Double.parseDouble(lat);
						double longitude = Double.parseDouble(longi);
						readDataLoc.setLatitude(latitude);
						readDataLoc.setLongitude(longitude);
					}
					if(tempStream == measureSavePrefixAccuracy)
					{
						readDataLoc.setAccuracy(Float.parseFloat(input.readLine()));
					}
					if(tempStream == measureSavePrefixData)
					{
						ArrayList<Integer> tempDataRss = new ArrayList<Integer>();
						
						while((tempStream = input.readLine()) != measureSavePrefixEnd)
						{
							tempDataRss.add(Integer.parseInt(tempStream));
						}
						int[] tempIntArray = new int[tempDataRss.size()];

						for(int i = 0; i<tempDataRss.size();i++)
						{
							tempIntArray[i]=tempDataRss.get(i);
						}
						tempNavData.addItem(readDataLoc, tempIntArray);						
					}				
					if(tempStream == measureSavePrefixEnd)
					{
						if(!tempNavData.hasDataObserver){
							
							Iterator it = dataObservers.iterator();
						    while(it.hasNext())  {
						    	tempNavData.addListener(((DataObserver) it.next()));
						    }
						}
						tempNavDataList.add(tempNavData);
					}
					
					
				}
				
				for(int i = 0; i < tempNavDataList.size();i++)
				{
					addData(tempNavDataList.get(i));
				}
				
				
				
				
			  }
		}
		 catch (Exception e) {
			  e.printStackTrace();
		}
	}
	
	public void loadDataFromFile(String filename) //TODO kropka za file i co to za typ :)
	{
		FileInputStream inputStream;

		try {

            File f = new File(filePath+filename);
            if(!f.exists()){
                return;}

				BufferedReader input = new BufferedReader(new FileReader(filePath+filename));
				String readDataName = "null";
				String tempStream;
				Time time = new Time();
				LatLng readDataLoc = new LatLng(52,21);
				List<DeviceLocation> tempNavDataList = new ArrayList<DeviceLocation>();

				while((tempStream = input.readLine()) != null)
				{
					if(tempStream == measureSavePrefixDevice)
					{
						readDataName = input.readLine();
					}
					if(tempStream == measureSavePrefixLocation)
					{
						String lat = input.readLine();
						String longi = input.readLine();
						double latitude = Double.parseDouble(lat);
						double longitude = Double.parseDouble(longi);
						readDataLoc = new LatLng(latitude,longitude);
					}
					if(tempStream == measureSavePrefixTime)
					{
						String tempTime = input.readLine();						
						time.parse(tempTime);
					}
					DeviceLocation tempdevloc = new DeviceLocation(readDataName,readDataLoc,0,time,0,0); // TODO update accuracy and rss count and loc count etc.
					tempNavDataList.add(tempdevloc);
					
					if(lDevLocation == null)
					{
						lDevLocation = new ArrayList<DeviceLocation>();
					}
					lDevLocation.addAll(tempNavDataList);
					
					Iterator it = dataObservers.iterator();
				    while(it.hasNext())  {
				    	Iterator devIt = tempNavDataList.iterator();
				    	while(devIt.hasNext()){
				    		((DataObserver) it.next()).onResultsAvailable((DeviceLocation) devIt.next());
				    	}
				    }
				}
				
				
			  
		}

		 catch (Exception e) {
			  e.printStackTrace();
		}
	}
	
	
	
	
	private class gpsStatusListener implements android.location.GpsStatus.Listener
	{
		// when we get location then we can start scanning :-)
		@Override
		public void onGpsStatusChanged(int event) {
			switch (event)
			{
			case GpsStatus.GPS_EVENT_FIRST_FIX:
				Toast.makeText(context, "Ustalono lokalizacje przy użyciu GPS", Toast.LENGTH_LONG).show();				
				onProviderChanged(LocationManager.GPS_PROVIDER);
				try{

				}
				catch(Exception ex)
				{
					Log.e("cannot remove listener",ex.getMessage());
				}
				return;
			case GpsStatus.GPS_EVENT_STOPPED:
				Toast.makeText(context, "Zakończono lokalizacje przy użyciu GPS", Toast.LENGTH_LONG).show();
				onProviderChanged("Fuse");
				try{
					/*if(locationProvider == LocationManager.NETWORK_PROVIDER)
					{
						lmManager.requestLocationUpdates(LocationManager.,locationUpdateTime,locationUpdateDistance,locationListener);
						Location loc = lmManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
						locationChanged(loc);
					}*/
				}
				catch(Exception ex)
				{
					Log.e("cannot remove listener",ex.getMessage());
				}
				return;
			}
			
			
		}
	}


	public class PositionListener implements LocationListener
	{
		@Override
		public void onLocationChanged(Location location)
		{

		}

		@Override
		public void onStatusChanged(String provider, int status, Bundle extras) {
			String message = "";
			switch(status)
			{
			case LocationProvider.OUT_OF_SERVICE:
				message = "Dostawca lokalizacji " +provider+" jest niedostępny";
				Toast.makeText(context, message, Toast.LENGTH_SHORT ).show();
				break;
			case LocationProvider.TEMPORARILY_UNAVAILABLE:
				message = "Dostawca lokalizacji " +provider+" jest tymczasowo niedostępny";
				Toast.makeText(context, message, Toast.LENGTH_SHORT ).show();
				break;
			case LocationProvider.AVAILABLE:
				message = "Dostawca lokalizacji " +provider+" jest juz dostępny";
				Toast.makeText(context, message, Toast.LENGTH_SHORT ).show();
				break;
				
			}
			
			
		}

		@Override
		public void onProviderEnabled(String provider) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onProviderDisabled(String provider) {
			// TODO Auto-generated method stub
			
		}
	}
	
	public class WifiReceiver extends BroadcastReceiver
	{

		@Override
		public void onReceive(Context context, Intent intent) {
			try {
				List<ScanResult> tempListScan = wmManager.getScanResults();
                addData(tempListScan,lActualLocation);
				Log.v("new data","received get new data");
				startScan(); // TODO CHECK

			}
			catch(Exception ex)			{
				//Toast.makeText(context, ex.toString(), Toast.LENGTH_LONG).show();
               return;
			}
			
		}
		
	}

	public class TimerScanTask extends TimerTask
    {
            @Override
            public void run() {
                startScan();
           }
        }
    }
	
}


