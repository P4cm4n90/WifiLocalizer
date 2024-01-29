package com.example.wifiloc;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

public class CListAdapter extends BaseAdapter {

	private String[] data;
    private Context ctx;
    
    public CListAdapter(Context ctx,String[] importeddata) {
        this.ctx = ctx;
        this.data = importeddata;
        }
	@Override
	public int getCount() {
		// TODO Auto-generated method stub
		int ilosc = 0;
		if(data.length>1) {
			ilosc = data.length/6;
		}else {
			ilosc = 1;}
		return ilosc;
	}

	@Override
	public Object getItem(int position) {
		// TODO Auto-generated method stub
		String[] obj = {
		data[position*6],
		data[position*6+1],
		data[position*6+2],
        data[position*6+3],
        data[position*6+4],
        data[position*6+5]
				};

        /*
        	String[] obj = {
		data[position*2],
		data[position*2+1],
		data[position*2+2]
				};   --- last save


         */
		return obj;
	}

	@Override
	public long getItemId(int position) {
		// TODO Auto-generated method stub
		return 0;
	}
	
	private class ViewHolderPattern {
		    TextView nameAndMac;
		    TextView location;
		    TextView tvRSS;
		    ProgressBar pbRSS;
		    ImageView wifiView;
            ImageView wifiReadyLoc;
            ImageView wifiRefreshLoc;
            ImageView wifiFloating;
		    }
	
	 
	public void UpdateData(String[] importeddata) 
	{
		this.data = importeddata;
		this.notifyDataSetChanged();
	}
	 
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		
		 ViewHolderPattern view_holder;
		 
		  if( convertView == null) {
			LayoutInflater inflater = (LayoutInflater) ctx.getSystemService(Context.LAYOUT_INFLATER_SERVICE);		 
			convertView = inflater.inflate(R.layout.wificell, parent, false);		 
			
			view_holder = new ViewHolderPattern();
			view_holder.wifiView = (ImageView) convertView.findViewById(R.id.ivWifiIcon);
			view_holder.nameAndMac = (TextView ) convertView.findViewById(R.id.tvWifiCellId);
			view_holder.location = (TextView ) convertView.findViewById(R.id.tvWifiCellLocation);
			view_holder.pbRSS = (ProgressBar) convertView.findViewById(R.id.pbWifiCellSignalStrength);
			view_holder.tvRSS = (TextView) convertView.findViewById(R.id.tvPBSignalStrength);
            view_holder.wifiReadyLoc = (ImageView) convertView.findViewById(R.id.ivReadyLoc);
            view_holder.wifiRefreshLoc = (ImageView) convertView.findViewById(R.id.ivRefreshLoc);
            view_holder.wifiFloating = (ImageView) convertView.findViewById(R.id.ivFloating);
			convertView.setTag(view_holder);
			
		  } else {
			  view_holder = (ViewHolderPattern) convertView.getTag();
		  }
		  
		  view_holder.nameAndMac.setText(data[(position*6)]);
		  view_holder.location.setText(data[(position*6)+1]);
		  //Progress assumption maxdbm = -10 min dbm = -100 code down here is conversion of these values
		  int rssvalue = 0;
		  try{
		  rssvalue = Integer.parseInt(data[(position*6)+2]);
		  }
		  catch(Exception ex){
			  rssvalue = -100;
		  }

        int ress= rssvalue+95;
        view_holder.pbRSS.setMax(65);
        if(ress > 65){
            ress = 65;}

        if(rssvalue == -100) // TODO check if its worth to do so
        {
            view_holder.pbRSS.setProgress(0);
            view_holder.tvRSS.setText("brak sygnału");
        }
        else
        {
            view_holder.pbRSS.setProgress(ress);
            view_holder.tvRSS.setText(rssvalue+"dBm");
        }

        if(ress <= 0)
        {
            view_holder.wifiView.setImageResource(R.drawable.wifiiconnull);
        }
		  if((ress>0) && (ress<16))
		  {
			  view_holder.wifiView.setImageResource(R.drawable.wifiicon0);
		  }
		  if((ress>15) && (ress<31))
		  {
			  view_holder.wifiView.setImageResource(R.drawable.wifiicon1);
		  }
		  if((ress>30) && (ress<51))
		  {
			  view_holder.wifiView.setImageResource(R.drawable.wifiicon2);
		  }
		  if((ress>50) && (ress<66))
		  {
			  view_holder.wifiView.setImageResource(R.drawable.wifiicon3);
		  }

          if("true".equalsIgnoreCase(data[(position*6)+3]))
          {
                if(view_holder.wifiReadyLoc.getVisibility() == View.INVISIBLE)
                {
                    view_holder.wifiReadyLoc.setVisibility(View.VISIBLE);
                }
          }
          else
          {
              if(view_holder.wifiReadyLoc.getVisibility() ==View.VISIBLE)
              {
                  view_holder.wifiReadyLoc.setVisibility(View.INVISIBLE);
              }
          }
          if("true".equalsIgnoreCase(data[(position*6)+4]))
          {
              if(view_holder.wifiRefreshLoc.getVisibility() == View.INVISIBLE)
              {
                  view_holder.wifiRefreshLoc.setVisibility(View.VISIBLE);
              }
          }
          else
          {
              if(view_holder.wifiRefreshLoc.getVisibility() ==View.VISIBLE)
              {
                  view_holder.wifiRefreshLoc.setVisibility(View.INVISIBLE);
              }
          }

          if("true".equalsIgnoreCase(data[(position*6)+5]))
          {
              if(view_holder.wifiFloating.getVisibility() == View.INVISIBLE)
              {
                  view_holder.wifiFloating.setVisibility(View.VISIBLE);
              }
          }
          else
          {//wifiFloating to add
              if(view_holder.wifiFloating.getVisibility() ==View.VISIBLE)
              {
                  view_holder.wifiFloating.setVisibility(View.INVISIBLE);
              }
          }

		  return convertView;
	}
	

}
