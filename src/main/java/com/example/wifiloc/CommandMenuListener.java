package com.example.wifiloc;
import android.location.Location;
/**
 * Created by pac on 01.06.14.
 */
public interface CommandMenuListener {

    public abstract void scanClicked();

    public abstract void enterLocation(Location _location);


}
