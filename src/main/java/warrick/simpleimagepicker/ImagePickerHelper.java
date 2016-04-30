package warrick.simpleimagepicker;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.Pair;
import android.support.v7.app.AlertDialog;

import com.warrick.simpleimagepicker.R;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Class for picking an image
 */
public class ImagePickerHelper {
    private static final int REQUEST_CODE_BASE = 1000;

    private Activity activity;

    /**
     * Creates a new Image picker helper for the specified activity
     * @param activity
     */
    public ImagePickerHelper(Activity activity) {
        this.activity = activity;
    }

    private HashMap<Integer, Pair<PickImageResult, Uri>> pickRequests = new HashMap<>();

    private HashMap<Integer, PickImageResult> permissionRequests = new HashMap<>();

    /**
     * Returns true if the user has granted camera permissions, which allows this app to take photos
     * @return
     */
    public boolean hasPhotoPermmissions() {
        return ContextCompat.checkSelfPermission(activity, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Pick an image, with the results being delievered in the resultCallback
     */
    private void startPickIntent(@NonNull PickImageResult resultCallback) {
        // Get a new unused request code to use for this image request
        int requestCode = REQUEST_CODE_BASE + pickRequests.size();
        // Assert that the request code has not been used yet
        assert !pickRequests.containsKey(requestCode);

        // Create an intent to pick an existing photo
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");

        // Check that the user has permissions to take a photo
        boolean canTakePhoto = hasPhotoPermmissions();

        // This is the uri that an external camera will save to
        Uri takePhotoFileUri = null;

        // If the user has permissions to take a photo, create a chooser intent
        if(canTakePhoto) {
            File directory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            // Make sure the directory exists
            directory.mkdirs();

            takePhotoFileUri = Uri.fromFile(new File(directory, UUID.randomUUID().toString() + ".jpg"));

            // Create an intent to take a new photo with the camera
            Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, takePhotoFileUri);

            // Create the chooser intent
            String pickTitle = "Select or take a new picture";
            Intent chooserIntent = Intent.createChooser(photoPickerIntent, pickTitle);

            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS,
                    new Intent[]{takePhotoIntent});

            // Start the chooser intent
            activity.startActivityForResult(chooserIntent, requestCode);
        }
        else {
            // The user hasn't granted permission to take a photo. Only allow them to select an image with the image
            // chooser.
            activity.startActivityForResult(photoPickerIntent, requestCode);
        }

        // Add the image request to the list of requests
        pickRequests.put(requestCode, new Pair<>(resultCallback, takePhotoFileUri));
    }

    /**
     * This must be called to
     * @param requestCode
     * @param resultCode
     * @param data
     */
    public boolean onActivityResult(int requestCode, int resultCode, Intent data) {
        for(Map.Entry<Integer, Pair<PickImageResult, Uri>> entry : pickRequests.entrySet()) {
            if(entry.getKey().equals(requestCode)) {
                handleImageResult(entry.getValue(), resultCode, data);

                // Remove the request since it has been handled
                pickRequests.remove(entry.getKey());

                return true;
            }
        }
        return false;
    }

    private void handleImageResult(Pair<PickImageResult, Uri> resultCallback, int resultCode, Intent data) {
        if(resultCode == Activity.RESULT_OK) {
            if(data.getData() != null) {
                resultCallback.first.imageSelected(data.getData());
            }
            else if(resultCallback.second != null) {
                resultCallback.first.imageSelected(resultCallback.second);
            }
            else {
                // Failed to get the data
                resultCallback.first.cancelled();
            }
        }
        else {
            resultCallback.first.cancelled();
        }
    }

    /**
     * This must be called from the activity
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(permissionRequests.containsKey(requestCode)) {
            final PickImageResult callback = permissionRequests.get(requestCode);
            permissionRequests.remove(requestCode);

            List<String> permissionsList = Arrays.asList(permissions);

            // If the user has granted the camera permission, then carry on to selecting a cover photo
            if (permissionsList.contains(Manifest.permission.CAMERA) || permissionsList.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                if (hasPhotoPermmissions()) {
                    // This will now let the user select a photo, since they have granted the camrea permission
                    beginPickImageFlow(false, callback);
                } else {
                    // Warn the user that they will be unable to take a photo without the camera permission
                    AlertDialog.Builder alertDialog = new AlertDialog.Builder(activity);
                    alertDialog.setTitle(R.string.request_permission_dialog_title);
                    alertDialog.setMessage(R.string.request_permission_dialog_message);
                    alertDialog.setPositiveButton(R.string.request_permission_dialog_continue, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Attempt to select a cover photo, and ignore any missing permissions since the user
                            // has been warned.
                            beginPickImageFlow(true, callback);
                        }
                    });
                    alertDialog.setNeutralButton(R.string.request_permission_dialog_tryagain, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Let the user accept the permission again
                            beginPickImageFlow(false, callback);
                        }
                    });
                    alertDialog.show();
                }
            }
        }
    }

    /**
     * Begins the flow to pick an image, or take a photo
     * @param resultCallback
     */
    public void pickImage(PickImageResult resultCallback){
        beginPickImageFlow(false, resultCallback);
    }

    /**
     * This starts the process of letting the user change the cover photo
     * @param ignoreCameraPermission
     */
    private void beginPickImageFlow(boolean ignoreCameraPermission, PickImageResult resultCallback) {
        // If the sdk version is above 23, make sure the user has camera permissions - unless the user has already ignored this.
        if(Build.VERSION.SDK_INT >= 23 && !ignoreCameraPermission && !hasPhotoPermmissions()) {
            // Since this request has been delayed for the permissions, add it to the hash map so we still have the
            // callback when after we actually pick the image.
            int requestCode = REQUEST_CODE_BASE + permissionRequests.size();
            permissionRequests.put(requestCode, resultCallback);
            activity.requestPermissions(PermissionHelper.getPermissionsToRequest(activity, new String[] { Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE }), requestCode);
        }
        else {
            startPickIntent(resultCallback);
        }
    }

    /**
     * Interface to define a callback when an image is selected, or failed to be selected
     */
    public interface PickImageResult {
        /**
         * Called when the user did not pick an image
         */
        void cancelled();

        /**
         * Called when the user picked or took a new photo
         * @param filePath The Uri to the piced (or new photo)
         */
        void imageSelected(@NonNull Uri filePath);
    }
}
