package edu.ucsd.dj.others;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import edu.ucsd.dj.managers.DJPhoto;
import edu.ucsd.dj.managers.Settings;

/**
 * Created by jonathanjimenez on 6/8/17.
 */

public class FileUtilities {
    public static void copy(String fromFile, String toFile){
        File from = new File(fromFile);
        File to = new File(toFile);

        InputStream is = null;
        OutputStream os = null;
        try {
            is = new FileInputStream(from);
            os = new FileOutputStream(to);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
            is.close();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateMediastore(String fileName){
        File file = new File(fileName);
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        DJPhoto.getAppContext().sendBroadcast(intent);
    }
    public static void deleteFile(String fileName){
        File file = new File(fileName);
        file.delete();
    }

}
