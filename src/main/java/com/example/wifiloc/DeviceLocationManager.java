package com.example.wifiloc;

import android.annotation.SuppressLint;
import android.os.AsyncTask;
import android.text.format.Time;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;

import org.jscience.geography.coordinates.LatLong;
import org.jscience.geography.coordinates.UTM;
import org.jscience.geography.coordinates.crs.ReferenceEllipsoid;
import org.jscience.mathematics.vector.Float64Matrix;
import org.jscience.mathematics.vector.Float64Vector;

import java.util.List;
import java.util.Random;

import javax.measure.quantity.Length;
import javax.measure.unit.NonSI;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;


public class DeviceLocationManager  {

	private double[] vecRss;
	private double[] vecDiffR;
	private double[] vecX;
	private double[] vecY;
	private double[] vecK;
	private double[] matH;
	private double[] vecR;
	private double[] vecStdDev;
	private double[][] matQ;
	private double[][] matGa;
	private double[] locAccuracy;
	private Unit<Length> unit = SI.METRE;
	private int longZone;
	private char latZone;
	private boolean isIndoor;

	// latitude -   northing
	// longitude -- easting
	public void putData(List<ExtendedLocation> locList) throws Exception{
		
		if(locList == null | locList.size() <2)
			throw new NullPointerException("Class: DeviceLocation, Method: put Data, Message: Given list is empty");
		int size = locList.size();

        if(locList.get(1).isLocatedIndoor()) { isIndoor = true; }
        locAccuracy = new double[locList.size()];
		vecRss = new double[size];
		vecX = new double[size];
		vecY = new double[size];
		vecK = new double[size];
		vecR = new double[size];
		
		vecDiffR = new double[size-1];
		vecStdDev = new double[size-1];
		
		longZone = locList.get(0).getLongZone();
		latZone = locList.get(0).getLatZone();
		
		for(int i = 0; i < size; i++){
            locAccuracy[i] =(double)locList.get(i).getAccuracy();
			vecRss[i] = locList.get(i).getRssi(); // TODO zmiana usunalem - 30 :)
			UTM temploc = locList.get(i).getUtmLocation();
			vecX[i] = temploc.eastingValue(SI.METRE);
			vecY[i] = temploc.northingValue(SI.METRE);
			vecK[i] = Math.pow(vecX[i],2) + Math.pow(vecY[i],2);
			if(i>0){
				if(locList.get(i).getStdDev() == 0 )
				{
					vecStdDev[i-1]=0.1;
                }
                else
                {
                    vecStdDev[i-1]=Math.abs(locList.get(i).getStdDev() - locList.get(0).getStdDev());
                }
			}
		}
			
		
		
	}
	//* rssVal - rss value
	//* nFactor - propagation factor
	//* faf - floor attenuation factor
	//* -30dBm - loss at one metre
	private static double rssToDistance(double rssVal,double tPower, double nFactor, double FAF)
	{
	if(FAF != 0){


	    }// Ponder about FAF
		double lossPower = tPower - 30 - rssVal - FAF;
	
		double distance = Math.pow(10, (lossPower/(10*nFactor)));
		return distance;
		
	}




	private static double diffRssToDistance(double diffRssVal,double refDistance ,double nFactor)
	{
		double distanceDiff = 0; //TODO where is FAF
		try{
		double step1 = Math.pow(10,(diffRssVal/(10*nFactor)));
		return distanceDiff = (step1 - 1) * refDistance;
		}		
		catch(Exception e){
			return distanceDiff;
		}
		
	}
	
	public void computeLocation(double nFactor, double FAF)
	{
        FAF = Math.abs((new Random()).nextGaussian()*FAF);
        if(Double.isNaN(FAF)){ FAF = 0;}

		Double[] params = { (double) nFactor, FAF };
		new ComputeLocation().execute(params);		
	}
		
	public void onResultsGet(LatLng loc, double accuracy, Time time){};

