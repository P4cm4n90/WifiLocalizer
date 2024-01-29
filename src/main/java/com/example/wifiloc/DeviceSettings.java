package com.example.wifiloc;
import 	android.net.wifi.ScanResult;
import 	com.google.android.gms.maps.model.LatLng;
/**
 * Created by pac on 13.05.14.
 */
public class DeviceSettings implements Comparable<DeviceSettings>{

    public String BSSID = "";
    public String SSID = "";
    public int level = 0;
    public boolean isFloating = false;
    public boolean wantToRefresh = false;
    public boolean readyToLoc = false;
    public LatLng location = null;

    public DeviceSettings(ScanResult scanRes)
    {
        this.BSSID = scanRes.BSSID;
        this.SSID = scanRes.SSID;
        this.level = scanRes.level;
    }

    public void enableWantToRefresh()
    {
        this.wantToRefresh = true;
    }

    public void disableWantToRefresh()
    {
        this.wantToRefresh = false;
    }

    public void enableReadyToLoc()
    {
        this.readyToLoc = true;
    }

    public void disableReadyToLoc()
    {
        this.readyToLoc = false;
    }

    public void setRss(int rss)
    {
        this.level = rss;
    }

    public void setLocation(double latitude,double longitude)
    {
        this.location = new LatLng(latitude,longitude);
    }

    @Override
    public int compareTo(DeviceSettings devSet)
    {
        if(this.level > devSet.level)
        {
            return 1;
        }
        if(this.level == devSet.level)
        {
            return 0;
        }
        if(this.level < devSet.level)
        {
            return -1;
        }
        return 0;
    }
}
