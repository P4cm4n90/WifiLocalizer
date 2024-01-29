package com.example.wifiloc;


import android.app.Service;
import android.graphics.Color;
import android.graphics.Matrix;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMyLocationChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;


public class FragmentMap extends SupportMapFragment {



	private SupportMapFragment fragment;
	private GoogleMap map;
	private HashMap <String,ExtendedMarker> markerMap = new HashMap<String,ExtendedMarker>();
	private HashMap <String,Circle> circleMap =new HashMap<String,Circle>();
	private HashMap <Integer,Marker> measurementsPoints = new HashMap<Integer, Marker>();
	private Marker realAP;
	private Marker customLocationMarker;
	private LatLng defaultPosition;
	private SensorManager sensorManager;
	private sListener sensorListenerAccel;
	private sListener sensorListenerMagn;
	private MyLocationListener myLocationListener;
	private TextView tvBearing;
	private TextView tvDistToAp;

    private boolean isAnyMarkerMaximalized = false;
    private boolean customLocalisation = false;

    private Location apLocation;
	private ImageView ivDirectionArrow;

	//private boolean
	private boolean navigationMode = false;
	private String navigationTag = "";

	private float zoom;

	private long bearingInterval = 0;
	private long prevTime = 0;

	private float[] mGravity = null;
	private float[] mGeomagnetic = null;


	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		
		return inflater.inflate(R.layout.fragment_map, container, false);
		
	}
	
	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		
		super.onActivityCreated(savedInstanceState);
		
		FragmentManager fm = getChildFragmentManager();
		fragment = (SupportMapFragment) fm.findFragmentById(R.id.map);
		if (fragment == null) {
			fragment = SupportMapFragment.newInstance();
			fm.beginTransaction().replace(R.id.map, fragment).commit();
		}
		
	}
	@Override
	public void onStart()
	{
		super.onStart();

		prevTime = System.nanoTime();
		tvBearing = (TextView) getView().findViewById(R.id.tvBearing);
		tvDistToAp = (TextView) getView().findViewById(R.id.tvDistToAp);
		LocationManager lm = (LocationManager) getActivity().getSystemService(Service.LOCATION_SERVICE);
		Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);
		ivDirectionArrow = (ImageView) getView().findViewById(R.id.ivDirectionArrow);

		if(location == null)
		{
			location = lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER);
			if(location == null)
			{
				location = new Location("gps");
				location.setLatitude(541400);
				location.setLatitude(210000);
			}
			
		}
		defaultPosition = new LatLng(location.getLatitude(),location.getLongitude());
        map = fragment.getMap();
        sensorManager = (SensorManager) getActivity().getSystemService(Service.SENSOR_SERVICE);

        if(PreferenceManager.getDefaultSharedPreferences
                (getActivity()).getString(getString(R.string.locProvider), LocationManager.GPS_PROVIDER)
                .compareToIgnoreCase(getString(R.string.locProviderCustom)) != 0)
        {
            setLocationTrace();
        }
        else
        {
            setCustomLocationTrace();
        }

        map.getUiSettings().setZoomGesturesEnabled(true);

        map.getUiSettings().setCompassEnabled(true);
        map.getUiSettings().setTiltGesturesEnabled(true);
        map.getUiSettings().setRotateGesturesEnabled(true);
        map.setInfoWindowAdapter(new MarkerInfoAdapter());

	}

    public void setCustomLocation(Location _location)
    {
        _location.setAccuracy(0.1f);

        if (customLocationMarker != null)
        {
            customLocationMarker.remove();
        }

        customLocationMarker = map.addMarker(new MarkerOptions()
            .position(new LatLng(_location.getLatitude(),_location.getLongitude()))
            .icon(BitmapDescriptorFactory.fromResource(R.drawable.bluedot)));

        onLocationChanged(_location);

        if(realAP != null)
        {
            updateApLocation();
            float _dist = _location.distanceTo(apLocation);
            tvDistToAp.setText(String.valueOf(_dist));
        }
    }

	public void setCustomLocationTrace()
    {
        customLocalisation = true;
        map.setMyLocationEnabled(false);
        map.getUiSettings().setMyLocationButtonEnabled(false);
        Button bShowCustomLocation = (Button) getView().findViewById(R.id.bShowCustomLocation);
        bShowCustomLocation.setVisibility(View.VISIBLE);
        bShowCustomLocation.setEnabled(true);
        bShowCustomLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(customLocationMarker != null)
                {
                    LatLng _latLng = customLocationMarker.getPosition();
                    map.animateCamera(CameraUpdateFactory.newLatLng(_latLng));
                }
            }
        });
    }

    public boolean isCustomLocationTrace()
    {
        return customLocalisation;
    }

    private void setLocationTrace()
    {
        customLocalisation = false;
        map.setMyLocationEnabled(true);
        Button bShowCustomLocation = (Button) getView().findViewById(R.id.bShowCustomLocation);
        bShowCustomLocation.setVisibility(View.INVISIBLE);
        bShowCustomLocation.setEnabled(false);
        map.setOnMyLocationChangeListener(new OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                onLocationChanged(location);
                if(realAP != null)
                {
                    updateApLocation();
                    float _dist = map.getMyLocation().distanceTo(apLocation);
                    tvDistToAp.setText(String.valueOf(_dist));
                }

            }
        });
        map.getUiSettings().setMyLocationButtonEnabled(true);
    }

	public boolean isNagivationModeEnable()
	{
		return navigationMode;
	}
	
	public String getNavigationTag()
	{
		return navigationTag;
	}
	
	public void startNavigation(String bssid)
	{
		if(!ivDirectionArrow.isShown()){ ivDirectionArrow.setVisibility(View.VISIBLE);}
		navigationMode = true;
		tvBearing.setVisibility(View.VISIBLE);
		navigationTag = bssid;
		sensorListenerAccel = new sListener();
		map.setOnMyLocationChangeListener(myLocationListener = new MyLocationListener());
		map.getUiSettings().setScrollGesturesEnabled(false);
		map.getUiSettings().setRotateGesturesEnabled(false);
		Location loc = map.getMyLocation();
		CameraPosition old = map.getCameraPosition();
		CameraPosition pos = new CameraPosition(new LatLng(loc.getLatitude(),loc.getLongitude()),
				old.zoom,old.tilt,old.bearing);
		map.animateCamera( CameraUpdateFactory.newCameraPosition(pos),10,null);
		sensorManager.registerListener
				(sensorListenerAccel,
				(Sensor)sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0), 
				SensorManager.SENSOR_DELAY_NORMAL);
		sensorListenerMagn = new sListener();
		sensorManager.registerListener
		(sensorListenerMagn,
		(Sensor)sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).get(0), 
		SensorManager.SENSOR_DELAY_NORMAL);
	}

	public void stopNavigation()
	{

		ivDirectionArrow.setVisibility(View.INVISIBLE);
        tvBearing.setVisibility(View.INVISIBLE);
		map.getUiSettings().setScrollGesturesEnabled(true);
		map.getUiSettings().setRotateGesturesEnabled(true);
		try{
			if(navigationMode)
			{
				sensorManager.unregisterListener(sensorListenerAccel);
				sensorManager.unregisterListener(sensorListenerMagn);
                navigationMode = false;
                navigationTag = "";
			}
		}
		catch(Exception ex)
		{
			Log.e("Stopping navigation failed", ex.getMessage());
		}

	}
	
	private void navigateToMarker(String markerTag, float azimuth)
	{
		ExtendedMarker marker = markerMap.get(markerTag);
		if( marker == null) {
			throw new NullPointerException("Cannot start navigation. Marker not found");
		}
        Location myLocation;
		if(customLocalisation)
        {
            myLocation = new Location("custom");
            LatLng _latLng = customLocationMarker.getPosition();
            myLocation.setLatitude(_latLng.latitude);
            myLocation.setLongitude(_latLng.longitude);
        }
        else
        {
            myLocation = map.getMyLocation();
        }
		Location markerLocation = new Location("custom");
		
		markerLocation.setLatitude(marker.getPosition().latitude);
		markerLocation.setLongitude(marker.getPosition().longitude);
		
		float distance = myLocation.distanceTo(markerLocation);
		float initialBearing = myLocation.bearingTo(markerLocation);
		float endBearing = initialBearing - azimuth;
		tvBearing.setText(String.valueOf(endBearing));
		int rotBoundX = ivDirectionArrow.getDrawable().getBounds().width()/2;
		int rotBoundY = ivDirectionArrow.getDrawable().getBounds().height()/2;
		
		Matrix matrix = new Matrix();

		ivDirectionArrow.setScaleType(ScaleType.MATRIX);
		matrix.postRotate(endBearing, rotBoundX, rotBoundY);
		ivDirectionArrow.setImageMatrix(matrix);
		
		double radBearing = Math.toRadians(endBearing);
		int screenDist = 50;
		float deltaY = (float) (Math.cos(radBearing)*screenDist);
		float deltaX = (float) (Math.sin(radBearing)*screenDist);
		
		FrameLayout.LayoutParams lparam = (FrameLayout.LayoutParams) ivDirectionArrow.getLayoutParams();
		lparam.setMargins((int)deltaX , 0, 0, (int)deltaY);
		ivDirectionArrow.setLayoutParams(lparam);
		ivDirectionArrow.invalidate();
		ivDirectionArrow.bringToFront();
		tvBearing.invalidate();
		tvBearing.bringToFront();


		
		/*
		 Matrix matrix=new Matrix();
			imageView.setScaleType(ScaleType.MATRIX);   //required
			matrix.postRotate((float) angle, pivX, pivY);
			imageView.setImageMatrix(matrix);
			 matrix.postRotate( 180f, imageView.getDrawable().getBounds().width()/2, imageView.getDrawable().getBounds().height()/2);
		 */
		
	}

    public void markRealApLoc()
    {
    if(realAP != null) { realAP.remove();}
        realAP = map.addMarker(new MarkerOptions()
        .position(new LatLng(map.getMyLocation().getLatitude(), map.getMyLocation().getLongitude()))
        .draggable(true));


    }

    public void updateApLocation()
    {
        if(realAP != null)
        {
            apLocation = new Location("gps");
            LatLng _loc = realAP.getPosition();
            apLocation.setLatitude(_loc.latitude);
            apLocation.setLongitude(_loc.longitude);

        }
        return;
    }

    public LatLng getAPLocation()
    {
        if(realAP != null)
        {
            return realAP.getPosition();

        }
        return null;
    }

    public void drawMeasurementsPoints(Set<Location> measureLocPoints)
    {
        if(measurementsPoints.size() > 0)
        {
           Collection<Marker> tempMarkerSet = measurementsPoints.values();
           for( Marker _marker : tempMarkerSet)
           {
               _marker.remove();
           }
           measurementsPoints = new HashMap<Integer, Marker>();
        }

        int tick = 0;
        for(Location tempLoc : measureLocPoints)
        {
            Marker tempMarker = map.addMarker(new MarkerOptions()
                    .position(new LatLng(tempLoc.getLatitude(),tempLoc.getLongitude()))
                    .title(String.valueOf(tick))
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.bluedot)));
            measurementsPoints.put(tick,tempMarker);
            tick++;
        }
    }

    public void onLocationChanged(Location location){};
		
	public void addMarker(DeviceLocation devLocation){

		ExtendedMarker marker;
		String name = devLocation.bssid;
		LatLng position = devLocation.location;
		double accuracy = devLocation.accuracy;

		if((marker = markerMap.get(name)) != null) // todo floating ap nie zawsze dziala :)
		{
			if(!devLocation.isMobile()){
                marker.setPosition(position);
                marker.setAccuracy(accuracy);

            }
			else
			{
				marker.setPosition(position);
                marker.setFloating(true);
                marker.setAccuracy(accuracy);
			}
		}
		else
			if(!devLocation.isMobile()){
                marker = new ExtendedMarker(map.addMarker(new MarkerOptions()
                        .position(position)
                        .title(name)));
                        marker.addCircle(map.addCircle(new CircleOptions()
                                .center(position)
                                .radius(accuracy)
                                .fillColor(0x2F00FF00)
                                .strokeColor(Color.TRANSPARENT)
                                .strokeColor(Color.TRANSPARENT)));
			}
			else
			{
                marker = new ExtendedMarker(map.addMarker(new MarkerOptions()
                        .position(position)
                        .title(name)));
                marker.addCircle(map.addCircle(new CircleOptions()
                        .center(position)
                        .radius(accuracy)
                        .fillColor(0x2F00FF00)
                        .strokeColor(Color.TRANSPARENT)
                        .strokeColor(Color.TRANSPARENT)));
                marker.setFloating(true);
			}
		markerMap.put(name, marker);

	}
	
	public boolean checkMarkerExist(String name)
	{
		return markerMap.containsKey(name);
	}

    public void hideMarkers()
    {
        if(markerMap.size() > 0)
        {
            ArrayList<String> macList = new ArrayList<String>(markerMap.keySet());
            for(String s : macList)
            {
                markerMap.get(s).minimalize();
            }
        }
    }

	public boolean showMarker(String name)
	{
		ExtendedMarker marker = markerMap.get(name);
		if(marker == null)
		{
			return false;
		}
		else
		{
			marker.maximalize();
            marker.showInfoWindow();
            this.isAnyMarkerMaximalized = true;
			zoom = map.getCameraPosition().zoom;
			map.moveCamera( CameraUpdateFactory.newLatLngZoom(marker.getPosition() , 19f));
			return true;
		}
	}

    public boolean isAnyMarkerMaximalized()
    {
        return isAnyMarkerMaximalized;
    }

    public void minimalizeMarkers()
    {
        if(isAnyMarkerMaximalized)
        {
            for(String s : markerMap.keySet())
            {
                markerMap.get(s).minimalize();
            }
        }
    }
	//to implement
    public void updateMarkerData(DeviceSettings tempResult) // TODO ponder about last errors
    {
        if(!markerMap.containsKey(tempResult.BSSID)) // todo UPDATE BAD PERFORMANCE!
        { return;
        }
        else
        {
            ExtendedMarker marker = markerMap.get(tempResult.BSSID);
            marker.setTitle(tempResult.SSID);
            if(!tempResult.isFloating)
            {
                marker.setTitle(tempResult.SSID);
                marker.setBSSID(tempResult.BSSID);
                marker.setRss(tempResult.level);
            }
            else
            {
                marker.setTitle(tempResult.SSID);
                marker.setBSSID(tempResult.BSSID);
                marker.setRss(tempResult.level);
            }
       /*     if(marker.isInfoWindowShown())
            {
                marker.hideInfoWindow();
                marker.showInfoWindow();
            }*/

        }

    }

	@Override
	public void onStop() {
		super.onStop();
		try{
			if(navigationMode)
			{
				sensorManager.unregisterListener(sensorListenerAccel);
				sensorManager.unregisterListener(sensorListenerMagn);
			}
		}
		catch(NullPointerException ex)
		{
			Log.e("unregistration fail", ex.getMessage());
		}
	}
		
	
	@Override
	public void onResume() {
		super.onResume();
		if(navigationMode)
		{
			sensorListenerAccel = new sListener();
			sensorManager.registerListener
					(sensorListenerAccel,
					(Sensor)sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER).get(0), 
					SensorManager.SENSOR_DELAY_NORMAL);
			sensorListenerMagn = new sListener();
			sensorManager.registerListener
			(sensorListenerMagn,
			(Sensor)sensorManager.getSensorList(Sensor.TYPE_MAGNETIC_FIELD).get(0), 
			SensorManager.SENSOR_DELAY_NORMAL);
		}

	}
	
	public void setRotation(float degree)
	{
		bearingInterval = (long)(Float.parseFloat(PreferenceManager.getDefaultSharedPreferences
				(getActivity()).getString(getString(R.string.prefMapBearingInterval), "0.5f")) * 1000000000);
		if(System.nanoTime() > (prevTime + bearingInterval))
		{
			
			CameraPosition old = map.getCameraPosition();
			float bearing = old.bearing;
			float endbearing = 0;
			if(degree > bearing)
			{
				endbearing = degree - bearing;
			}
			else
			{
				endbearing = bearing - degree;
			}
			if(endbearing > 8)
			{
				int compensation  = getActivity().getWindowManager().getDefaultDisplay().getRotation();
				degree = degree + compensation*90;
				CameraPosition pos = new CameraPosition(old.target,old.zoom,old.tilt,degree);
				map.animateCamera( CameraUpdateFactory.newCameraPosition(pos),10,null);
			}
				
			navigateToMarker(navigationTag,degree);	
			prevTime = System.nanoTime();
			
		}
    }

	

	
	class sListener implements SensorEventListener
	{

		SensorManager sensorManager;

		@Override
		public void onSensorChanged(SensorEvent event) {
		

			    if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
			      mGravity = event.values;
			    if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
			      mGeomagnetic = event.values;
			    if (mGravity != null && mGeomagnetic != null) {
			      float R[] = new float[9];
			      float I[] = new float[9];
			      boolean success = SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic);
			      if (success) {
			        float orientation[] = new float[3];
			        SensorManager.getOrientation(R, orientation);
			        float azimut = (orientation[0]*180f)/3.14f; 
			        setRotation(azimut);}
			    }
		}
			
		

		@Override
		public void onAccuracyChanged(Sensor sensor, int accuracy) {
			// TODO Auto-generated method stub
			
		}
		
		
		
	}
	
	class MyLocationListener implements OnMyLocationChangeListener
	{

		@Override
		public void onMyLocationChange(Location location) {
			if(navigationMode)
			{
				CameraPosition old = map.getCameraPosition();
				CameraPosition pos = new CameraPosition(new LatLng(location.getLatitude(),location.getLongitude()),old.zoom,old.tilt,old.bearing);
				map.animateCamera( CameraUpdateFactory.newCameraPosition(pos),10,null);
			}
			
		}
		
	}

    class MarkerInfoAdapter implements GoogleMap.InfoWindowAdapter {

        private final View mymarkerview;

        MarkerInfoAdapter() {
            mymarkerview = getActivity().getLayoutInflater().inflate(R.layout.marker_window, null);
        }

        @Override
        public View getInfoWindow(Marker marker) {
            render(marker, mymarkerview);
            return mymarkerview;
        }

      //  public void set

        @Override
        public View getInfoContents(Marker marker) {
            return null;
        }

        private void render(Marker marker, View view) {
            try{
                if(marker == null){return;}


            String snippet = marker.getSnippet();
            String title = marker.getTitle();
            char[] snippetArray = new char[snippet.length()];
            snippet.getChars(0,snippet.length(),snippetArray,0);
            StringBuilder builder = new StringBuilder();
            String bssid = "";
            String state = "";
            String rss = "";
            int iter = 0;
            for( char c : snippetArray)
            {
                if(c != '|')
                {
                    builder.append(c);
                }
                else
                {
                    switch (iter)
                    {
                        case 1:
                            bssid = builder.toString();
                            builder = new StringBuilder();
                            break;
                        case 0:
                            state = builder.toString();
                            builder = new StringBuilder();
                            break;
                        case 2:
                            rss = builder.toString();
                            builder = new StringBuilder();
                            break;
                    }
                    iter++;

                }
            }
            iter = 0;
            final TextView tvSsid= (TextView) view.findViewById(R.id.tvMarkerSsid);
            final TextView tvBssid = (TextView) view.findViewById(R.id.tvMarkerBssid);
            final TextView tvState = (TextView) view.findViewById(R.id.tvMarkerState);
            final TextView tvRss = (TextView) view.findViewById(R.id.tvMarkerSignalStrength);

            tvSsid.setText(title);
            tvBssid.setText(bssid);
            tvRss.setText(rss);
            tvState.setText(state);
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }

    }
}


