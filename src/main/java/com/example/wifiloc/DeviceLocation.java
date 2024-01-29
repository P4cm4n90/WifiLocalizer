package com.example.wifiloc;

import android.location.Location;
import android.net.wifi.ScanResult;
import android.text.format.Time;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class DeviceLocation {
	private static final int maxLocationStored = 20;
	public String bssid;
	public LatLng location;
	public Time time;
	public TreeMap<String,LatLng> map;
	public double accuracy;
	private boolean isFloating = false;
	public int rssCount = 0;
	public int locCount = 0;




	DeviceLocation(String bssid, LatLng location,double accuracy, Time time,int _rssCount,int _locCount) {
		this.bssid= (bssid);
		this.location =(location);
		this.time = time;
		this.accuracy = accuracy;
		map = new TreeMap<String,LatLng>();
        map.put(time.toString(),location);
        this.rssCount = _rssCount;
        this.locCount = _locCount;
	}
		
	public boolean addData(LatLng location,Time time)
	{
		
		if(map.size() > 0)
		{
			List<LatLng> tempList = new ArrayList<LatLng>(map.values());
			Location refLoc = new Location("none");
			refLoc.setLatitude(tempList.get(0).latitude);
			refLoc.setLongitude(tempList.get(0).longitude);
			Location currentLoc = new Location("none");
			currentLoc.setLatitude(location.latitude);
			currentLoc.setLongitude(location.longitude);
			
			if(refLoc.distanceTo(currentLoc) > accuracy)
			{

                isFloating = true;
				return true;
			}
		}
		map.put(time.toString(), location);
		return false;
	}

    public boolean addData(DeviceLocation tempLoc)
    {
        this.bssid= tempLoc.bssid;
        this.location = tempLoc.location;
        this.time = tempLoc.time;

        if(map.size() > 0)
        {
            List<LatLng> tempList = new ArrayList<LatLng>(map.values());
            Location refLoc = new Location("none");
            refLoc.setLatitude(tempList.get(0).latitude);
            refLoc.setLongitude(tempList.get(0).longitude);
            Location currentLoc = new Location("none");
            currentLoc.setLatitude(tempLoc.location.latitude);
            currentLoc.setLongitude(tempLoc.location.longitude);
            float accuracy = (float) tempLoc.accuracy;
            if(refLoc.distanceTo(currentLoc) > accuracy )
            {

                isFloating = true;
                return true;
            }
        }
        map.put(time.toString(), location);
        return false;
    }


	public Location getLocation()
    {
        Location _loc = new Location("rssi");
        if(location == null){ return null;}
        _loc.setLatitude(location.latitude);
        _loc.setLongitude(location.longitude);
        return _loc;
    }
	public boolean isMobile()
	{
		return isFloating;
	}
	
	
	public boolean compareTo(ScanResult result)
	{
		if(bssid == result.BSSID)
		{
			return true;
		}
		else{
			return false;
		}
			// TODO Auto-generated constructor stub
	}

}
