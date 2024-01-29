package com.example.wifiloc;

/**
 * Created by pac on 01.06.14.
 */

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.maps.SupportMapFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by pac on 31.05.14.
 */
public class FragmentCommandMenu extends SupportMapFragment {


    private final static String filePath = "/Android/data/com.example.wifiloc/files/";

    private CommandMenuListener commandMenuListener;
    private Button bScanNow;
    private Button bNextLocation;
    private Button bPreviousLocation;
    private Button bLoadLocationList;
    private Button bEnterLocation;
    private Button bMinimalize;

    private double tempLatitude;
    private double tempLongitude;
    private boolean isListLoaded = false;
    private int locationIterator = 0;
    private List<Location> locationList;


    public FragmentCommandMenu(CommandMenuListener _commandMenuListener)
    {
        this.commandMenuListener=_commandMenuListener;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        return inflater.inflate(R.layout.fragment_command_menu, container, false); //TODO


    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    @Override
    public void onStart()
    {
        super.onStart();
        bScanNow = (Button) getView().findViewById(R.id.bScanNow);
        bNextLocation = (Button) getView().findViewById(R.id.bNextLocation);
        bPreviousLocation = (Button) getView().findViewById(R.id.bPreviousLocation);
        bMinimalize = (Button) getView().findViewById(R.id.bMinimalize);
        bEnterLocation = (Button) getView().findViewById(R.id.bChangeLocation);
        bLoadLocationList = (Button) getView().findViewById(R.id.bLoadLocationList);
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        LayoutInflater inflater = LayoutInflater.from(getActivity());
        ViewGroup viewGroup = (ViewGroup) getView();

        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
            viewGroup.removeAllViewsInLayout();
            inflater.inflate(R.layout.fragment_command_menu_landscape, viewGroup);
            initialize();

        } else {
            viewGroup.removeAllViewsInLayout();
            inflater.inflate(R.layout.fragment_command_menu, viewGroup);
            initialize();
        }
    }

    private void hide()
    {
        this.hide();
    }

    public void initialize()
    {
        bScanNow = (Button) getView().findViewById(R.id.bScanNow);
        bNextLocation = (Button) getView().findViewById(R.id.bNextLocation);
        bPreviousLocation = (Button) getView().findViewById(R.id.bPreviousLocation);
        bMinimalize = (Button) getView().findViewById(R.id.bMinimalize);
        bEnterLocation = (Button) getView().findViewById(R.id.bChangeLocation);
        bLoadLocationList = (Button) getView().findViewById(R.id.bLoadLocationList);

        bScanNow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                commandMenuListener.scanClicked();
            }
        });

        bMinimalize.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hide();
            }
        });

        bEnterLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Dialog enterCordDialog = enterCord();
                if (enterCordDialog != null)
                {
                    enterCordDialog.show();
                    Location location = new Location("Custom");
                    location.setLatitude(tempLatitude);
                    location.setLatitude(tempLongitude);
                    commandMenuListener.enterLocation(location);
                }

                enterCordDialog.show();
            }
        });

        bNextLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isListLoaded){
                    return;
                }
                else
                {
                    if(locationIterator < (locationList.size()-1))
                    {
                        locationIterator = locationIterator +1;
                        commandMenuListener.enterLocation(locationList.get(locationIterator));
                    }
                    else
                    {
                        commandMenuListener.enterLocation(locationList.get(locationIterator));
                    }
                }
            }
        });

        bPreviousLocation.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!isListLoaded){
                    return;
                }
                else
                {
                    if(locationIterator == 0)
                    {
                        commandMenuListener.enterLocation(locationList.get(locationIterator));
                    }
                    else if(locationIterator > 0)
                    {
                        locationIterator = locationIterator -1;
                        commandMenuListener.enterLocation(locationList.get(locationIterator));
                    }
                }
            }
        });

        bLoadLocationList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(loadLocationList())
                {
                    isListLoaded = true;
                }
                else
                {
                    isListLoaded = false;
                }
            }
        });
    }

    private Dialog enterCord() {
        final View layout = View.inflate(getActivity(), R.layout.dialog_enter_cord, null);

        final EditText etLatitude = ((EditText) layout.findViewById(R.id.etLatitude));
        final EditText etLongitude= ((EditText) layout.findViewById(R.id.etLongitude));
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setIcon(0);

        builder.setPositiveButton("Save", new Dialog.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            try{

                tempLatitude = Double.parseDouble(etLatitude.getText().toString().trim());
                tempLongitude = Double.parseDouble(etLongitude.getText().toString().trim());

                if( tempLatitude < 0 || tempLatitude > 90 || tempLongitude < 0 || tempLongitude > 90)
                {
                    Toast.makeText(getActivity(),"Wpisano nie poprawne dane",Toast.LENGTH_LONG).show();
                }
            }
            catch(NumberFormatException nfe)
            {
                Toast.makeText(getActivity(),"Wpisano nie poprawne dane",Toast.LENGTH_LONG).show();
                return;
            }
            }
        });
        builder.setView(layout);
        return builder.create();
    }
    //TODO pomysly
    // uruchomienie skanowanie w trybie na zawolanie daje mozliwosc startscanu
    // zmiana trybu powoduje zmiane trybu skanowania od razu
    // uruchomienie uslug lokalizacyjnych w trybie custom daje mozliwosc otwarcia menu comandera
    // start scan ma zalezec od od ui kompletnie,a le warto skorzystac z opracowanego wczesniej rozwiazania odnosnie ilosci lokalizacji...
    //
    private boolean loadLocationList()
    {
        String filename = PreferenceManager.getDefaultSharedPreferences
            (getActivity()).getString(getString(R.string.prefLocationListFileName), "locationlist"));
        String directory = Environment.getExternalStorageDirectory().getAbsolutePath();
        directory = directory + filePath;
        File destFile = new File(directory+filename+".txt");
        boolean fileExists = destFile.exists();
        FileInputStream inputStream;
        locationList = new ArrayList<Location>(); // TODO filename in settings
        locationIterator = 0;
        BufferedReader in;
        if(fileExists){
            try
            {
                inputStream = new FileInputStream(destFile);
                in= new BufferedReader(
                        new InputStreamReader(
                                inputStream, "UTF16"));
                                while(true)
                                {
                                      String slat = in.readLine();
                                      String slongi = in.readLine();
                                      if(slat == null || slongi == null)
                                      {
                                          if(locationList != null)
                                          {
                                              return true;
                                          }
                                          else
                                          {
                                              return false;
                                          }
                                      }
                                      double lat = Double.parseDouble(slat.trim());
                                      double longi = Double.parseDouble(slongi.trim());

                                      if( lat < 0 || lat > 90 || longi < 0 || longi> 90)
                                      {
                                          Toast.makeText(getActivity(),"Dane znajdujące się w pliku są niepoprawne",Toast.LENGTH_LONG).show();
                                          if(locationList.size() > 0) { return true;
                                          }
                                          else { return false;
                                          }
                                      }
                                      else
                                      {
                                          Location _location = new Location("Custom");
                                          _location.setLatitude(lat);
                                          _location.setLongitude(longi);

                                          locationList.add(_location);
                                      }
                                }

            }
            catch(FileNotFoundException fnfe)
            {
                fnfe.printStackTrace();
                return false;
            }
            catch(UnsupportedEncodingException uex)
            {
                uex.printStackTrace();
                return false;
            }
            catch(IOException ex)
            {
                if(locationList.size() > 0){
                    return true;}
                    else
                {
                    return false;
                }
            }
        }
        else
        {
            return false;
        }

    }

}

