package com.example.wifiloc;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;

import javax.measure.quantity.Angle;
import javax.measure.unit.NonSI;
import javax.measure.unit.Unit;

public class ExtendedLocation {

	private static Unit<Angle> unit = NonSI.DEGREE_ANGLE;
	private Location location;
	private double rssi;
	private double stdDev;
	private int longZone;
	private char latZone;
	private boolean indoorLocation = false;

	public ExtendedLocation( Location loc,double rssi, double stdDev)
	{
		
		if((this.location = loc) == null){
			throw new NullPointerException("Given location is unknown");}
		this.rssi = rssi;
		this.stdDev = stdDev;
		LatLong tempLoc = LatLong.valueOf(location.getLatitude(), location.getLongitude(), unit);
		longZone = UTM.getLongitudeZone(tempLoc);
		latZone = UTM.getLatitudeZone(tempLoc);
		
	}

    public ExtendedLocation( Location loc, double stdDev)
    {

        if((this.location = loc) == null){
            throw new NullPointerException("Given location is unknown");}
        this.stdDev = stdDev;
        LatLong tempLoc = LatLong.valueOf(location.getLatitude(), location.getLongitude(), unit);
        longZone = UTM.getLongitudeZone(tempLoc);
        latZone = UTM.getLatitudeZone(tempLoc);

    }
	
	public int getLongZone()
	{
		return longZone;
	}
	
	public char getLatZone()
	{
		return latZone;
	}
	public ExtendedLocation(UTM utmLoc)
	{
		location = new Location("Custom");
		setLocationUtm(utmLoc);
	}

	public void setIndoorLocation(boolean value)
    {
        indoorLocation = value;
    }

	public boolean isLocatedIndoor()
    {
        return indoorLocation;
    }

	public double getStdDev()
	{
		return this.stdDev;
	}
	
	public float getAccuracy()
	{
		return location.getAccuracy();
	}
	public double getRssi()
	{
		return this.rssi;
	}
	
	public void setRssi(double rss)
	{
		this.rssi = rss;
	}
	public void setLocation(LatLng loc)
	{
		location.setLatitude(loc.latitude);
		location.setLongitude(loc.longitude);
	}
	
	public void setLocationUtm(UTM loc)
	{
		LatLong tempLoc = UTM.utmToLatLong(loc,  ReferenceEllipsoid.WGS84);
		location.setLatitude(tempLoc.latitudeValue(unit));
		location.setLongitude(tempLoc.longitudeValue(unit));
	}
	public UTM getUtmLocation()
	{
		return UTM.latLongToUtm(LatLong.valueOf(location.getLatitude(),location.getLongitude(), unit),  ReferenceEllipsoid.WGS84);
	}

	public LatLng getWgsLocation()
	{
		return new LatLng(location.getLatitude(),location.getLongitude());
	}
}
