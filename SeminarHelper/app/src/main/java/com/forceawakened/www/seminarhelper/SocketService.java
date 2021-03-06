package com.forceawakened.www.seminarhelper;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;

/**
 * Created by forceawakened on 24/03/17.
 */

public class SocketService extends Service {
    private String filename1 = "home.txt";
    private String filename2 = "query.txt";
    private String filename3 = "filelist.txt";
    private String filename4 = "announce.txt";
    private Socket socket;
    public Integer countMsg = 0, countAnn = 0;
    OutputStream out1, out2, out3, out4;

    @Override
    public IBinder onBind(Intent intent) {
        return myBinder;
    }

    private final IBinder myBinder = new LocalBinder() ;

    public class LocalBinder extends Binder {
        public SocketService getService() {
            return SocketService.this;
        }
    }

    public void IsBoundable(){
        Toast.makeText(this,"I bind like butter", Toast.LENGTH_LONG).show();
    }

    public void sendMessage(String msg){
        try {
            out1.write(msg.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendQuery(String msg){
        try {
            out2.write(msg.getBytes());
            displayNotification(2);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFile(String msg){
        try {
            out3.write(msg.getBytes());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendAnnounce(String msg){
        try {
            out4.write(msg.getBytes());
            displayNotification(4);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void displayNotification(int flag){
        if(flag == 2) {
            if(countMsg <= 0){
                return;
            }
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    countMsg = 0;
                    unregisterReceiver(this);
                }
            };
            String delete_msg = "DELETE_MESSAGE";
            Intent intent = new Intent(delete_msg);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent, 0);
            registerReceiver(receiver, new IntentFilter(delete_msg));
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Seminar Helper")
                    .setContentText("You have " + countMsg + " unread messages.")
                    .setDeleteIntent(pendingIntent);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(0, builder.build());
        }
        else{
            if(countAnn <= 0){
                return;
            }
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    countAnn = 0;
                    unregisterReceiver(this);
                }
            };
            String delete_notif = "DELETE_NOTIFICATION";
            Intent intent = new Intent(delete_notif);
            PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0);
            registerReceiver(receiver, new IntentFilter(delete_notif));
            NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Seminar Helper")
                    .setContentText("You have missed " + countAnn + " notifications.")
                    .setDeleteIntent(pendingIntent);
            NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(1, builder.build());
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Runnable connect = new connectSocket();
        System.out.println("The service thread is about to start.") ;
        new Thread(connect).start();
        return START_STICKY;
    }

    class connectSocket implements Runnable {
        @Override
        public void run() {
            System.out.println("Service thread created!") ;
            try {
                socket = SocketHandler.getSocket();
                System.out.println("Socket: " + String.valueOf(socket));
                try{
                    out1 = openFileOutput(filename1, Context.MODE_APPEND);
                    out2 = openFileOutput(filename2, Context.MODE_APPEND);
                    out3 = openFileOutput(filename3, Context.MODE_APPEND);
                    out4 = openFileOutput(filename4, Context.MODE_APPEND);
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String text, key, value;
                    while(true) {
                        text = in.readLine();
                        System.out.println(text);
                        if(text.length() > 5) {
                            key = text.substring(0, 4);
                            value = text.substring(5);
                            value += "\n";
                            if ("EXIT".equals(key)) {
                                break;
                            } else if ("TEXT".equals(key)) {
                                sendMessage(value);
                            } else if ("QUER".equals(key)) {
                                sendQuery(value);
                                ++countMsg;
                            } else if ("FILE".equals(key)) {
                                sendFile(value);
                            } else if ("ANNO".equals(key)) {
                                sendAnnounce(value);
                                ++countAnn;
                            }
                        }
                    }
                    out1.close();
                    out2.close();
                    out3.close();
                    out4.close();
                }
                catch (Exception e) {
                    System.out.println("Service: error while reading/writing socket.");
                    e.printStackTrace();
                }
            }
            catch (Exception e ) {
                System.out.println("Service: error in service thread.");
                e.printStackTrace();
            }
        }
    }
}