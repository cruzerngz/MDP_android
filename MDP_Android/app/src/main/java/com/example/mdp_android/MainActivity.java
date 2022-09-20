package com.example.mdp_android;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.mdp_android.databinding.ActivityMainBinding;

import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;


import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Handler;
import android.view.Gravity;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.ToggleButton;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.nio.charset.Charset;
import java.util.UUID;

public class MainActivity extends AppCompatActivity implements IAppendMessages {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;
    // Declare BTManager class
    BTManager btManager;
    TextView receiveMsgTextView;
    EditText sendMsgEditText;

    public static Context ctx;


//    // UI PART
    private static final String TAG = "Main Activity";

    private static SharedPreferences sharedPreferences;
    private static SharedPreferences.Editor editor;
    private static Context context;

    private static GridMap gridMap;

    BluetoothDevice mBTDevice;
    private static UUID myUUID;

    static TextView robotXTextView, robotYTextView, robotBearingTextView;
    static TextView bluetoothStatusLabel, bluetoothStatusTextView, robotStatusTextView;

    static Button moveForwardButton, moveBackButton, turnLeftButton, turnRightButton;

    static Button addObstacleButton, resetMapButton;
    static Spinner imageIDSpinner, obstacleBearingSpinner;
    static Switch editObstacleSwitch, dragObstacleSwitch;
    static String imageID; //for spinner
    static String obstacleBearing; //for spinner

    static ToggleButton startPointToggle;

    public static ToggleButton imageRecognitionToggle, fastestCarToggle;
    static Button resetImageRecognitionButton, resetFastestCarButton;
    static TextView imageRecognitionTimeTextView, fastestCarTimeTextView;
    private static long imageRecognitionTimer, fastestCarTimer;

    private static TextView receiveMessageTextBox;
    private EditText sendMessageTextBox;
    Button sendMessageButton;

    private static boolean autoUpdate = false;
    static boolean dragObstacleFlag;
    static boolean editObstacleFlag;
    public static boolean stopImageRecognitionTimerFlag = false;
    public static boolean stopFastestCarTimerFlag = false;

    String obstacleIDCommand = "";
    String imageIDCommand = "";

    public static Handler timerHandler = new Handler();

    public Runnable imageRecognitionTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long msImageRecognition = System.currentTimeMillis() - imageRecognitionTimer;
            int secImageRecognition = (int) (msImageRecognition / 1000);
            int minImageRecognition = secImageRecognition / 60;
            secImageRecognition = secImageRecognition % 60;

            if (stopImageRecognitionTimerFlag == false) {
                imageRecognitionTimeTextView.setText(String.format("%02d:%02d", minImageRecognition, secImageRecognition));
                timerHandler.postDelayed(this, 500);
            }
        }
    };

    public Runnable fastestCarTimerRunnable = new Runnable() {
        @Override
        public void run() {
            long msFastestCar = System.currentTimeMillis() - fastestCarTimer;
            int secFastestCar = (int) (msFastestCar / 1000);
            int minFastestCar = secFastestCar / 60;
            secFastestCar = secFastestCar % 60;

            if (stopFastestCarTimerFlag == false) {
                fastestCarTimeTextView.setText(String.format("%02d:%02d", minFastestCar, secFastestCar));
                timerHandler.postDelayed(this, 500);
            }
        }
    };
    ////////////////end of UI PART //////////////////////////////

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ctx = this;
        btManager = new BTManager(this, this);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        receiveMsgTextView = findViewById(R.id.receiveMsgTextView);
        receiveMsgTextView.setMovementMethod(new ScrollingMovementMethod());
        sendMsgEditText = findViewById(R.id.sendMsgEditText);






        // UI PART
        LocalBroadcastManager.getInstance(this).registerReceiver(messageReceiver, new IntentFilter("incomingMessage"));

        //Setting up sharedPreferences
        context = getApplicationContext();
        this.sharedPreferences();
        editor.putString("message", "");
        editor.putString("direction", "None");
        editor.putString("connStatus", "Disconnected");
        editor.commit();

        //Initialising map
        gridMap = new GridMap(this);
        gridMap = findViewById(R.id.mapView);

        //Initialising bluetooth segment
