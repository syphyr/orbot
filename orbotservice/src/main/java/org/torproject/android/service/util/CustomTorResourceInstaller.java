package org.torproject.android.service.util;

import android.content.Context;

import org.torproject.android.service.OrbotConstants;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CustomTorResourceInstaller {

    private final File installFolder;
    private final Context context;

    public CustomTorResourceInstaller(Context context, File installFolder) {
        this.installFolder = installFolder;
        this.context = context;
    }

    /*
     * Write the inputstream contents to the file
     */
    private static void streamToFile(InputStream stm, File outFile) throws IOException {
        byte[] buffer = new byte[1024];
        int bytecount;
        OutputStream stmOut = new FileOutputStream(outFile.getAbsolutePath(), false);

        while ((bytecount = stm.read(buffer)) > 0) {
            stmOut.write(buffer, 0, bytecount);
        }

        stmOut.close();
        stm.close();
    }

    /*
     * Extract the Tor resources from the APK file using ZIP
     */
    public void installGeoIP() throws IOException {
        if (!installFolder.exists()) installFolder.mkdirs();
        assetToFile(OrbotConstants.GEOIP_ASSET_KEY, OrbotConstants.GEOIP_ASSET_KEY);
        assetToFile(OrbotConstants.GEOIP6_ASSET_KEY, OrbotConstants.GEOIP6_ASSET_KEY);
    }

    /*
     * Reads file from assetPath/assetKey writes it to the install folder
     */
    private void assetToFile(String assetPath, String assetKey) throws IOException {
        InputStream is = context.getAssets().open(assetPath);
        File outFile = new File(installFolder, assetKey);
        streamToFile(is, outFile);
    }
}

