package com.example.wifiloc;

public interface DataObserver {
	
	final static int minRss = 10;
	final static int minLocCount = 7;

	public abstract void onDataCollected(String bssid);

    public abstract void onReadyToLoc(String bssid);
	
	public abstract void onResultsAvailable(DeviceLocation loc);
	
	public abstract int[] getSettings();
	
	public abstract void omGuideModeLocationChange();
	
	public abstract void foundFloatingDevice(String bssid,int floatingLocNumber);

    public abstract void onGuideModeNewData();

}
