package com.example.skumaresan2;


import android.util.Log;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class UploadFilesFlask {

    final String uploadServiceURL = "http://192.168.0.198:5000/upload";
    final String downloadServiceURL = "http://192.168.0.198:5000/return-files";

    MainActivity parentActity;
    private boolean downloadComplete = false;
    private boolean uploadComplete = false;

    UploadFilesFlask(MainActivity parentActity)
    {
        this.parentActity = parentActity;
    }
/*
 * mediaType  -  image/jpeg, image/*jpg, video/mp4, ...
 */

    public void sendFileToLocalServer(String filename, String mediaType)
    {
        sendFileToServer(filename, this.uploadServiceURL, mediaType);
    }
    public void sendFileToServer(String filename, String targetUploadServiceURL, String mediaType)
    {

        try {
            String filenameWithoutPath = filename.substring(filename.lastIndexOf("/")+1);
            Log.i("Upload","Prepare sending " + filename + " to server..."+ targetUploadServiceURL);

            FileInputStream fileInputStream = new FileInputStream(new File(filename));
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            byte[] buffer = new byte[1024];
            int read = 0;
            while ((read = fileInputStream.read(buffer, 0, buffer.length)) != -1) {
                baos.write(buffer, 0, read);
            }
            baos.flush();

            byte[] byteArray = baos.toByteArray();


            MultipartBody.Builder multipartBodyBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
            multipartBodyBuilder.addFormDataPart("file", filenameWithoutPath,
                    RequestBody.create(MediaType.parse(mediaType), byteArray));

            RequestBody postBodyFile = multipartBodyBuilder.build();

            postRequest(targetUploadServiceURL, postBodyFile);
        }
        catch (Exception e)
        {
            Toast.makeText(parentActity.getBaseContext(),"Failed to prepare sending " + filename + " to server..." + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    void postRequest( String postUrl, RequestBody postBody) {

        Toast.makeText(parentActity.getBaseContext(),"Sending file to server...", Toast.LENGTH_SHORT).show();

        OkHttpClient client = new OkHttpClient();

        Request request = new Request.Builder()
                .url(postUrl)
                .post(postBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                // Cancel the post on failure.
                call.cancel();
                //e.printStackTrace();
                parentActity.runOnUiThread(new Runnable() {
                    public void run() {
                        String errString = e.getMessage();
                        Toast.makeText(parentActity.getBaseContext(),"Connection Error: " + errString, Toast.LENGTH_LONG).show();
                        Log.e("Upload", errString);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {

                parentActity.runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String bodyString = response.body().string();
                            Toast.makeText(parentActity.getBaseContext(), bodyString, Toast.LENGTH_SHORT).show();
                            Log.i("Upload", bodyString);
                            uploadComplete=true;
                        } catch ( Exception e) {
                            // Don't know
                            e.printStackTrace();
                        }
                    }
                });

            }
        });
    }


    void getFileFromServer(String filename, String mediaType){
        getFileFromServer(filename, this.downloadServiceURL, mediaType);
    }

    void getFileFromServer(String filename, String targetUploadServiceURL, String mediaType){
        downloadComplete = false; //Flag to indicate if download has been completed.

        final String file = filename;
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder().url(targetUploadServiceURL).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, final IOException e) {
                Log.d("Download", "Download onFailure()");
                // Cancel the post on failure.
                call.cancel();
                //Stop the timer in MainActivity that keeps polling to check if download was completed successfully
                parentActity.stopTimer2();
                parentActity.timer2 = null;
                parentActity.enableDownloadButton();
                //e.printStackTrace();
                parentActity.runOnUiThread(new Runnable() {
                    public void run() {
                        String errString = e.getMessage();
                        Toast.makeText(parentActity.getBaseContext(),"Connection Error wile downloading: " + errString, Toast.LENGTH_LONG).show();
                        Log.e("Download", errString);
                    }
                });
            }

            @Override
            public void onResponse(Call call, final Response response)  {
                try {
                    InputStream is = response.body().byteStream();

                    BufferedInputStream input = new BufferedInputStream(is);
                    OutputStream output = new FileOutputStream(file);

                    byte[] data = new byte[1024];
                    long total = 0; int count;

                    while ((count = input.read(data)) != -1) {
                        Log.d("Downloading","Reading inputStream: "+count+" bytes.");
                        total += count;
                        output.write(data, 0, count);
                    }
                    output.flush();
                    output.close();
                    input.close();

                    downloadComplete = true;
                    Log.d("Download", "Download complete... attempting to toast success message");

                    parentActity.runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                Toast.makeText(parentActity.getBaseContext(), "Download Successful! ", Toast.LENGTH_LONG).show();

                            } catch ( Exception e) {
                                // Don't know
                                e.printStackTrace();
                            }
                        }
                    });

                }catch (java.net.ProtocolException e){

                }
                catch (IOException e) {
                    Log.d("Download",e.toString());

                    parentActity.runOnUiThread(new Runnable() {
                        public void run() {
                            try {
                                Toast.makeText(parentActity.getBaseContext(), "Error downloading!", Toast.LENGTH_LONG).show();
                            } catch ( Exception e) {
                                // Don't know
                                e.printStackTrace();
                            }
                        }
                    });

                } finally{
                    //Wait for 1 second (to ensure MainActivity realizes that the DB download has completed successfully
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    parentActity.runOnUiThread(new Runnable() {
                        public void run() {
                            parentActity.stopTimer2();
                            parentActity.timer2 = null;
                            parentActity.enableDownloadButton();
                        }
                    });

                }
            }
        });
    }

    public boolean getDownloadStatus(){
        return downloadComplete;
    }

    public boolean getUploadStatus(){
        return uploadComplete;
    }

    public void setDownloadStatus(boolean status){
        downloadComplete = status;
    }

}