//        bluetoothStatusLabel = findViewById(R.id.bluetoothStatusLabel);
//        bluetoothStatusTextView = findViewById(R.id.bluetoothStatusTextView);

        // Robot status
        robotStatusTextView = findViewById(R.id.robotStatusTextView);

        //Initialising robot axis and bearing
        robotXTextView = findViewById(R.id.robotXTextView);
        robotYTextView = findViewById(R.id.robotYTextView);
        robotBearingTextView = findViewById(R.id.robotBearingTextView);

        //Initialising robot movement buttons
        moveForwardButton = findViewById(R.id.upButton);
        moveBackButton = findViewById(R.id.downButton);
        turnLeftButton = findViewById(R.id.leftButton);
        turnRightButton = findViewById(R.id.rightButton);

        //Initialising obstacle segment
        addObstacleButton = findViewById(R.id.addObstacleButton);
        dragObstacleSwitch = findViewById(R.id.dragObstacleSwitch);
        editObstacleSwitch = findViewById(R.id.editObstacleSwitch);
        imageIDSpinner = findViewById(R.id.imageIDSpinner);
        obstacleBearingSpinner = findViewById(R.id.obstacleBearingSpinner);
        imageIDSpinner.setEnabled(false);
        obstacleBearingSpinner.setEnabled(false);

        //Initialising the array of imageID list and obstacleBearing list
        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                gridMap.IMAGE_ID_LIST.get(i)[j] = "";
                gridMap.OBSTACLE_BEARING_LIST.get(i)[j] = "";
            }
        }

        resetMapButton = findViewById(R.id.resetMapButton);
        startPointToggle = findViewById(R.id.startPointToggle);

        //Initialising IR and FC mode segment
        imageRecognitionToggle = findViewById(R.id.imageRecognitionToggle);
        fastestCarToggle = findViewById(R.id.fastestCarToggle);
        imageRecognitionTimeTextView = findViewById(R.id.imageRecognitionTimeTextView);
        fastestCarTimeTextView = findViewById(R.id.fastestCarTimeTextView);
        resetImageRecognitionButton = findViewById(R.id.resetImageRecognitionButton);
        resetFastestCarButton = findViewById(R.id.resetFastestCarButton);
        imageRecognitionTimer = 0;
        fastestCarTimer = 0;

        //Initialising the communication segment


