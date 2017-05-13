package edu.ucsd.dj;

import android.content.Context;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by jakesutton on 5/6/17.
 */
public class PhotoCollection {

    //Collection of photos queried from the gallery
    private List<Photo> album;
    //A queue to handle previous feature, keeping track of your browsing history
    private Deque<Photo> history;
    //Current pointer to the image that's being set as the wallpaper
    private int curr;

    private static final PhotoCollection ourInstance = new PhotoCollection();

    public static PhotoCollection getInstance() {
        return ourInstance;
    }

    protected PhotoCollection() {
        album = new ArrayList<Photo>();
        history = new LinkedList<Photo>();
        curr = 0;
    }

    /**
     *  Updates the list of photo by querying from the current gallery and put
     *  them into the current photo array
     *
     */
    public void update(Context context) {

        PhotoLoader loader = new PhotoLoader(context);
        ArrayList<Photo> newAlbum = loader.getPhotos();

        //TODO optimization problem
        for(Photo photo: newAlbum){
            if(!album.contains(photo)) {
                album.add( photo );
            }
        }

        Collections.sort(album);
    }

    /**
     *  Updates the values of photo, used after changing settings
     *
     */
    public void sort() {

        //TODO optimization problem
        for(Photo photo: album){
            photo.calculateScore();
        }

        Collections.sort(album);

        curr = 0;
    }

    /**
     * Releases the current photo from the album
     * next() should be called immediately after this method
     */
    public void release() {
        album.get(curr).release();
    }

    /**
     * Return the next photo from the current list
     * If there is no image, return the stock image
     * Increases the current pointer to the next image
     * If a photo is blacklisted, it will not be returned
     * @return A photo object from the album data structure
     */
    public Photo next() {

        // TODO return the special image
        if (album.size() < 1) return null;

        // Add to history if it is not released
        if(!album.get(curr).isReleased()){
            history.addFirst(album.get(curr));
        }

        curr++;

        // Loop back to start if need be
        if (curr == album.size()) {
            sort();
        }

        // TODO check for removed from album
        Photo result = album.get(curr);
        return result;
    }

    /*
     * Returns the current photo
     */
    public Photo current() {
        Photo result = album.get(curr);
        return result;
    }

    /**
     * Return the previous photo from user's history
     *
     * @return A photo object from the history data structure
     */
    public Photo previous() {

        --curr;
        if (curr < 0) curr += album.size();

        return history.removeFirst();
    }

    /**
     * Checks if there is a previous photo in the history data structure
     *
     * @return true if there is a photo in history
     *         false if there isn't a photo in history
     */
    public boolean hasPrevious() {
        return !history.isEmpty();
    }


//    TODO - Bring me back to life :(

//    /**
//     * Save current album to file using album field as name.
//     *
//     * @param context The current state of the application
//     * @see {@linktourl http://stackoverflow.com/questions/4118751/how-do-i-serialize-an-object-and-save-it-to-a-file-in-android}
//     */
//    public void saveToFile(Context context){
//        try {
//            FileOutputStream fos = context.openFileOutput(album + ".album", Context.MODE_PRIVATE);
//            ObjectOutputStream os = new ObjectOutputStream(fos);
//            os.writeObject(this);
//            os.close();
//            fos.close();
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
//
//    /**
//     * Load Album using album field
//     * @param context Current state of the application
//     * @param albumName the name of the album that needs to be loaded
//     * @see {@linktourl http://stackoverflow.com/questions/4118751/how-do-i-serialize-an-object-and-save-it-to-a-file-in-android}
//     */
//    public static DJPhotoCollection loadFromFile(Context context, String albumName){
//        try {
//            FileInputStream fis = context.openFileInput(albumName + ".album");
//            ObjectInputStream is = new ObjectInputStream(fis);
//            DJPhotoCollection tempAlbum = (DJPhotoCollection) is.readObject();
//            is.close();
//            fis.close();
//            Load new photos from album
//            tempAlbum.update(context);
//            return tempAlbum;
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return null;
//    }
}
