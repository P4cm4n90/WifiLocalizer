package com.example.wifiloc;

import android.location.Location;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.TreeMap;


public class FragmentWifiList extends Fragment {


    public AsyncTask updateWifiListTask;
	private ListView wifiList;
	private CListAdapter laAdapter;
	private WifiManager wManager;
    private Switch sSwitchTerminal;
    private boolean showAvailable = true;
	private List<ScanResult> lScanResult;
	private DialogActionWifi dialogActionWifi;
	public boolean isVisible = false;

    private TreeMap<String,DeviceSettings> storedData;
    private TreeMap<String,DeviceSettings> lastData;

    private int lastDevFoundQuantity = 0;
   // private List<Stringt> lastData;

	
	private int lastSelectedItem = 0;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        wManager = (WifiManager)getActivity().getSystemService(getActivity().WIFI_SERVICE);
        storedData = new TreeMap<String, DeviceSettings>();
        lastData = new TreeMap<String, DeviceSettings>();

        //setRetainInstance(true);
    }
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		isVisible = true;
		return  inflater.inflate(R.layout.fragment_wifilist, container,false);
	}
	

	
	@Override
    public void onStart() {
		super.onStart();
		wManager = (WifiManager)getActivity().getSystemService(getActivity().WIFI_SERVICE);
		isVisible = true;
		wifiList = (ListView) getView().findViewById(R.id.wifiList);

        sSwitchTerminal = (Switch) getView().findViewById(R.id.sSwitchTerminal);
        sSwitchTerminal.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked)
                {
                    showAvailable = false;
                    updateWifiListInfo();
                }
                else
                {
                    showAvailable = true;
                    updateWifiListInfo();
                }
            }
        });
		dialogActionWifi = new DialogActionWifi(){
			@Override
			public void onPostDialogExecution(int i)
			{
                onItemSelected(getStoredDataList(showAvailable).get(lastSelectedItem).BSSID,i);
				// onItemSelected(lScanResult.get(lastSelectedItem),i); TODO if new solution wouldn't work it's had to be restored
                //TODO USE comparator if you want sorted map by rss - to be done later
			}
			
		};
		wifiList.setOnItemClickListener(new OnItemClickListener(){

			@Override
			public void onItemClick(AdapterView<?> parent, View view,
					int position, long id) {
				lastSelectedItem = position;
				
				dialogActionWifi.show((getActivity()).getFragmentManager(), "Wifi choice");
				
			}
			
			
			
		});
		
		

	}

    @Override
    public void onResume()
    {
        super.onResume();
        updateWifiListInfo();
    }
	@Override
	public void onDestroyView()
	{
		super.onDestroyView();
		isVisible = false;
		/*if(tUpdate != null){
			getActivity().unregisterReceiver(wifiInfoReceiver);
			tUpdate.cancel();}*/
	}

	public void wipeData()
    {
        storedData = new TreeMap<String, DeviceSettings>();
        if(lScanResult != null)
        {
        updateWifiList(lScanResult);}
    }

	public void onItemSelected(String bssid, int actionNumber){};
	
	public void enableWifi()
	{
		wManager.setWifiEnabled(true);
	}

    public DeviceSettings getLastResult(String bssid)
    {
        return storedData.get(bssid);
    }

    public void setWifiRefreshLocationReadiness(String bssid, boolean enabled)
    {

        if(storedData == null)
        {
            return;
        }
        if(bssid.equalsIgnoreCase("all"))
        {
            String[] tempBssid =  Arrays.copyOf(storedData.keySet().toArray(),storedData.keySet().toArray().length,String[].class);

            for(String tempString : tempBssid)
            {
                storedData.get(tempString).disableWantToRefresh(); // TODO moze byc blad dodaje all nie wiem czemu
            }
        }
        else
        {
            if(storedData.containsKey(bssid)){
                if(enabled)
                {
                    storedData.get(bssid).enableWantToRefresh();
                }
                else
                {
                    storedData.get(bssid).disableWantToRefresh();
                }
            }

        }
    }

    public void addFloatingDevice(String bssid)
    {
        if(storedData.containsKey(bssid)){
             storedData.get(bssid).isFloating = true;}
    }

    public void setWifiLocalizationReadiness(String bssid, boolean enabled)
    {
        if(storedData.containsKey(bssid)){

            if(enabled)
            {
                storedData.get(bssid).enableReadyToLoc();
            }
            else
            {
                storedData.get(bssid).disableReadyToLoc();
            }
            //updateWifiListInfo();
    }
    }

	public void addLocationToItem(String bssid, LatLng location)
	{
        if(storedData.containsKey(bssid)){
            storedData.get(bssid).location = location;
            updateWifiListInfo();
        }

	}

    public List<DeviceSettings> getStoredDataList(boolean isAvailable)
    {
    if(storedData == null){return null;} if(storedData.size() < 1){return null;}
        if(isAvailable)
        {
            List<DeviceSettings> tempList = new ArrayList<DeviceSettings>(lastDevFoundQuantity);
            for(String s : storedData.keySet())
            {
                DeviceSettings tempSet;
                if((tempSet = storedData.get(s)).level != -100)
                    tempList.add(tempSet);
            }
            return tempList;
        }
        else
        {
            int length = storedData.size()-lastDevFoundQuantity;
            if(length < 1){ return null;}

            List<DeviceSettings> tempList = new ArrayList<DeviceSettings>(length);

            for(String s : storedData.keySet())
            {
                DeviceSettings tempSet;
                if((tempSet = storedData.get(s)).level == -100)
                    tempList.add(tempSet);
            }
            return tempList;
        }


    }

    @SuppressWarnings("unchecked")
	public void updateWifiList(List<ScanResult> templist)
	{

                if(templist != null){
                lastDevFoundQuantity = templist.size();
                try{

                if (storedData.size() > 0) {
                    for (String mkey : storedData.keySet()) {
                        boolean added = false;
                        for (ScanResult mscan : templist.toArray(new ScanResult[1])) {
                          if(mscan != null)
                          {
                            if (mscan.BSSID.equalsIgnoreCase(mkey)) {
                                storedData.get(mkey).setRss(mscan.level);
                                templist.remove(mscan);
                                added = true;
                            }
                          }
                        }
                        if (!added) {
                            storedData.get(mkey).setRss(-100);
                        }
                    }
                }
                for (ScanResult scres : templist) {
                    storedData.put(scres.BSSID, new DeviceSettings(scres));
                }
                }
                catch(NullPointerException exp)
                {
                    exp.printStackTrace();

                }

              }

        List lista = getStoredDataList(showAvailable);
        lScanResult = lista;
        if(lista == null){return;}
        int childCount = wifiList.getCount();



        if(isVisible){
            if(laAdapter != null && childCount > 0){
                int index = wifiList.getFirstVisiblePosition();
                View v = wifiList.getChildAt(0);

                int top = (v == null) ? 0 : v.getTop();

                if( lista.size() == 0){ return; }
                laAdapter.UpdateData(getComposedString(lista)); // TODO make another method in order to deal with unavailable terminals
                wifiList.setSelectionFromTop(index, top);
            }
            else{
                if( lista.size() == 0){ return; }
                laAdapter = new CListAdapter(getActivity(),getComposedString(lista));
                wifiList.setAdapter(laAdapter);

            }}
      }

	
    public void updateWifiListInfo()
    {
    try{
                if(storedData != null) {return;} if( storedData.size() > 0) { return; }

                    if( getStoredDataList(showAvailable).size() == 0){ return; }
                    if(laAdapter != null)
                    {
                        int index = wifiList.getFirstVisiblePosition();
                        View v = wifiList.getChildAt(0);

                        int top = (v == null) ? 0 : v.getTop();
                        wifiList.setSelectionFromTop(index, top);
                        laAdapter.UpdateData(getComposedString(getStoredDataList(showAvailable)));
                    }
                    else
                    {
                        int index = wifiList.getFirstVisiblePosition();
                        View v = wifiList.getChildAt(0);

                        int top = (v == null) ? 0 : v.getTop();
                        wifiList.setSelectionFromTop(index, top);
                        laAdapter = new CListAdapter(getActivity(),getComposedString(getStoredDataList(showAvailable)));
                    }
                    wifiList.setAdapter(laAdapter);

    }
    catch(Exception ex)
    {
        ex.printStackTrace();
    }

    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if (isVisibleToUser) { updateWifiListInfo(); }

    }

	private String[] getComposedString(List<DeviceSettings> templist)
	{

       String[] tempstring = new String[templist.size()*6];

	for(int i=0; i<templist.size();i++)
		{
			LatLng tempLoc;

            if(storedData.get(templist.get(i).BSSID) != null){
			if((tempLoc = storedData.get(templist.get(i).BSSID).location) != null)
			{
				String strLongitude = Location.convert(tempLoc.latitude, Location.FORMAT_SECONDS);
				String strLatitude = Location.convert(tempLoc.longitude, Location.FORMAT_SECONDS);
				tempstring[(i*6)+1]= strLongitude +" N "+strLatitude+" E ";
			}
			else
			{
				tempstring[(i*6)+1]="Położenie nieznane";
			}

			tempstring[i*6]=templist.get(i).SSID + " (" + templist.get(i).BSSID +")";

			tempstring[(i*6)+2]= Integer.toString(templist.get(i).level);

            tempstring[(i*6)+3] = String.valueOf(storedData.get(templist.get(i).BSSID).readyToLoc);

            tempstring[(i*6)+4] = String.valueOf(storedData.get(templist.get(i).BSSID).wantToRefresh);

            tempstring[(i*6)+5] = String.valueOf(storedData.get(templist.get(i).BSSID).isFloating);


		}}
		return tempstring;
        }

}

	
	


