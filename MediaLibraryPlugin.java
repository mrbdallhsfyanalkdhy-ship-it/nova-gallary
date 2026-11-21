package com.novagallery.app;

import android.Manifest;
import android.content.ContentUris;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.provider.Settings;
import androidx.core.content.ContextCompat;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import com.getcapacitor.annotation.PermissionCallback;

@CapacitorPlugin(
    name = "MediaLibrary",
    permissions = {
        @Permission(
            alias = "media",
            strings = {
                Manifest.permission.READ_MEDIA_IMAGES,
                Manifest.permission.READ_MEDIA_VIDEO,
                Manifest.permission.READ_EXTERNAL_STORAGE
            }
        )
    }
)
public class MediaLibraryPlugin extends Plugin {

    private boolean hasMediaPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_IMAGES) == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_MEDIA_VIDEO) == PackageManager.PERMISSION_GRANTED;
        }
        return ContextCompat.checkSelfPermission(getContext(), Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
    }

    @PluginMethod
    public void checkPermissions(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("media", hasMediaPermission() ? "granted" : "prompt");
        call.resolve(ret);
    }

    @PluginMethod
    public void requestPermissions(PluginCall call) {
        if (hasMediaPermission()) {
            JSObject ret = new JSObject();
            ret.put("media", "granted");
            call.resolve(ret);
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requestPermissionForAliases(new String[]{"media"}, call, "mediaPermCallback");
        } else {
            requestPermissionForAlias("media", call, "mediaPermCallback");
        }
    }

    @PermissionCallback
    private void mediaPermCallback(PluginCall call) {
        JSObject ret = new JSObject();
        ret.put("media", hasMediaPermission() ? "granted" : "denied");
        call.resolve(ret);
    }

    @PluginMethod
    public void openAppSettings(PluginCall call) {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getContext().getPackageName(), null);
        intent.setData(uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        getContext().startActivity(intent);
        JSObject ret = new JSObject();
        ret.put("opened", true);
        call.resolve(ret);
    }

    @PluginMethod
    public void getAllMedia(PluginCall call) {
        if (!hasMediaPermission()) {
            call.reject("Media permission not granted");
            return;
        }
        int limit = call.getInt("limit", 1500);
        JSArray items = new JSArray();
        queryCollection(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image", limit / 2, items);
        queryCollection(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, "video", limit / 2, items);
        JSObject ret = new JSObject();
        ret.put("items", items);
        call.resolve(ret);
    }

    private void queryCollection(Uri collection, String type, int maxItems, JSArray out) {
        String[] projection = new String[]{
            MediaStore.MediaColumns._ID,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.MediaColumns.DATE_MODIFIED,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            MediaStore.MediaColumns.DATA
        };
        String sortOrder = MediaStore.MediaColumns.DATE_MODIFIED + " DESC";
        try (Cursor cursor = getContext().getContentResolver().query(collection, projection, null, null, sortOrder)) {
            if (cursor == null) return;
            int idCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns._ID);
            int nameCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DISPLAY_NAME);
            int mimeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE);
            int sizeCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.SIZE);
            int wCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH);
            int hCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT);
            int dateCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATE_MODIFIED);
            int bucketCol = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME);
            int dataCol = cursor.getColumnIndex(MediaStore.MediaColumns.DATA);

            int count = 0;
            while (cursor.moveToNext() && count < maxItems) {
                long id = cursor.getLong(idCol);
                Uri contentUri = ContentUris.withAppendedId(collection, id);
                String filePath = dataCol >= 0 ? cursor.getString(dataCol) : null;
                JSObject obj = new JSObject();
                obj.put("id", type + "_" + id);
                obj.put("name", cursor.getString(nameCol));
                obj.put("uri", contentUri.toString());
                if (filePath != null) {
                    obj.put("path", filePath);
                }
                obj.put("mimeType", cursor.getString(mimeCol));
                obj.put("size", cursor.getLong(sizeCol));
                obj.put("width", cursor.getInt(wCol));
                obj.put("height", cursor.getInt(hCol));
                obj.put("createdAt", cursor.getLong(dateCol) * 1000L);
                obj.put("folder", cursor.getString(bucketCol));
                obj.put("type", type);
                out.put(obj);
                count++;
            }
        }
    }
}
