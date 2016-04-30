# Android pick or take image
Library to make it easier to pick or take images inside an android app.
This simple android module makes it easy to provide means for users in your app to pick an existing photo, or take a new photo and handles requesting user permissions for taking photos.

## Installation
You will need to download the code, and add the module as a dependancy in android studio.

You should then create an instance of `ImagePickerHelper` as a member variable of the activity that you will be picking an image from. For example, in `onCreate`:

`private ImagePickerHelper imagePickerHelper;`

You must then forward a few activity callbacks to this variable. In your activity, make sure you are overriding `onRequestPermissionsResult` and `onActivityResult`.
You must then call these methods in the ImagePickerHelper instance you created as a member variable in your activity.

Here is an example implementation of onRequestpermissionsResult that should be in your activity
```
@Override
public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    // The following line is where the magic happens:
    imagePickerHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
}
```

## How to use
Call `ImagePickerHelper.pickImage` on the instance of `ImagePickerHelper` that you created in `onCreate`, passing in a `ImagePickerHelper.PickImageResult` callback which will be used to deliever the results back to your activity. For example:
```
imagePickerHelper.pickImage(new ImagePickerHelper.PickImageResult() {
    @Override
    public void cancelled() {
        // Called when the user canceled somewhere, and didn't end up picking an image
    }

    @Override
    public void imageSelected(@NonNull Uri filePath) {
        // Called when the user has picked an image. The Uri points to the selected image.
        // You could load the image into an image view with the following line:
        //imageView.setImageURI(filePath);
    }
});
```

Here is an example implementation of onActivityResult:
```
@Override
protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    // Let the image picker helper handle any activity results
    imagePickerHelper.onActivityResult(requestCode, resultCode, data)
    super.onActivityResult(requestCode, resultCode, data);
}
```

## What this will make simple...
Many apps need to allow the user to take or pick images, and this is made even more complicated by the fact that with Android 6.0 there are permissions that need to be explicitaly accepted by the user. This library makes it easy to retreive images from the user.

## Need to knows
The `ImagePickerHelper` will be calling `startActivityForResult` with request codes starting at 1000. Make sure you are not using 1000 to 1010 as activity request code in your app. The same applies for the request codes for requesting permissions.

## Screenshots
Requesting user permissions

![stack Overflow](https://raw.githubusercontent.com/awarlt/android-pick-image/master/docs/images/request-permissions.png)

Explaining to the user why the permissions are required (only shown if they don't accept the permissions, since it's obvious what they are for in context)

![Explain requesting permissions](https://raw.githubusercontent.com/awarlt/android-pick-image/master/docs/images/permissions-request-dialog.png)

Letting the user pick whether they want to take a photo, or pick an existing photo. If the user hasn't granted the camera and storage permissions, only the pick option will be available.

![Pick or take](https://raw.githubusercontent.com/awarlt/android-pick-image/master/docs/images/picker-intent.png)
