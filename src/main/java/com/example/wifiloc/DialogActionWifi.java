package com.example.wifiloc;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

public class DialogActionWifi extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(R.string.action)
               .setItems(R.array.actionArray, new DialogInterface.OnClickListener() {
                   public void onClick(DialogInterface dialog, int which) {
                	   switch(which) {
                		   case 0:
                			   onPostDialogExecution(0);
                			   return;
                		   case 1:
                			   onPostDialogExecution(1);
                			   return;
                		   case 2:
                			   onPostDialogExecution(2);
                			   return;
                		   case 3:
                			   onPostDialogExecution(3);
                			   return;
                	   }
                	   
               }
        });
        
        
        return builder.create();

        
    }
    
    public void onPostDialogExecution(int i){}
}
