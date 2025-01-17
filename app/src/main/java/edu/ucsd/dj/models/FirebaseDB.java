package edu.ucsd.dj.models;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Query;
import com.google.firebase.database.Transaction;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FileDownloadTask;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import edu.ucsd.dj.interfaces.models.IFriendList;
import edu.ucsd.dj.interfaces.IRemotePhotoStore;
import edu.ucsd.dj.interfaces.models.IUser;
import edu.ucsd.dj.managers.Settings;
import edu.ucsd.dj.others.FileUtilities;
import edu.ucsd.dj.others.PhotoCollection;
import edu.ucsd.dj.others.PhotoLoader;

/**
 * Created by nguyen on 6/4/2017.
 * Class for our database
 */
public class FirebaseDB implements IRemotePhotoStore {

    private static final StorageReference storageRef =
            FirebaseStorage.getInstance().getReference();
    private static final DatabaseReference
            databaseRef = FirebaseDatabase.getInstance().getReference();
    private static final FirebaseDB ourInstance = new FirebaseDB();

    private static DatabaseReference
            primaryUserRef,
            primaryUserPhotoRef;

    private static final String IMAGE_PREFIX = "images";
    private static final String USERS = "users";
    private static final String DELIMITER = "-";
    private static final String TAG = "FirebaseDB";
    final long FILE_SIZE = 1024 * 1024;

    private static List<Photo> friendsPhotos =  new LinkedList<>();
    private static List<ChildEventListener> listeners = new ArrayList<>();
    private static List<DatabaseReference> friendsRef = new ArrayList<>();
    public static FirebaseDB getInstance() {
        return ourInstance;
    }
    private final PhotoLoader loader = new PhotoLoader(Settings.getInstance().MAIN_LOCATION);
//TODO ADD THIS TO CURRENT USER TOO MAN
    public void addFriendsListeners(final IUser user){
        DatabaseReference temp = databaseRef.child(user.getUserId()).child("photos");
        friendsRef.add(temp);
        ChildEventListener childEventListener = new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildAdded:" + "New photo added");
                Photo tempPhoto = dataSnapshot.getValue(Photo.class);
                Log.i("FirebaseDB", "Photo: " + tempPhoto.getPathname());
                //downloadPhotoFromStorage(friend, tempPhoto);
                downloadPhotoFromStorage_file(user, tempPhoto);

            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildChanged:" + dataSnapshot.getKey());
                Photo tempPhoto = dataSnapshot.getValue(Photo.class);
                Log.i("FirebaseDB", "Photo: " + tempPhoto.getPathname());
                //This means karma could be changed, or location
                PhotoCollection.getInstance().updatePhotoFromStorage(tempPhoto);

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
                Log.d(TAG, "onChildRemoved:" + dataSnapshot.getKey());
                Photo tempPhoto = dataSnapshot.getValue(Photo.class);
                Log.i("FirebaseDB", "Photo removed: " + tempPhoto.getPathname());
                PhotoCollection.getInstance().release(tempPhoto);
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String previousChildName) {
                Log.d(TAG, "onChildMoved:" + dataSnapshot.getKey());


            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w(TAG, "postComments:onCancelled", databaseError.toException());
            }
        };
        temp.addChildEventListener(childEventListener);
        listeners.add(childEventListener);
    }

    @Override
    public List<Photo> downloadAllFriendsPhotos(IFriendList friends) {

        for (IUser u : friends.getFriends()) {
            Log.i("FirebaseDB", "Friends: " + u.getUserId());
            downloadPhotos(u);
            addFriendsListeners(u);
        }
        return friendsPhotos;

    }

    @Override
    public void addUser(IUser user) {
        primaryUserRef = databaseRef.child(user.getUserId());
        primaryUserRef.setValue(user.getEmail());
        primaryUserPhotoRef = primaryUserRef.child("photos");
    }

    @Override
    public void removeUser(IUser user) {

    }

    public void removeSharedPhotos(){
        primaryUserPhotoRef.removeValue();
    }
    @Override
    public void uploadPhotos(IUser user, List<Photo> photos) {
        int count = 0;
        for (Photo p: photos) {
            //DatabaseReference temp = primaryUserPhotoRef.child("photo" + count);
            DatabaseReference temp = primaryUserPhotoRef.push();
            temp.setValue(p);
            //temp.child("uid").setValue( p.getPathname() + "@" + primaryUserRef.getKey());

            count++;

            storePhotoToStorage(user, p);
        }
    }

    @Override
    public void downloadPhotos(final IUser friend) {

        Query query = databaseRef.child(friend.getUserId()).child("photos").orderByKey();

            query.addListenerForSingleValueEvent(new ValueEventListener() {

                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {

                    for (DataSnapshot dsp : dataSnapshot.getChildren()) {
                        Photo tempPhoto = dsp.getValue(Photo.class);
                        Log.i("FirebaseDB", "Photo: " + tempPhoto.getPathname());
                        friendsPhotos.add(tempPhoto);
                        //downloadPhotoFromStorage(friend, tempPhoto);
                        downloadPhotoFromStorage_file(friend, tempPhoto);
                    }
                }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                // nothing
            }
        });
    }

