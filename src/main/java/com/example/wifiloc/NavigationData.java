package com.example.wifiloc;

import android.location.Location;
import android.text.format.Time;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


public class NavigationData{
	public final String tag;
	private DeviceLocationManager deviceLocation;
	private HashMap<Location,int[]> nData;
	private HashMap<Location,Integer> rssCount;
	private HashMap<Location,Boolean> collectedDataInfo;
	private ArrayList<Location> locationList;
	private ArrayList<DataObserver> dataObservers = new ArrayList<DataObserver>();
	//private ArrayList<int[]> rssList;
	
	///Map<Location,int[]> dataList; 
	private int samplesQuantity;
	private int locationQuantity = 100;
	private int locationCount = 0;
	private int locMin = 0;
	private int rssMin = 0;
	public boolean hasDataObserver = false;
	private boolean indoorLocation = false;
	public boolean isDataCollected = false;
	

	public NavigationData(int samplesQuantity,final String tag)
	{
		this.tag = tag;
			this.samplesQuantity = samplesQuantity;
			nData = new HashMap<Location,int[]>(locationQuantity);
			rssCount = new HashMap<Location,Integer>();
			collectedDataInfo = new HashMap<Location,Boolean>();
			deviceLocation = new DeviceLocationManager(){
				@Override
				public void onResultsGet(LatLng loc,double accuracy,Time time)
				{				
					DeviceLocation deviceLocation = new DeviceLocation(tag,loc,accuracy,time,rssMin,locMin);
					Iterator<DataObserver> i = dataObservers.iterator();
				    while(i.hasNext())  {
				      ((DataObserver) i.next()).onResultsAvailable(deviceLocation);
				    }
					//onLocalizationResultsAvailable(deviceLocation);
				}
			};
			
		
	}



	public synchronized void addItem(Location location, int rss)
	{
                try{
                    if(nData.containsKey(location))
                    {
                        int rssCounter = rssCount.get(location) + 1;
                        rssCount.put(location, rssCounter);
                        int[] temparray = nData.get(location);
                        int lastrss = getLastRssNumber(temparray);
                        Log.v(location.toString(),String.valueOf(rss));
                        temparray[lastrss+1]=rss;
                        nData.put(location, temparray);

                    }
                    else
                    {
                        rssCount.put(location, 1);
                        int[] temparray = new int[samplesQuantity];
                        temparray[0] = rss;
                        Log.v(location.toString(),String.valueOf(rss));
                        nData.put(location,temparray);
                    }


                    if(collectedDataInfo != null){

                        if(rssCount.get(location) >= rssMin)
                        {

                            Iterator<DataObserver> i = dataObservers.iterator();

                                 while(i.hasNext())  {

                                    ((DataObserver) i.next()).onDataCollected(tag);
                                     locationCount++;

                                }
                             //TODO for test change later
                            if(collectedDataInfo.get(location) == null){
                                collectedDataInfo.put(location, true);
                            }

                             //TODO check if its worth to use ''equality'' :)
                        if(collectedDataInfo.keySet().size() == locMin)
                        {
                            isDataCollected = true;
                            Iterator<DataObserver> ik = dataObservers.iterator();
                            while(ik.hasNext())  {
                                ((DataObserver) ik.next()).onReadyToLoc(tag);

                            }
                        }
                     /*   if(isDataCollected(rssMin,locMin))
                        {
                            Iterator<DataObserver> i = dataObservers.iterator();
                            while(i.hasNext())  {
                                ((DataObserver) i.next()).onReadyToLoc(tag);
                        }
                        }*/
                    }
                    }
                }
                catch(IndexOutOfBoundsException iErr)
                {
                    return;
                }

       }




	
	public synchronized boolean addItem(Location location, int[] rssArray)
	{

        try{
                    if(nData.containsKey(location))
                    {
                        int rssCounter = rssCount.get(location) + rssArray.length;
                        rssCount.put(location, rssCounter);

                        int[] temparray = nData.get(location);
                        int lastrss = getLastRssNumber(temparray);
                        for(int i = 0;i<rssArray.length;i++)
                        {
                            temparray[i+lastrss]=rssArray[i];
                        }
                        nData.put(location, temparray);

                    }
                    else
                    {
                        rssCount.put(location, rssArray.length);
                        int[] temparray = new int[samplesQuantity];
                        for(int i = 0;i<rssArray.length;i++)
                        {
                            temparray[i]=rssArray[i];
                        }
                        nData.put(location,temparray);
                    }
                    /*if(rssCount.size() >= locMin)
                    {
                        if(isDataCollected(locMin,rssMin))
                        {
                            Iterator<DataObserver> i = dataObservers.iterator();
                            while(i.hasNext())  {
                              ((DataObserver) i.next()).onDataCollected(tag);
                            }
                        }
                    }*/ // temporary unavaliable
                     }
                catch(IndexOutOfBoundsException iErr)
                {
                    return false;
                }
           return true;
            }