	private double computeAccuracy(double cordX,double cordY)
    {
        int size = vecX.length;
        double[][] matA = new double[size][2];
        for(int i = 0;i<size;i++)
        {
            matA[i][0] = (vecX[i]-cordX)/vecR[i];
            matA[i][1] = (vecY[i]-cordY)/vecR[i];
        }
        Float64Matrix mA = Float64Matrix.valueOf(matA);
        Float64Matrix mQ = (mA.transpose().times(mA)).inverse();

        double meanLocAcc = calcMeanValue(locAccuracy);
        double sdLocAcc = calcStdDev(locAccuracy,meanLocAcc);
        double hdop = Math.abs(Math.sqrt(mQ.get(0,0).doubleValue()+mQ.get(1,1).doubleValue()));
        if(Double.isNaN(hdop)){
            Log.e("Accuracy NAN",mA.toString()); hdop = 10;}
        double result = Math.sqrt( Math.pow(hdop,2)+
                                   Math.pow(meanLocAcc,2)+
                                   Math.pow(sdLocAcc,2));
        if(Double.isNaN(result)){
         Log.e("Accuracy NAN",mA.toString()); result = 20;}
        return result;
    }
		
	class ComputeLocation extends AsyncTask<Double,Void,Double[]>
	{

		@SuppressLint("UseValueOf")
		@Override
		protected Double[] doInBackground(Double... params) {
			if(params.length < 1){
				Log.w("Compute Location Task","Not enough params was sent");}
			float nFactor = new Double(params[0]).floatValue();
			double FAF = params[1];

			int length = vecDiffR.length;
			matQ = new double[length][length];
			matH = new double[length];
			matGa = new double[length][3];
            vecR[0] = rssToDistance(vecRss[0],-10,nFactor,FAF);
			for(int i = 0; i < length; i++)
			{
                    vecR[i+1] = rssToDistance(vecRss[i+1],-10,nFactor,FAF);
					vecDiffR[i] = diffRssToDistance((vecRss[i+1] - vecRss[0]),vecR[i+1],nFactor);
					matH[i] = (Math.pow(vecDiffR[i],2) - vecK[i+1] + vecK[0])/2; // podzielona przez 1/2
					matGa[i][0] = -(vecX[i+1] - vecX[0]); //kolejne linie + ta = mnozenie przez -1 dla kazdego elementu Ga;
					matGa[i][1] = -(vecY[i+1] - vecY[0]);
					matGa[i][2] = -vecDiffR[i];
					for(int k = 0; k < length;k++)
					{
						if(k == i){
							matQ[i][k] = 1/vecStdDev[i];}
						else{
							matQ[i][k] = 0;}
					}
						
			}
			try{
				// inne odwracanie ztobic
				Float64Matrix matrixInvQ = Float64Matrix.valueOf(matQ);
				//Float64Matrix matrixInvQ = Float64Matrix.valueOf(matQ).inverse();
				Float64Vector matrixH = Float64Vector.valueOf(matH);
				Float64Matrix matrixGa = Float64Matrix.valueOf(matGa);
				Float64Matrix matrixGaT = matrixGa.transpose();
				
				Float64Matrix step1 = ((matrixGaT.times(matrixInvQ)).times(matrixGa)).inverse();
				Float64Vector step2 = ((step1.times(matrixGaT)).times(matrixInvQ)).times(matrixH);
				vecR[0]=step2.getValue(2);

				UTM step3 = UTM.valueOf(longZone, latZone, step2.getValue(0), step2.getValue(1), SI.METRE);
				
				LatLong step4 = UTM.utmToLatLong(step3, ReferenceEllipsoid.WGS84);

                double accuracy = computeAccuracy(step2.getValue(0),step2.getValue(1));

				return new Double[]{step4.latitudeValue(NonSI.DEGREE_ANGLE),
				                    step4.longitudeValue(NonSI.DEGREE_ANGLE),
				                    accuracy};

			}
			catch(Exception ex)
			{
				Log.e("Matrix Error",ex.getMessage());
				return null;
			}
		}
		
		@Override
		protected void onPostExecute(Double[] data)
		{
		Time time = new Time();
			time.setToNow();
			for( double d : data) { if (Double.isNaN(d)) {
			Log.e("Location NAN",data.toString());
			return;}}
			LatLng loc = new LatLng(data[0],data[1]);
			onResultsGet(loc, data[2], time);
		}
	}



    private double calcMeanValue(double[] array)
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
            return 1;
        }
    }


    private double calcStdDev(double[] array,double mean)
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
		
}