//        receiveMessageTextBox = findViewById(R.id.receiveMessageTextBox);
//        sendMessageTextBox = findViewById(R.id.sendMessageTextBox);
//        sendMessageButton = findViewById(R.id.sendMessageButton);
//
//        receiveMessageTextBox.setMovementMethod(new ScrollingMovementMethod());

        //Creating the layout for the spinner
        ArrayAdapter<CharSequence> imageIDAdapter = ArrayAdapter.createFromResource(context, R.array.imageID_Array, android.R.layout.simple_spinner_item);
        imageIDAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        imageIDSpinner.setAdapter(imageIDAdapter);

        ArrayAdapter<CharSequence> obstacleBearingAdapter = ArrayAdapter.createFromResource(context, R.array.obstacleBearing_Array, android.R.layout.simple_spinner_item);
        obstacleBearingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        obstacleBearingSpinner.setAdapter(obstacleBearingAdapter);

        imageIDSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> a, View v, int pos, long arg3) {
                imageID = a.getItemAtPosition(pos).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> a) { }
        });

        obstacleBearingSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> a, View v, int pos, long arg3) {
                obstacleBearing = a.getItemAtPosition(pos).toString();
            }
            @Override
            public void onNothingSelected(AdapterView<?> a) { }
        });

        resetMapButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked resetMapButton");
                showToast("Resetting map...");
                gridMap.resetMap();
            }
        });

        dragObstacleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                showToast("Dragging obstacle switch is " + (isChecked ? "on" : "off"));
                dragObstacleFlag = isChecked;

                if (dragObstacleFlag == true) {
                    imageIDSpinner.setEnabled(false);
                    obstacleBearingSpinner.setEnabled(false);
                    gridMap.setObstacleFlag(false);
                    editObstacleSwitch.setChecked(false);
                }
            }
        });

        editObstacleSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton toggleButton, boolean isChecked) {
                showToast("Editing obstacle switch is " + (isChecked ? "on" : "off"));
                editObstacleFlag = isChecked;

                if (editObstacleFlag == true) {
                    imageIDSpinner.setEnabled(false);
                    obstacleBearingSpinner.setEnabled(false);
                    gridMap.setObstacleFlag(false);
                    dragObstacleSwitch.setChecked(false);
                }
            }
        });

        startPointToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked startPointToggle");
                if (startPointToggle.getText().equals("PLACE ROBOT")) {
                    showToast("Cancelled selecting starting point");
                }
                else if (startPointToggle.getText().equals("CANCEL") && !gridMap.getAutoUpdate()) {
                    showToast("Please select starting point");
                    gridMap.setStartCoordFlag(true);
                    gridMap.toggleCheckedButton("startPointToggle");
                }
                else;
                showLog("Exiting startPointToggle");
            }
        });

        addObstacleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked addObstacleButton");

                if (!gridMap.getObstacleFlag()) {
                    showToast("Please plot obstacle(s)");
                    gridMap.setObstacleFlag(true);
                    imageIDSpinner.setEnabled(true);
                    obstacleBearingSpinner.setEnabled(true);
                    gridMap.toggleCheckedButton("addObstacleButton");
                }
                else if (gridMap.getObstacleFlag()) {
                    gridMap.setObstacleFlag(false);
                    imageIDSpinner.setEnabled(false);
                    obstacleBearingSpinner.setEnabled(false);
                }

                editObstacleSwitch.setChecked(false);
                dragObstacleSwitch.setChecked(false);
                showLog("Obstacle flag = " + gridMap.getObstacleFlag());
                showLog("Exiting addObstacleButton");
            }
        });

        moveForwardButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveForwardButton");
                if (gridMap.getAutoUpdate()) {
                    updateStatus("");
                }
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("forward");
                    updateRobotAxisAndBearing();
                    if (gridMap.getValidPosition()) {
                        updateStatus("moving forward");
                    }
                    else {
                        updateStatus("Unable to move forward");
                    }
                    //printMessage("STM|b \n");
                }
                else {
                    updateStatus("Please press 'PLACE ROBOT'");
                }
                showLog("Exiting moveForwardButton");
            }
        });

        turnRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked turnRightButton");
                if (gridMap.getAutoUpdate());
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("right");
                    updateRobotAxisAndBearing();
                    //printMessage("STM|j  / \n");
                }
                else {
                    updateStatus("Please press 'PLACE ROBOT'");
                }
                showLog("Exiting turnRightButton");
            }
        });

        moveBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked moveBackButton");
                if (gridMap.getAutoUpdate());
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("back");
                    updateRobotAxisAndBearing();
                    if (gridMap.getValidPosition()) {
                        updateStatus("Moving backward");
                    }
                    else {
                        updateStatus("Unable to move backward");
                    }
                    //printMessage("STM|f \n");
                }
                else {
                    updateStatus("Please press 'PLACE ROBOT'");
                }
                showLog("Exiting moveBackButton");
            }
        });

        turnLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showLog("Clicked turnLeftButton");
                if (gridMap.getAutoUpdate());
                else if (gridMap.getCanDrawRobot() && !gridMap.getAutoUpdate()) {
                    gridMap.moveRobot("left");
                    updateRobotAxisAndBearing();
                    //printMessage("STM|i \n");
                }
                else {
                    updateStatus("Please press 'PLACE ROBOT'");
                }
                showLog("Exiting turnLeftButton");
            }
        });

        imageRecognitionToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked imageRecognitionToggle");
                ToggleButton imageRecognitionToggle = (ToggleButton) v;

                if (imageRecognitionToggle.getText().equals("IMAGE RECOGNITION MODE")) {
                    showToast("IR mode stopped");
                    robotStatusTextView.setText("IR MODE ENDED");
                    timerHandler.removeCallbacks(imageRecognitionTimerRunnable);
                }
                else if (imageRecognitionToggle.getText().equals("STOP")) {
                    String message = gridMap.getObstacles();
                    //printMessage(message);
                    stopImageRecognitionTimerFlag = false;
                    showToast("IR mode started");
                    robotStatusTextView.setText("IR MODE");
                    imageRecognitionTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(imageRecognitionTimerRunnable, 0);
                }
                else {
                    showToast("Else statement: " + imageRecognitionToggle.getText());
                }
                showLog("Exiting imageRecognitionToggle");
            }
        });

        fastestCarToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked fastestCarToggle");
                ToggleButton fastestCarToggle = (ToggleButton) v;
                if (fastestCarToggle.getText().equals("FASTEST CAR MODE")) {
                    showToast("FC mode stopped");
                    robotStatusTextView.setText("FC MODE ENDED");
                    timerHandler.removeCallbacks(fastestCarTimerRunnable);
                }
                else if (fastestCarToggle.getText().equals("STOP")) {
                    showToast("FC mode started");
                    /*try {
                        printMessage("STM|G");
                    }
                    catch (Exception e) {
                        showLog(e.getMessage());
                    }*/
                    stopFastestCarTimerFlag = false;
                    robotStatusTextView.setText("FC MODE");
                    fastestCarTimer = System.currentTimeMillis();
                    timerHandler.postDelayed(fastestCarTimerRunnable, 0);
                }
                else {
                    showToast(fastestCarToggle.getText().toString());
                }
                showLog("Exiting fastestCarToggle");
            }
        });

        resetImageRecognitionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked resetImageRecognitionButton");
                showToast("Resetting image recognition mode time...");
                imageRecognitionTimeTextView.setText("00:00");
                robotStatusTextView.setText("Not Available");
                if(imageRecognitionToggle.isChecked()) {
                    imageRecognitionToggle.toggle();
                }
                timerHandler.removeCallbacks(imageRecognitionTimerRunnable);
                showLog("Exiting resetImageRecognitionButton");
            }
        });

        resetFastestCarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showLog("Clicked resetFastestCarButton");
                showToast("Resetting fastest car mode time...");
                fastestCarTimeTextView.setText("00:00");
                robotStatusTextView.setText("Not Available");
                if(fastestCarToggle.isChecked()) {
                    fastestCarToggle.toggle();
                }
                timerHandler.removeCallbacks(fastestCarTimerRunnable);
                showLog("Exiting resetFastestCarButton");
            }
        });

