package com.reactnativehce.services;

import android.nfc.cardemulation.HostApduService;
import android.os.Bundle;
import android.util.Log;
import com.reactnativehce.HceAndroidViewModel;
import com.reactnativehce.IHCEApplication;
import com.reactnativehce.PrefManager;
import com.reactnativehce.apps.nfc.NFCTagType4;
import com.reactnativehce.utils.BinaryUtils;
import java.util.ArrayList;
import java.util.Arrays;

public class CardService extends HostApduService {
    private static final String TAG = "CardService";
    private static final byte[] SELECT_APDU_HEADER = BinaryUtils.HexStringToByteArray("00A40400");
    private static final byte[] CMD_OK = BinaryUtils.HexStringToByteArray("9000");
    private static final byte[] CMD_ERROR = BinaryUtils.HexStringToByteArray("6A82");

    private final ArrayList<IHCEApplication> registeredHCEApplications = new ArrayList<>();
    private IHCEApplication currentHCEApplication = null;
    private HceAndroidViewModel model = null;
    private PrefManager prefManager;

    @Override
    public byte[] processCommandApdu(byte[] commandApdu, Bundle extras) {
      if (currentHCEApplication != null) {
        return currentHCEApplication.processCommand(commandApdu);
      }

      byte[] header = Arrays.copyOfRange(commandApdu, 0, 4);

      if (Arrays.equals(SELECT_APDU_HEADER, header)) {
        for (IHCEApplication app : registeredHCEApplications) {
          if (app.assertSelectCommand(commandApdu)) {
            currentHCEApplication = app;
            this.model.getLastState().setValue("NFC");
            return CMD_OK;
          }
        }
      }

      return CMD_ERROR;
    }

    @Override
    public void onCreate() {
      Log.d(TAG, "Starting service");

      this.prefManager = PrefManager.getInstance(getApplicationContext());
      this.model = HceAndroidViewModel.getInstance(this.getApplicationContext());
      this.model.getLastState().setValue("CONNECTED");

      registeredHCEApplications.add(new NFCTagType4(
        prefManager.getType(),
        prefManager.getContent(),
        prefManager.getWritable()
      ));
    }

    @Override
    public void onDeactivated(int reason) {
      this.model.getLastState().setValue("DISCONNECTED");
      Log.d(TAG, "Finishing service: " + reason);
    }
}