    public void setIndoorLocation(boolean value)
    {
        indoorLocation = value;
    }

    public boolean getIndoorLocation()
    {
        return indoorLocation;
    }
	
	public boolean isDataCollected()
	{

		return isDataCollected;
		
	}
	
	public void onLocalizationResultsAvailable(DeviceLocation deviceLocation){};
		
	public void onDataCollected(){};

	public Set<Location> getUsedLocations()
    {
        return nData.keySet();
    }

	public int getLocationCount()
	{
		return locationCount;
	}

	// automatically sorting and ...
	public ArrayList<ExtendedLocation> getItems() throws Exception
	{
		if(nData == null || nData.size() < 3){
			throw new Exception("Navigation data is not gathered yet");
		}
		int size = nData.size();
		ArrayList<ExtendedLocation> finalList = new ArrayList<ExtendedLocation>(size);
		List<Location> tempList = new ArrayList<Location>(size);
		tempList.addAll(nData.keySet());
		double maxRss = -100000000;
		int index = 0;
		for(int i = 0 ;i < size;i++)
		{
			Location temploc = tempList.get(i);
			double average,stDev;

                average = calcMeanValue(nData.get(temploc)); // TODO cos nie tak sprawdzic
                stDev = calcStdDev(nData.get(temploc),average);
                if(average > maxRss){
                    maxRss = average;
                    index = i;}
                if(indoorLocation)
                {
                    ExtendedLocation ext;
                    int maxValue = calcMaxValue(nData.get(temploc));
                    ext = new ExtendedLocation(temploc,(double) maxValue,stDev);
                    ext.setIndoorLocation(true);
                    finalList.add(ext);

                }
                else
                {
                    finalList.add( new ExtendedLocation(temploc,average,stDev)); //TODO testowac
                }



		}
		if(index == 0){
			return finalList;}
		
		ExtendedLocation maxRssiLoc = finalList.get(index);
		ExtendedLocation zeroLoc = finalList.get(0);
		finalList.set(0, maxRssiLoc);
		finalList.set(index, zeroLoc);
		
		return finalList;
	}

	
	public HashMap<Location, int[]> getDataToSave()
	{
		return nData;
	}
	public int[] getRssAtLocation(Location location)
	{
		return nData.get(location);
	}
	
	public void findDevice(float nFactor, double FAF)
	{
		try{
			//deviceLocation = new DeviceLocationManager();
			deviceLocation.putData(getItems());
			deviceLocation.computeLocation(nFactor, FAF);
		}
		catch(Exception ex)
		{
			Log.e("Computation of localization","failed");
		}
	}
	
	public int getLastRssNumber(int[] rsstable)
	{
		int sth = 100000;
		try{
		for(int i =0 ;i<samplesQuantity;i++)
		{
			if((sth = rsstable[i]) == 0)
			{
				if(i>0)
				{
					return (i-1);
				}
			}
		}
		return 0;
		}
		catch(Exception ex)
		{
			return 0;
		}
		
	}

	/// experiment
	private double calcMeanValue(int[] array)
	{
		double mean=0;
		double lastitem=0;
        try{
		for(int i=0;i< array.length; i++)
		{
            if(array[i] == 0)
            {
                lastitem = i;
                break;
            }
			lastitem = mean;
			mean = mean + array[i];

			lastitem = i;
		}
			
		
		mean = mean/lastitem;
		return mean;
        }
        catch(Exception ex)
        {
            ex.printStackTrace();
            return -75;
        }
	}

	private int calcMaxValue(int[] array)
    {
        int lastitem=-1000;
            for(int i=0;i< array.length; i++)
            {
                if(array[i] == 0)
                {
                    break;
                }
                if(lastitem < array[i])
                {
                    lastitem = array[i];
                }

            }
            return lastitem;
    }
	
	private double calcStdDev(int[] array,double mean)
	{
		double stdDev=0;
		double lastitem=0;
        try
        {
            for(int i=0;i< array.length; i++)
            {
                lastitem = stdDev;
                stdDev = stdDev + Math.pow((array[i] - mean), 2);
                if(array[i] == 0){
                    if((lastitem = i) == 0){
                        return 0;}
                    else{
                        break;
                    }
                }

            }
            stdDev = Math.sqrt((stdDev / lastitem));
            return stdDev;
        }
        catch(Exception ex)
        {

            ex.printStackTrace();
            return 1;
        }
	}

	public synchronized void addListener(DataObserver observer)
	{
		dataObservers.add(observer);
		hasDataObserver = true;
		rssMin = observer.getSettings()[0];
		locMin = observer.getSettings()[1];
	}
	
	public synchronized void removeListener(DataObserver observer)
	{
		hasDataObserver = false;
		dataObservers.remove(observer);
	}
	
}





