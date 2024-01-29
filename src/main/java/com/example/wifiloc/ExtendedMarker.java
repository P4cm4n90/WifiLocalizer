package com.example.wifiloc;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;

/**
 * Created by pac on 14.05.14.
 */
public class ExtendedMarker {


    public final static BitmapDescriptor iconHideStatic = BitmapDescriptorFactory.fromResource(R.drawable.reddot);
    public final static BitmapDescriptor iconHideRunning = BitmapDescriptorFactory.fromResource(R.drawable.greendot);
    public final static BitmapDescriptor iconRunning = BitmapDescriptorFactory.fromResource(R.drawable.wifi_floating);
    public final static BitmapDescriptor iconStatic = BitmapDescriptorFactory.fromResource(R.drawable.wifi_stationary);
    public final static String stateStatic = "Terminal stacjonarny";
    public final static String stateFloating = "Terminal ruchomy";
    public final static String textSignalStrength = "Siła sygnału: ";
    public final static String textBssid = "Adres: ";

    public Marker marker;
    public Circle markerCircle;

    private String actualState = stateStatic;
    private String SSID = "";
    private String BSSID = "";
    private boolean isFloating = false;
    private boolean isMinimalized = true;
    private int rss = 0;


    public ExtendedMarker(Marker marker)
    {
        if(marker == null){ throw new NullPointerException("marker is null");}
        this.marker = marker;
        this.SSID = marker.getTitle();
        marker.setAnchor(0.5f,0.5f);
        marker.setIcon(iconHideStatic);
        setSnippet();

    }

    public void addCircle(Circle circle)
    {
        markerCircle = circle;
        markerCircle.setVisible(false);
    }

    public void remove()
    {
        marker.remove();
        markerCircle.remove();
    }

    public void setState(String tempState)
    {
        if(tempState.equalsIgnoreCase(stateFloating))
        {
            actualState = stateFloating;

        }
        if(tempState.equalsIgnoreCase(stateStatic))
        {
            actualState = stateStatic;

        }
    }

    public void setBSSID(String bssid)
    {
        if(this.BSSID == bssid)
        {
            return;
        }
        this.BSSID = bssid;

    }

    public void setAccuracy(double accuracy)
    {
        markerCircle.setRadius(accuracy);

    }

    public String getBSSID()
    {
        return BSSID;
    }

    public void minimalize()
    {
        if(!isMinimalized){

            marker.setAnchor(0.5f,0.5f);
            if(!isFloating){
                marker.setIcon(iconHideStatic);
                markerCircle.setVisible(false);

            }
            else
            {
                marker.setIcon(iconHideRunning);
                markerCircle.setVisible(false);
            }
            this.isMinimalized = true;

        }
    }

    public void maximalize()
    {
        if(isMinimalized){

            marker.setAnchor(0.5f,1f);
            if(!isFloating){
               marker.setIcon(iconStatic);
                markerCircle.setVisible(true);
            }
            else
            {
                marker.setIcon(iconRunning);
                markerCircle.setVisible(true);
            }
        this.isMinimalized = false;
        }
    }

    public void setRss(int rss)
    {
        this.rss=rss;
        setSnippet();
    }
    public int getRss()
    {
        return rss;
    }

    public void setFloating(boolean state)
    {
        if(state == this.isFloating)
        {
            return;
        }
        this.isFloating = state;
        if(isMinimalized){
            if(!isFloating){
                setState(stateStatic);
                marker.setIcon(iconHideStatic);
            }
            else{
                setState(stateFloating);
                marker.setIcon(iconHideRunning);
            }
        }
        else
        {
            if(!isFloating){
                setState(stateStatic);
                marker.setIcon(iconStatic);
            }
            else{
                setState(stateFloating);
                marker.setIcon(iconRunning);
            }
        }

        setSnippet();

    }

    public boolean isFloating()
    {
        return this.isFloating;
    }

    public void setTitle(String title)
    {
        if(this.SSID == title)
        {
            return;
        }
        this.SSID =title;
        marker.setTitle(title);


    }

    public void setIcon(BitmapDescriptor icon)
    {
        marker.setIcon(icon);
    }

    public void setPosition(LatLng latLng)
    {
        marker.setPosition(latLng);
        markerCircle.setCenter(latLng);
    }

    public void showInfoWindow()
    {
        marker.showInfoWindow();
    }

    public void setSnippet() // separator |
    {
        marker.setSnippet(actualState+"|"
                +textBssid+this.BSSID+"|"
                +textSignalStrength+this.rss+"dBm|");

        update();
    }

    public void update()
    {
        if(isInfoWindowShown())
        {
            hideInfoWindow();
            showInfoWindow();
        }
    }

    public boolean isInfoWindowShown()
    {
        return marker.isInfoWindowShown();
    }

    public void hideInfoWindow()
    {
        marker.hideInfoWindow();
    }

    public LatLng getPosition()
    {
        return marker.getPosition();
    }

    public String getSnippet()
    {
        return marker.getSnippet();
    }

    public String getTitle()
    {
        return marker.getTitle();
    }

    public String getId()
    {
        return marker.getId();
    }
}