//    private void downloadPhotoFromStorage(IUser user, final Photo photo){
//        StorageReference temp = buildStoragePath(user, photo);
//        Log.i("FirebaseDB", temp.getPath());
//        temp.getBytes(FILE_SIZE).addOnSuccessListener(new OnSuccessListener<byte[]>() {
//            @Override
//            public void onSuccess(byte[] bytes) {
//                // Data for "images/island.jpg" is returns, use this as needed
//                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
//                loader.insertPhoto(bitmap, photo, "DejaPhotoFriends");
//                //
//            }
//        }).addOnFailureListener(new OnFailureListener() {
//            @Override
//            public void onFailure(@NonNull Exception exception) {
//                // Handle any errors
//            }
//        });
//
//    }

    private void downloadPhotoFromStorage_file(IUser user, final Photo photo){
        StorageReference temp = buildStoragePath(user, photo);

        final File localFile = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)+
                        File.separator+
                        "DejaPhotoFriends"+
                        File.separator+
                        photo.getUid()
        );
        temp.getFile(localFile).addOnSuccessListener(new OnSuccessListener<FileDownloadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(FileDownloadTask.TaskSnapshot taskSnapshot) {
                // Local temp file has been created
                Log.i("FirebaseDB", "Downloading file success: " + taskSnapshot.toString());
                photo.setPathname(localFile.getPath());
                PhotoCollection.getInstance().addPhoto(photo);
                FileUtilities.updateMediastore(localFile.getPath());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle any errors
            }
        });
    }

    public StorageReference storePhotoToStorage(IUser user, Photo photo){

        //Get the path to upload
        StorageReference ref = buildStoragePath(user, photo);
        // Get the data from an ImageView as bytes
        Bitmap bitmap = BitmapFactory.decodeFile(photo.getPathname());
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
        byte[] data = baos.toByteArray();

        UploadTask uploadTask = ref.putBytes(data);
        uploadTask.addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception exception) {
                // Handle unsuccessful uploads
                Log.w(TAG, "Upload: onFailure", exception);

            }
        }).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                // Upload succeeded
                Log.d(TAG, "Upload: onSuccess");
                // Handle successful uploads
            }
        });


        return ref;
    }

    public void deleteUserPhoto(final Photo photo){
        StorageReference ref = buildStoragePath(DJPrimaryUser.getInstance(), photo);
        ref.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.i("FirebaseDB", "Delete photo successful: " + photo.getUid());
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.i("FirebaseDB", "Delete photo unsuccessful: " + photo.getUid());

            }
        });
        Query ref2 = primaryUserPhotoRef.orderByChild("uid").equalTo(photo.getUid());
        ref2.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot dsp: dataSnapshot.getChildren()){
                    dataSnapshot.getRef().removeValue();
                }
                Log.i("FirebaseDB", "Delete photo successful (ref): " + photo.getUid());

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("FirebaseDB", "Delete photo unsuccessful (ref): " + photo.getUid());

            }
        });

    }

    public void updatePhotoKarma(final Photo photo){
        DatabaseReference ref = databaseRef.child(photo.getUid().substring(0, photo.getUid().indexOf('-')))
                .child("photos");

        Query ref2 = ref.orderByChild("uid").equalTo(photo.getUid());
        //ref2.getRef().setValue(photo.getKarma());
//        ref2.getRef().child("karma").runTransaction(new Transaction.Handler() {
//            @Override
//            public Transaction.Result doTransaction(MutableData mutableData) {
//                Photo s = mutableData.getValue(Photo.class);
//                if (s == null) {
//                    return Transaction.success(mutableData);
//                }
//
//                s.setKarma(photo.getKarma());
//                // Set value and report transaction success
//                mutableData.setValue(s);
//                return Transaction.success(mutableData);
//            }
//
//            @Override
//            public void onComplete(DatabaseError databaseError, boolean b,
//                                   DataSnapshot dataSnapshot) {
//                // Transaction completed
//                Log.d(TAG, "postTransaction:onComplete:" + databaseError);
//            }
//        });
        ref2.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                for(DataSnapshot dsp: dataSnapshot.getChildren()){
                    dsp.getRef().setValue(photo);
                    Log.i("FirebaseDB", "Ref: " + dsp.getRef());

                }
                Log.i("FirebaseDB", "Karma photo successful (ref): " + photo.getUid());

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.i("FirebaseDB", "Karma photo unsuccessful (ref): " + photo.getUid());

            }
        });

    }
    private StorageReference buildStoragePath(IUser user, Photo photo ){
        String str = user.getUserId() + DELIMITER + photo.getName();
        return storageRef.child(str);
    }

    public void removeFriendsListeners(){
        for(int i = 0; i < friendsRef.size(); i++){
            friendsRef.get(i).removeEventListener(listeners.get(i));
        }
    }
    @Override
    public DatabaseReference getPrimaryUserPhotoRef() {
        return primaryUserPhotoRef;
    }

    @Override
    public DatabaseReference getPrimaryUserRef() {
        return primaryUserRef;
    }
}
