//Vishnu...Thank you for electronics.

package com.appwithmkmishra2000.mic_client_prototype01;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.MediaRecorder.AudioSource;
import android.media.audiofx.NoiseSuppressor;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.UUID;

import static android.media.AudioFormat.CHANNEL_IN_MONO;

public class MainActivity<myRecorder, outputFile> extends AppCompatActivity {

    //all buttons.
    Button streamingButton;
    Button stoppingButton;
    Button playingButton;
    Button recordingButton;
    Button stopRecording;
    Button playOnSpeaker;

    MediaRecorder mediarecorder;
    MediaPlayer mediaPlayer;

    final int REQUEST_PERMISSION_CODE = 1000;


    EditText ipAddress;
    EditText sampleRate;

    TextView terminal;
    private String outputFile = null;

    //check the Button
    Boolean status= false;

    String ipAdd = "";
    String pathSave = "";

    int sampleRateInHz;
    int channelConfig = AudioFormat.CHANNEL_IN_MONO;
    int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
    int minBufSize;
    int port = 50005;

    AudioRecord recorder;


    public void startStreaming(View view){
        Log.i("Button", "Live streaming");
        streamingButton.setEnabled(false);
        stoppingButton.setEnabled(true);
        recordingButton.setEnabled(false);
        status = true;

        Thread streamThread = new Thread(new Runnable() {

            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN)
            @Override
            public void run() {
                try{
                    sampleRateInHz = Integer.parseInt(sampleRate.getText().toString());

                    //minimum size of buffer.
                    minBufSize = AudioRecord.getMinBufferSize(sampleRateInHz, channelConfig, audioFormat);

                    ipAddress   = (EditText)findViewById(R.id.ipAddress);
                    String ipAdd =ipAddress.getText().toString();


                    DatagramSocket socket = new DatagramSocket();
                    Log.d("VS", "Socket Created");

                    byte[] buffer = new byte[minBufSize];

                    Log.d("VS","Buffer created of size " + minBufSize);
                    DatagramPacket packet;

                    String bufferSize = String.valueOf(minBufSize);
                    terminal = (TextView)findViewById(R.id.terminal);
                    terminal.setSelected(true);
                    terminal.append(bufferSize+"\n");

                    final InetAddress destination = InetAddress.getByName(ipAdd);
                    Log.d("VS", "Address retrieved");

                    try {
                        recorder = new AudioRecord(MediaRecorder.AudioSource.VOICE_RECOGNITION, sampleRateInHz, channelConfig, audioFormat, minBufSize * 10);
                        Log.d("VS", "Recorder initialized");



                        recorder.startRecording();
                    }catch (Exception e){
                        e.printStackTrace();
                    }



                    while(status == true) {


                        //reading data from MIC into buffer
                        minBufSize = recorder.read(buffer, 0, buffer.length);



                        //putting buffer in the packet
                        packet = new DatagramPacket (buffer,buffer.length,destination,port);

                        socket.send(packet);
                        System.out.println("MinBufferSize: " +minBufSize);


                    }



                } catch(UnknownHostException e) {
                        Log.e("VS", "UnknownHostException");
                } catch (IOException e) {
                        e.printStackTrace();
                        Log.e("VS", "IOException");
                }

            }
        });
        streamThread.start();
    }




    public void stopStreaming(View view){
        Log.i("Button", "Stop");
        streamingButton.setEnabled(true);
        recordingButton.setEnabled(true);
        stoppingButton.setEnabled(false);
        playingButton.setEnabled(false);

        status = false;
        if(recorder!=null){
            recorder.stop();
            recorder.release();
            Log.d("VS", "Recorder released");

            Toast.makeText(getApplicationContext(), "Recorder released...", Toast.LENGTH_SHORT).show();
        }
        if(mediaPlayer!=null){
            mediaPlayer.stop();
            mediaPlayer.release();
            setupMediaRecorder();
        }

    }



    private void setupMediaRecorder() {
        mediarecorder = new MediaRecorder();
        mediarecorder.setAudioSource(AudioSource.VOICE_COMMUNICATION);
        mediarecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        mediarecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
        mediarecorder.setOutputFile(pathSave);
    }


    public void playButton(View view){
        Log.i("Button", "Play");
        streamingButton.setEnabled(true);
        stoppingButton.setEnabled(false);
        playingButton.setEnabled(false);
        recordingButton.setEnabled(true);

        mediaPlayer = new MediaPlayer();

        try {
            mediaPlayer.setDataSource(pathSave);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }




    public void recordingStart(View view){
        Log.i("Button", "Record");

        if(checkPermissionFromDevice()){

            streamingButton.setEnabled(false);
            stoppingButton.setEnabled(false);
            playingButton.setEnabled(false);
            recordingButton.setEnabled(false);
            stopRecording.setEnabled(true);
            playOnSpeaker.setEnabled(false);

            pathSave = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+"_audio_store.3gp";
            setupMediaRecorder();
            try {
                mediarecorder.prepare();
                mediarecorder.start();

            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(),"Recording...", Toast.LENGTH_LONG).show();

        }else{
            requestPermission();
        }
    }


    public void playOnSpeaker(View view){

        streamingButton.setEnabled(false);
        stoppingButton.setEnabled(true);
        playingButton.setEnabled(true);
        recordingButton.setEnabled(false);
        stopRecording.setEnabled(false);
        playOnSpeaker.setEnabled(false);


        pathSave = Environment.getExternalStorageDirectory().getAbsolutePath()+"/"+"_audio_store.3gp";
        File myAudioFile  = new File(pathSave);
        DatagramSocket ds = null;
        BufferedInputStream bis = null;
        try{
            ds = new DatagramSocket();
            DatagramPacket packets;
            int packetSize = 3584;
            double noOfPackets;
            noOfPackets = Math.ceil(((int) myAudioFile.length())/packetSize);

            bis = new BufferedInputStream(new FileInputStream(myAudioFile));
            final InetAddress destination = InetAddress.getByName(ipAdd);
            Log.d("VS", "Address retrieved");


            for(double i =0; i<noOfPackets+1; i++){
                byte[] myByteArray = new byte[packetSize];
                bis.read(myByteArray,0,myByteArray.length);
                Log.i("VS", "stream formed...");
                packets = new DatagramPacket(myByteArray,myByteArray.length, destination, port);
                try {
                    ds.send(packets);
                    Log.i("VS", "packet send...");
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
            if(bis !=null)
                Log.i("byte stream", "stream failed!!!");
                bis.close();
            if(ds!=null){
                ds.close();

            }

        } catch (SocketException | FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    public void stopRecordingButtonPress(View view){
        Log.i("Button", "Stop Recording");

        streamingButton.setEnabled(false);
        stoppingButton.setEnabled(false);
        playingButton.setEnabled(true);
        recordingButton.setEnabled(true);

        mediarecorder.stop();
        stopRecording.setEnabled(false);
        playOnSpeaker.setEnabled(true);

        Toast.makeText(getApplicationContext(), "Recording stop...", Toast.LENGTH_LONG).show();
    }



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        streamingButton = (Button) findViewById(R.id.StreamButton);
        stoppingButton  = (Button)findViewById(R.id.stopButton);
        playingButton   = (Button)findViewById(R.id.playButton);
        recordingButton = (Button)findViewById(R.id.recordButton);
        stopRecording = (Button) findViewById(R.id.stopRecordingButton);
        playOnSpeaker = findViewById(R.id.playLive);

        ipAddress = (EditText) findViewById(R.id.ipAddress);
        sampleRate =(EditText) findViewById(R.id.sampleRate);

        terminal = findViewById(R.id.terminal);

        streamingButton.setEnabled(true);
        stoppingButton.setEnabled(false);
        playingButton.setEnabled(false);
        recordingButton.setEnabled(true);
        stopRecording.setEnabled(false);
        playOnSpeaker.setEnabled(false);

        Toast.makeText(getApplicationContext(),"Put ip address",Toast.LENGTH_LONG).show();

        ipAdd = ipAddress.getText().toString();

        Log.i("ipadd","There is a Ip Address");

        if(!checkPermissionFromDevice()){
            requestPermission();
        }
    }


    private void requestPermission() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.RECORD_AUDIO
        }, REQUEST_PERMISSION_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode){
            case REQUEST_PERMISSION_CODE:
            {
                if(grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
                    Toast.makeText(this, "Permission Granted",Toast.LENGTH_LONG).show();
                else
                    Toast.makeText(this, "Permission Denied",Toast.LENGTH_SHORT).show();
            }
            break;
        }
    }


    private boolean checkPermissionFromDevice() {
        int write_external_storage_result = ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int record_audio_result = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);

        return write_external_storage_result== PackageManager.PERMISSION_GRANTED &&
                record_audio_result == PackageManager.PERMISSION_GRANTED;

    }

}