//        sendMessageButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showLog("Clicked sendMessageButton");
//                String sentMessage = "" + sendMessageTextBox.getText().toString();
//
//                SharedPreferences.Editor editor = sharedPreferences.edit();
//                editor.putString("message", sharedPreferences.getString("message", "") + '\n' + sentMessage);
//                editor.commit();
//                receiveMessageTextBox.setText(sharedPreferences.getString("message", ""));
//                sendMessageTextBox.setText("");
//
//                /*if (BluetoothConnectionService.BluetoothConnectionStatus == true) {
//                    byte[] bytes = sentMessage.getBytes(Charset.defaultCharset());
//                    BluetoothConnectionService.write(bytes);
//                }*/
//                showLog("Exiting sendMessageButton");
//            }
//        });

    }
    // UI PART
    public static void sharedPreferences() {
        sharedPreferences = getSharedPreferences(context);
        editor = sharedPreferences.edit();
    }

    public static void refreshMessageReceived() {
        receiveMessageTextBox.setText(sharedPreferences.getString("message", ""));
    }

    public static void updateRobotAxisAndBearing() {
        robotXTextView.setText(String.valueOf(gridMap.getCurCoord()[0] - 1));
        robotYTextView.setText(String.valueOf(gridMap.getCurCoord()[1] - 1));
        robotBearingTextView.setText(sharedPreferences.getString("direction", ""));
    }

    public static void receiveMessage(String message) {
        showLog("Entering receiveMessage");
        sharedPreferences();
        editor.putString("message", sharedPreferences.getString("message", "") + "\n" + message);
        editor.commit();
        showLog("Exiting receiveMessage");
    }

    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    private void updateStatus(String message) {
        Toast toast = Toast.makeText(context, message, Toast.LENGTH_SHORT);
        toast.setGravity(Gravity.TOP,0, 0);
        toast.show();
    }

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    BroadcastReceiver messageReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String message = intent.getStringExtra("receivedMessage");
            showLog("receivedMessage: message --- " + message);

            if(message.contains(",")) {
                String[] command = message.split(",");

                //Check if there is a command sent by ALGO or RPI to get the obstacle information
                //ObstacleID refers to whatever in on the obstacle; ImageID is the embedded information
                if (command[0].equals("ALG") || command[0].equals("RPI")) {
                    showLog("command[0] is ALG or RPI");
                    if(obstacleIDCommand.equals("")) {
                        obstacleIDCommand = command[0].equals("ALG") ? command[1] : "";
                    }
                    if(imageIDCommand.equals("")) {
                        imageIDCommand = command[0].equals("RPI") ? command[1] : "";
                    }

                    showLog("obstacleIDCommand = " + obstacleIDCommand);
                    showLog("imageIDCommand = " + imageIDCommand);

                    //Update when both obstacle information is received
                    if (!(obstacleIDCommand.equals("") || imageIDCommand.equals(""))) {
                        showLog("imageIDCommand and obstacleIDCommand not empty");
                        gridMap.updateObstaclesFromRPI(obstacleIDCommand, imageIDCommand);
                        obstacleIDCommand = "";
                        imageIDCommand = "";
                    }
                }
                else {

                    //ALGO sends in cm and float e.g. 100, 100, N
                    float x = Integer.parseInt(command[0]);
                    float y = Integer.parseInt(command[1]);

                    //Received numbers to pass on to functions
                    int a = Math.round(x);
                    int b = Math.round(y);
                    a = (a / 10) + 1;
                    b = (b / 10) + 1;

                    String direction = command[2];

                    //Allow robot to show up on grid if it is within grid
                    if (a == 1) a++;
                    if (b == 20) b--;

                    if (command.length == 4){
                        String command2 = command[3];

                        //f = forward; r = reverse; sr = spot right; sl = spot left
                        if (command2.equals("f") || command2.equals("r") || command2.equals("sr") || command2.equals("sl")) {
                            showLog("forward, reverse or turn on spot");
                            gridMap.updateRobotPositionFromAlgo(a, b, direction);
                        }
                        //Further robot movements
                        else {
                            gridMap.moveRobotFromAlgo(a, b, direction, command2);
                        }
                    }
                }
            }
            else if (message.equals("END")) {
                ToggleButton imageRecognitionToggle = findViewById(R.id.imageRecognitionToggle);
                ToggleButton fastestCarToggle = findViewById(R.id.fastestCarToggle);

                if (imageRecognitionToggle.isChecked()) {
                    showLog("imageRecognitionToggle is checked");
                    stopImageRecognitionTimerFlag = true;
                    imageRecognitionToggle.setChecked(false);
                    robotStatusTextView.setText("IR MODE ENDED");
                }
                else if (fastestCarToggle.isChecked()) {
                    showLog("fastestCarToggle is checked");
                    stopFastestCarTimerFlag = true;
                    fastestCarToggle.setChecked(false);
                    robotStatusTextView.setText("FC MODE ENDED");
                }
            }
        }
    };

    @Override
    public void onSaveInstanceState(Bundle outState) {
        showLog("Entering onSaveInstanceState");
        super.onSaveInstanceState(outState);

        outState.putString(TAG, "onSaveInstanceState");
        showLog("Exiting onSaveInstanceState");
    }

    //////////////// end of UI PART///////////////////////






    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.bluetooth_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }


    // For "Enable Discovery" button onClick function
    public void enableBTDiscovery(MenuItem item) {
        btManager.toggleBluetooth();
    }

    @SuppressLint("MissingPermission")
    public void connectDeviceBtn(MenuItem item){
        if (btManager.bluetoothAdapter.isEnabled()){
            Intent intent = new Intent(MainActivity.this,DeviceListActivity.class);
            startActivity(intent);
        }
        else{
            Toast.makeText(this, "please enable bluetooth", Toast.LENGTH_SHORT).show();
        }
    }

    // Destructor for broadcast receivers
    @Override
    protected void onDestroy() {
        Log.e(BTManager.TAG, "Destroying all broadcast receivers");
        super.onDestroy();
        btManager.onDestroy();
    }

    public boolean checkCommand(String command){
        if (!command.equals("")){ // at least 1 element inside
            command = command.replaceAll("\\s", ""); // remove all in between spaces
            String[] arrOfStr = command.split(",");
            if (arrOfStr.length == 0){
                return false;
            }
            switch(arrOfStr[0]){
                case "TARGET":
                    if (arrOfStr.length == 3){
                        updateObstacle(arrOfStr[1],arrOfStr[2]);
                    }
                    return true;
                case "ROBOT":
                    if (arrOfStr.length == 4){
                        updateRobotPosition(arrOfStr[1],arrOfStr[2],arrOfStr[3]);
                    }
                    return true;
                case "MOVE":
                    if (arrOfStr.length == 2){
                        handleMovement(arrOfStr[1]);
                    }
                    return true;
                default:
                    return false;
            }
        }
        return false;
    }

    public void handleMessage(String sender,String content){
        String temp = content.trim();
        boolean valid = checkCommand(temp);
        if (!valid){
            // append default message (no commands involved)
            appendMessage(sender,temp);
        }
//        switch(temp){
//            case "w":
//                Log.e("MainActivity","Moving Forward");
//                appendMessage("Robot","Moving Forward...");
//                break;
//            // update the ui buttons here also
//            case "a":
//                Log.e("MainActivity","Moving Backward");
//                appendMessage("Robot","Moving Backward...");
//                break;
//            // update the ui buttons here also
//            case "s":
//                Log.e("MainActivity","Turning Left");
//                appendMessage("Robot","Turning Left...");
//                break;
//            // update the ui buttons here also
//            case "d":
//                Log.e("MainActivity","Turning Right");
//                appendMessage("Robot","Turning Right...");
//                break;
//            // update the ui buttons here also
//
//            default:
//                appendMessage(sender,content);
//                break;
//        }
    }

    private void handleMovement(String content){
        switch(content){
            case "w":
                Log.e("MainActivity","Moving Forward");
                appendMessage("Robot","Moving Forward...");
                robotStatusTextView.setText("Moving");
                gridMap.moveRobot("forward");
                updateRobotAxisAndBearing();
                if (gridMap.getValidPosition()) {
                    updateStatus("moving forward");
                }
                else {
                    updateStatus("Unable to move forward");
                }


                break;
            // update the ui buttons here also
            case "s":
                Log.e("MainActivity","Moving Backward");
                appendMessage("Robot","Moving Backward...");
                robotStatusTextView.setText("Moving");

                gridMap.moveRobot("back");
                updateRobotAxisAndBearing();

                break;
            // update the ui buttons here also
            case "a":
                Log.e("MainActivity","Turning Left");
                appendMessage("Robot","Turning Left...");
                robotStatusTextView.setText("Moving");

                gridMap.moveRobot("left");
                updateRobotAxisAndBearing();

                break;
            // update the ui buttons here also
            case "d":
                Log.e("MainActivity","Turning Right");
                appendMessage("Robot","Turning Right...");
                robotStatusTextView.setText("Moving");

                gridMap.moveRobot("right");
                updateRobotAxisAndBearing();

                break;
            // update the ui buttons here also

            case "stop":
                Log.e("MainActivity","Stopping");
                appendMessage("Robot","Stopping...");
                robotStatusTextView.setText("Stationary");
                break;
        }
    }

    public void appendMessage(String sender, String s){
        // first message only
        if (receiveMsgTextView.getText() == ""){
            receiveMsgTextView.append(sender + ": " + s);
        }
        else {
            receiveMsgTextView.append("\n" + sender + ": " + s);
        }
    }

    public void sendMessage(View view) {
        String messageToSend = sendMsgEditText.getText().toString();
        if (!messageToSend.equals("")){
            if (BTManager.instance.myBluetoothService != null){
                appendMessage("You" , messageToSend);
                BTManager.instance.myBluetoothService.sendMessage(messageToSend);
            }
            else {
                appendMessage("System","No devices connected!");
            }
        }
    }

    public void clearMessage(View view) {
        sendMsgEditText.setText("");
    }


    // Function to implement
    public void updateObstacle(String obstacleNumber, String targetID){
        // TO DO
        gridMap.updateObstaclesFromRPI(obstacleNumber,targetID);
    }

    public void updateRobotPosition(String x, String y, String direction){
        // TO DO
        gridMap.updateRobotPositionFromAlgo(Integer.parseInt(x),Integer.parseInt(y),direction);
    }
}