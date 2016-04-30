package warrick.simpleimagepicker;

import android.content.Context;
import android.content.pm.PackageManager;
import android.support.v4.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

class PermissionHelper {
    /**
     * Returns the permissions that still need granted out of all the required permissions
     * @param context
     * @param requiredPermissions
     * @return
     */
    public static String[] getPermissionsToRequest(Context context, String[] requiredPermissions) {
        List<String> permissionsToRequest = new ArrayList<>();

        for(String p : requiredPermissions) {
            if(ContextCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(p);
        }

        return permissionsToRequest.toArray(new String[permissionsToRequest.size()]);
    }
}
