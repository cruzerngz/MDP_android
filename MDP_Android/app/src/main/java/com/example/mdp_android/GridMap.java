package com.example.mdp_android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;


import androidx.annotation.Nullable;

import static com.example.mdp_android.Constants.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Arrays;
import java.math.BigInteger;
import java.util.List;

public class GridMap extends View {
    boolean printRobotCoord = false;
    public GridMap(Context c) {
        super(c);
        initMap();
        setWillNotDraw(false);
    }

    // bonus
    ObstacleManager om = new ObstacleManager();
    // bonus(end)

    SharedPreferences sharedPreferences;

    private static JSONObject receivedJsonObject = new JSONObject();
    private static JSONObject backupMapInformation;

    private static final String TAG = "GridMap";
    private static final int COL = 20;
    private static final int ROW = 20;

    private static float cellSize;
    private static Cell[][] cells;
    private static int[] startCoord = new int[]{-1, -1};
    private static int[] curCoord = new int[]{-1, -1};
    private static int[] oldCoord = new int[]{-1, -1};

    private static ArrayList<String[]> arrowCoord = new ArrayList<>();
    private static ArrayList<int[]> obstacleCoord = new ArrayList<>();
    private Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.draw_arrow_error);
    private static String robotBearing = "None";

    private static boolean autoUpdate = false;
    private static boolean canDrawRobot = false;
    private static boolean startCoordFlag = false;
    private static boolean obstacleFlag = false;
    private static boolean cellFlag = false;
    private static boolean exploreFlag = false;
    private static boolean validPosition = false;
    private boolean mapDrawn = false;

    private Paint blackPaint = new Paint();
    private Paint whitePaint = new Paint();
    private Paint redPaint = new Paint();
    private Paint obstacleColor = new Paint();
    private Paint robotColor = new Paint();
    private Paint endColor = new Paint();
    private Paint startColor = new Paint();
    private Paint unexploredColor = new Paint();
    private Paint exploredColor = new Paint();
    private Paint arrowColor = new Paint();
    private Paint fastestPathColor = new Paint();

    public static String publicMDFExploration;
    public static String publicMDFObstacle;

    public ArrayList<String[]> IMAGE_ID_LIST = new ArrayList<>(Arrays.asList(
            new String[20], new String[20], new String[20], new String[20], new String[20],
            new String[20], new String[20], new String[20], new String[20], new String[20],
            new String[20], new String[20], new String[20], new String[20], new String[20],
            new String[20], new String[20], new String[20], new String[20], new String[20]
    ));
    public ArrayList<String[]> OBSTACLE_BEARING_LIST = new ArrayList<>(Arrays.asList(
            new String[20], new String[20], new String[20], new String[20], new String[20],
            new String[20], new String[20], new String[20], new String[20], new String[20],
            new String[20], new String[20], new String[20], new String[20], new String[20],
            new String[20], new String[20], new String[20], new String[20], new String[20]
    ));

    static ClipData clipData;
    static Object localState;
    int initialColumn, initialRow;

    public GridMap(Context context, @Nullable AttributeSet attrs){
        super(context, attrs);
        initMap();

        blackPaint.setStyle(Paint.Style.FILL_AND_STROKE);
        whitePaint.setColor(Color.WHITE);
        whitePaint.setTextSize(15);
        whitePaint.setTextAlign(Paint.Align.CENTER);
        redPaint.setColor(Color.RED);
        redPaint.setStrokeWidth(3);
        obstacleColor.setColor(Color.BLACK);
        robotColor.setColor(Color.YELLOW);
        endColor.setColor(Color.RED);
        startColor.setColor(Color.CYAN);
        unexploredColor.setColor(Color.LTGRAY);
        exploredColor.setColor(Color.WHITE);
        arrowColor.setColor(Color.BLACK);
        fastestPathColor.setColor(Color.MAGENTA);

        // get shared preferences
        sharedPreferences = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
    }

    private void initMap() {
        setWillNotDraw(false);
    }

    @Override
    protected void onDraw(Canvas canvas){
        showLog("Entering onDraw");
        showLog("canDrawRobot = " + String.valueOf(getCanDrawRobot()));
        super.onDraw(canvas);
        showLog("Drawing map");

        Log.d(TAG,"Creating Cell");

        if (!mapDrawn) {
            String[] pseudoArrowCoord = new String[3];
            pseudoArrowCoord[0] = "1";
            pseudoArrowCoord[1] = "1";
            pseudoArrowCoord[2] = "pseudo";
            arrowCoord.add(pseudoArrowCoord);
            this.createCell();
            mapDrawn = true;
        }

        drawCell(canvas);
        drawHorizontalLines(canvas);
        drawVerticalLines(canvas);
        drawGridNumber(canvas);
        if (getCanDrawRobot()) {
            drawRobot(canvas, curCoord);

            if (printRobotCoord == false){
                String robotCoord = "my Coordinates are: [" + (curCoord[0] - 1) + "," + (curCoord[1] - 1) + "]";
                Log.e("GridMap", robotCoord);
                BTManager.instance.passMessageToMessageInterface("Robot",robotCoord);
                printRobotCoord = true;
            }

        }
        drawArrow(canvas, arrowCoord);
        drawObstacles(canvas);

        showLog("Exiting onDraw");
    }

    private void drawObstacles(Canvas canvas) {
        showLog("Entering drawObstacles");

        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                //Draw imageID
                canvas.drawText(IMAGE_ID_LIST.get(19-i)[j], cells[j+1][19-i].startX + ((cells[1][1].endX - cells[1][1].startX) / 2.2f),
                        cells[j+1][i].startY + ((cells[1][1].endY - cells[1][1].startY) / 3) + 10, whitePaint);

                //Draw the obstacle direction
                switch (OBSTACLE_BEARING_LIST.get(19-i)[j]) {
                    case "North":
                        canvas.drawLine(cells[j + 1][20 - i].startX, cells[j + 1][i].startY, cells[j + 1][20 - i].endX, cells[j + 1][i].startY, redPaint);
                        break;

                    case "South":
                        canvas.drawLine(cells[j + 1][20 - i].startX, cells[j + 1][i].startY + cellSize, cells[j + 1][20 - i].endX, cells[j + 1][i].startY + cellSize, redPaint);
                        break;

                    case "East":
                        canvas.drawLine(cells[j + 1][20 - i].startX + cellSize, cells[j + 1][i].startY, cells[j + 1][20 - i].startX + cellSize, cells[j + 1][i].endY, redPaint);
                        break;

                    case "West":
                        canvas.drawLine(cells[j + 1][20 - i].startX, cells[j + 1][i].startY, cells[j + 1][20 - i].startX, cells[j + 1][i].endY, redPaint);
                        break;
                }
            }
        }
        showLog("Exiting drawObstacles");
    }

    private void drawCell(Canvas canvas) {
        showLog("Entering drawCell");
        for (int x = 1; x <= COL; x++) {
            for (int y = 0; y < ROW; y++) {
                for (int i = 0; i < this.getArrowCoord().size(); i++) {
                    if (!cells[x][y].type.equals("image") && cells[x][y].getId() == -1) {
                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);
                    }
                    else {
                        Paint textPaint = new Paint();
                        textPaint.setTextSize(20);
                        textPaint.setColor(Color.WHITE);
                        textPaint.setTextAlign(Paint.Align.CENTER);
                        canvas.drawRect(cells[x][y].startX, cells[x][y].startY, cells[x][y].endX, cells[x][y].endY, cells[x][y].paint);
                        canvas.drawText(String.valueOf(cells[x][y].getId()), (cells[x][y].startX + cells[x][y].endX) / 2, cells[x][y].endY + (cells[x][y].startY - cells[x][y].endY) / 4, textPaint);
                    }
                }
            }
        }
        showLog("Exiting drawCell");
    }

    private void drawHorizontalLines(Canvas canvas) {
        showLog("Entering drawHorizontalLines");
        for (int y = 0; y <= ROW; y++) {
            canvas.drawLine(cells[1][y].startX, cells[1][y].startY - (cellSize / 30), cells[20][y].endX, cells[20][y].startY - (cellSize / 30), blackPaint);
        }
        showLog("Exiting drawHorizontalLines");
    }

    private void drawVerticalLines(Canvas canvas) {
        showLog("Entering drawVerticalLines");
        for (int x = 0; x <= COL; x++) {
            canvas.drawLine(cells[x][0].startX - (cellSize / 30) + cellSize, cells[x][0].startY - (cellSize / 30), cells[x][0].startX - (cellSize / 30) + cellSize, cells[x][19].endY + (cellSize / 30), blackPaint);
        }
        showLog("Exiting drawVerticalLines");
    }

    private void drawGridNumber(Canvas canvas) {
        showLog("Entering drawGridNumber");
        for (int x = 1; x <= COL; x++) {
            if (x <= 10) {
                canvas.drawText(Integer.toString(x - 1), cells[x][20].startX + (cellSize / 4), cells[x][20].startY + (cellSize / 2), blackPaint);
            }
            else {
                canvas.drawText(Integer.toString(x - 1), cells[x][20].startX + (cellSize / 5), cells[x][20].startY + (cellSize / 2), blackPaint);
            }
        }

        for (int y = 0; y < ROW; y++) {
            if (y <= 9) { //Counting from top to down
                canvas.drawText(Integer.toString(19 - y), cells[0][y].startX + (cellSize / 3.5f), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
            }
            else {
                canvas.drawText(Integer.toString(19 - y), cells[0][y].startX + (cellSize / 2), cells[0][y].startY + (cellSize / 1.5f), blackPaint);
            }
        }
        showLog("Exiting drawGridNumber");
    }

    private void drawRobot(Canvas canvas, int[] curCoord) {
        showLog("Entering drawRobot");
        int androidRowCoord = this.convertRow(curCoord[1]);
        for (int y = androidRowCoord; y <= androidRowCoord + 1; y++) {
            canvas.drawLine(cells[curCoord[0] - 1][y].startX, cells[curCoord[0] - 1][y].startY - (cellSize / 30), cells[curCoord[0] + 1][y].endX, cells[curCoord[0] + 1][y].startY - (cellSize / 30), robotColor);
        }
        for (int x = curCoord[0] - 1; x < curCoord[0] + 1; x++) {
            canvas.drawLine(cells[x][androidRowCoord - 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord - 1].startY, cells[x][androidRowCoord + 1].startX - (cellSize / 30) + cellSize, cells[x][androidRowCoord + 1].endY, robotColor);
        }

        switch (this.getRobotBearing()) {
            case "North":
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, (cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, blackPaint);
                canvas.drawLine((cells[curCoord[0]][androidRowCoord - 1].startX + cells[curCoord[0]][androidRowCoord - 1].endX) / 2, cells[curCoord[0]][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                break;

            case "South":
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, (cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, blackPaint);
                canvas.drawLine((cells[curCoord[0]][androidRowCoord + 1].startX + cells[curCoord[0]][androidRowCoord + 1].endX) / 2, cells[curCoord[0]][androidRowCoord + 1].endY, cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, blackPaint);
                break;

            case "East":
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord - 1].startX, cells[curCoord[0] - 1][androidRowCoord - 1].startY, cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, blackPaint);
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord].endX, cells[curCoord[0] + 1][androidRowCoord - 1].endY + (cells[curCoord[0] + 1][androidRowCoord].endY - cells[curCoord[0] + 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] - 1][androidRowCoord + 1].startX, cells[curCoord[0] - 1][androidRowCoord + 1].endY, blackPaint);
                break;

            case "West":
                canvas.drawLine(cells[curCoord[0] + 1][androidRowCoord - 1].endX, cells[curCoord[0] + 1][androidRowCoord - 1].startY, cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, blackPaint);
                canvas.drawLine(cells[curCoord[0] - 1][androidRowCoord].startX, cells[curCoord[0] - 1][androidRowCoord - 1].endY + (cells[curCoord[0] - 1][androidRowCoord].endY - cells[curCoord[0] - 1][androidRowCoord - 1].endY) / 2, cells[curCoord[0] + 1][androidRowCoord + 1].endX, cells[curCoord[0] + 1][androidRowCoord + 1].endY, blackPaint);
                break;

            default:
                Toast.makeText(this.getContext(), "Error with drawing robot (unknown direction)", Toast.LENGTH_LONG).show();
                break;
        }
        showLog("Exiting drawRobot");
    }

    private ArrayList<String[]> getArrowCoord() {
        return arrowCoord;
    }

    public String getRobotBearing() {
        return robotBearing;
    }

    public void setAutoUpdate(boolean autoUpdate) throws JSONException {
        showLog(String.valueOf(backupMapInformation));
        if (!autoUpdate)
            backupMapInformation = this.getReceivedJsonObject();
        else {
            setReceivedJsonObject(backupMapInformation);
            backupMapInformation = null;
            this.updateMapInformation();
        }
        GridMap.autoUpdate = autoUpdate;
    }

    public JSONObject getReceivedJsonObject() {
        return receivedJsonObject;
    }

    public void setReceivedJsonObject(JSONObject receivedJsonObject) {
        showLog("Entered setReceivedJsonObject");
        GridMap.receivedJsonObject = receivedJsonObject;
        backupMapInformation = receivedJsonObject;
    }

    public boolean getAutoUpdate() {
        return autoUpdate;
    }

    private void setValidPosition(boolean status) {
        validPosition = status;
    }

    public boolean getValidPosition() {
        return validPosition;
    }

    public void setObstacleFlag(boolean status) {
        obstacleFlag = status;
    }

    public boolean getObstacleFlag() {
        return obstacleFlag;
    }

    public void setStartCoordFlag(boolean status) {
        startCoordFlag = status;
    }

    private boolean getStartCoordFlag() {
        return startCoordFlag;
    }

    public boolean getCanDrawRobot() {
        return canDrawRobot;
    }

    private void createCell() {
        showLog("Entering createCell");
        cells = new Cell[COL + 1][ROW + 1];
        this.calculateDimension();
        cellSize = this.getCellSize();

        for (int x = 0; x <= COL; x++) {
            for (int y = 0; y <= ROW; y++) {
                cells[x][y] = new Cell(x * cellSize + (cellSize / 30), y * cellSize + (cellSize / 30), (x + 1) * cellSize, (y + 1) * cellSize, unexploredColor, "unexplored");
            }
        }
        showLog("Exiting createCell");
    }

    public void setStartCoord(int col, int row) {
        showLog("Entering setStartCoord");
        startCoord[0] = col;
        startCoord[1] = row;
        String direction = getRobotBearing();
        if(direction.equals("None")) {
            direction = "North";
        }
        if (this.getStartCoordFlag()) {
            this.setCurCoord(col, row, direction);
        }
        showLog("Exiting setStartCoord");
    }

    private int[] getStartCoord() {
        return startCoord;
    }

    public void setCurCoord(int col, int row, String direction) {
        showLog("Entering setCurCoord");
        curCoord[0] = col;
        curCoord[1] = row;
        this.setRobotDirection(direction);
        this.updateRobotAxisAndBearing(col, row, direction);

        row = this.convertRow(row);
        for (int x = col - 1; x <= col + 1; x++) {
            for (int y = row - 1; y <= row + 1; y++) {
                cells[x][y].setType("robot");
            }
        }
        showLog("Exiting setCurCoord");
    }

    public int[] getCurCoord() {
        return curCoord;
    }

    private void calculateDimension() {
        this.setCellSize(getWidth()/(COL+1));
    }

    private int convertRow(int row) {
        return (20 - row);
    }

    private void setCellSize(float cellSize) {
        GridMap.cellSize = cellSize;
    }

    private float getCellSize() {
        return cellSize;
    }

    private void setOldRobotCoord(int oldCol, int oldRow) {
        showLog("Entering setOldRobotCoord");
        oldCoord[0] = oldCol;
        oldCoord[1] = oldRow;
        oldRow = this.convertRow(oldRow);
        for (int x = oldCol - 1; x <= oldCol + 1; x++) {
            for (int y = oldRow - 1; y <= oldRow + 1; y++) {
                cells[x][y].setType("explored");
            }
        }
        showLog("Exiting setOldRobotCoord");
    }

    private int[] getOldRobotCoord() {
        return oldCoord;
    }

    private void setArrowCoordinate(int col, int row, String arrowDirection) {
        showLog("Entering setArrowCoordinate");
        int[] obstacleCoord = new int[]{col, row};
        this.getObstacleCoord().add(obstacleCoord);
        String[] arrowCoord = new String[3];
        arrowCoord[0] = String.valueOf(col);
        arrowCoord[1] = String.valueOf(row);
        arrowCoord[2] = arrowDirection;
        this.getArrowCoord().add(arrowCoord);

        row = convertRow(row);
        cells[col][row].setType("arrow");
        showLog("Exiting setArrowCoordinate");
    }

    public void setRobotDirection(String direction) {
        sharedPreferences = getContext().getSharedPreferences("Shared Preferences", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        robotBearing = direction;
        editor.putString("direction", direction);
        editor.commit();
        this.invalidate();
    }

    private void updateRobotAxisAndBearing(int col, int row, String direction) {
        TextView robotXTextView =  ((Activity)this.getContext()).findViewById(R.id.robotXTextView);
        TextView robotYTextView =  ((Activity)this.getContext()).findViewById(R.id.robotYTextView);
        TextView robotBearingTextView =  ((Activity)this.getContext()).findViewById(R.id.robotBearingTextView);

        robotXTextView.setText(String.valueOf(col-1));
        robotYTextView.setText(String.valueOf(row-1));
        robotBearingTextView.setText(direction);
    }

    public void sendObstacleInformation(int column, int row, String imageid, String face) {
        String x = String.valueOf(column-1);
        String y = String.valueOf(row-1);
        String obstacleImageID = imageid;
        String obstacleFace = face;

        String info = "Coordinates: " + x + ", " + y + " | Direction: " + obstacleFace + " | currentObstacleID: " + obstacleImageID;
//        BTManager.instance.myBluetoothService.sendMessage(info);
        TextView receiveMsgTextView = ((Activity)this.getContext()).findViewById(R.id.receiveMsgTextView);
        receiveMsgTextView.append("\n" + info + "\n");
    }

    public void setObstacleCoord(int col, int row) {
        showLog("Entering setObstacleCoord");
        int[] obstacleCoord = new int[]{col, row};
        GridMap.obstacleCoord.add(obstacleCoord);
        row = this.convertRow(row);
        cells[col][row].setType("obstacle");
        showLog("Exiting setObstacleCoord");
    }

    private ArrayList<int[]> getObstacleCoord() {
        return obstacleCoord;
    }

    private static void showLog(String message) {
        Log.d(TAG, message);
    }

    private void drawArrow(Canvas canvas, ArrayList<String[]> arrowCoord) {
        showLog("Entering drawArrow");
        RectF rect;

        for (int i = 0; i < arrowCoord.size(); i++) {
            if (!arrowCoord.get(i)[2].equals("pseudo")) {
                int col = Integer.parseInt(arrowCoord.get(i)[0]);
                int row = convertRow(Integer.parseInt(arrowCoord.get(i)[1]));
                rect = new RectF(col * cellSize, row * cellSize, (col + 1) * cellSize, (row + 1) * cellSize);
                switch (arrowCoord.get(i)[2]) {
                    case "North":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.draw_arrow_up);
                        break;

                    case "East":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.draw_arrow_right);
                        break;

                    case "South":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.draw_arrow_down);
                        break;

                    case "West":
                        arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.draw_arrow_left);
                        break;

                    default:
                        break;
                }
                canvas.drawBitmap(arrowBitmap, null, rect, null);
            }
            showLog("Exiting drawArrow");
        }
    }

    private class Cell {
        float startX, startY, endX, endY;
        Paint paint;
        String type;
        int id = -1;

        private Cell(float startX, float startY, float endX, float endY, Paint paint, String type) {
            this.startX = startX;
            this.startY = startY;
            this.endX = endX;
            this.endY = endY;
            this.paint = paint;
            this.type = type;
        }

        public void setType(String type) {
            this.type = type;
            switch (type) {
                case "obstacle":
                    this.paint = obstacleColor;
                    break;

                case "robot":
                    this.paint = robotColor;
                    break;

                case "end":
                    this.paint = endColor;
                    break;

                case "start":
                    this.paint = startColor;
                    break;

                case "unexplored":
                    this.paint = unexploredColor;
                    break;

                case "explored":
                    this.paint = exploredColor;
                    break;

                case "arrow":
                    this.paint = arrowColor;
                    break;

                case "fastestPath":
                    this.paint = fastestPathColor;
                    break;

                case "image":
                    this.paint = obstacleColor;

                default:
                    showLog("setType default: " + type);
                    break;
            }
        }

        public void setId(int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }
    }

    int endColumn, endRow;
    String oldItem;

    //Obstacle drag segment
    @Override
    public boolean onDragEvent(DragEvent dragEvent) {
        try {
            showLog("Entering onDragEvent");
            clipData = dragEvent.getClipData();
            localState = dragEvent.getLocalState();

            String tempID, tempBearing;
            tempID = tempBearing = "";
            endColumn = endRow = -999;
            oldItem = IMAGE_ID_LIST.get(initialRow - 1)[initialColumn - 1];
            showLog("dragEvent.getAction() == " + dragEvent.getAction());
            showLog("dragEvent.getResult() is " + dragEvent.getResult());
            showLog("initialColumn = " + initialColumn + ", initialRow = " + initialRow);

            //If obstacle is dragged and dropped out of the gridmap
            if ((dragEvent.getAction() == DragEvent.ACTION_DRAG_ENDED) && (endColumn == -999 || endRow == -999) && dragEvent.getResult() == false) {
                for (int i = 0; i < obstacleCoord.size(); i++) {
                    if (Arrays.equals(obstacleCoord.get(i), new int[]{initialColumn - 1, initialRow - 1})) {
                        obstacleCoord.remove(i);
                    }
                }
                // remove from Obstacles array List
                om.removeObstacle(Integer.toString(initialColumn - 1), Integer.toString(initialRow - 1));
                cells[initialColumn][20 - initialRow].setType("unexplored");
                IMAGE_ID_LIST.get(initialRow - 1)[initialColumn - 1] = "";
                OBSTACLE_BEARING_LIST.get(initialRow - 1)[initialColumn - 1] = "";
                showLog(commandMessageGenerator(REMOVE_OBSTACLE));
                // commented out for Wk 8 and Wk 9
//            MainActivity.printMessage(commandMessageGenerator(REMOVE_OBSTACLE));
            }

            //If obstacle is dropped within the gridmap
            else if (dragEvent.getAction() == DragEvent.ACTION_DROP && this.getAutoUpdate() == false) {
                endColumn = (int) (dragEvent.getX() / cellSize);
                endRow = this.convertRow((int) (dragEvent.getY() / cellSize));

                //If user clicked on an empty cell
                if (IMAGE_ID_LIST.get(initialRow - 1)[initialColumn - 1].equals("") && OBSTACLE_BEARING_LIST.get(initialRow - 1)[initialColumn - 1].equals("")) {
                    showLog("Cell is empty");
                }

                //If obstacle is dragged and dropped out of the gridmap
                else if (endColumn <= 0 || endRow <= 0) {
                    for (int i = 0; i < obstacleCoord.size(); i++) {
                        if (Arrays.equals(obstacleCoord.get(i), new int[]{initialColumn - 1, initialRow - 1})) {
                            obstacleCoord.remove(i);
                        }
                    }
                    cells[initialColumn][20 - initialRow].setType("unexplored");
                    IMAGE_ID_LIST.get(initialRow - 1)[initialColumn - 1] = "";
                    OBSTACLE_BEARING_LIST.get(initialRow - 1)[initialColumn - 1] = "";
                    showLog(commandMessageGenerator(REMOVE_OBSTACLE));
                    // commented out for Wk 8 and Wk 9
//                MainActivity.printMessage(commandMessageGenerator(REMOVE_OBSTACLE));
                }

                //If obstacles is dropped on an empty cell, place it there unless there is an existing item
                else if ((1 <= initialColumn && initialColumn <= 20) && (1 <= initialRow && initialRow <= 20) && (1 <= endColumn && endColumn <= 20) && (1 <= endRow && endRow <= 20)) {
                    tempID = IMAGE_ID_LIST.get(initialRow - 1)[initialColumn - 1];
                    tempBearing = OBSTACLE_BEARING_LIST.get(initialRow - 1)[initialColumn - 1];

                    //Check if there is any obstacle is the desired drop cell
                    if (IMAGE_ID_LIST.get(endRow - 1)[endColumn - 1] != "" || OBSTACLE_BEARING_LIST.get(endRow - 1)[endColumn - 1] != "") {
                        showLog("An obstacle is already at the drop location");
                        Toast.makeText(BTManager.instance.appCompatActivity, "An obstacle is already at the drop location...", Toast.LENGTH_SHORT).show();

                    } else {
                        IMAGE_ID_LIST.get(initialRow - 1)[initialColumn - 1] = "";
                        OBSTACLE_BEARING_LIST.get(initialRow - 1)[initialColumn - 1] = "";
                        IMAGE_ID_LIST.get(endRow - 1)[endColumn - 1] = tempID;

                        OBSTACLE_BEARING_LIST.get(endRow - 1)[endColumn - 1] = tempBearing;
                        setObstacleCoord(endColumn, endRow);

                        om.updateObstacle(om.getObstacle(Integer.toString(initialColumn - 1), Integer.toString(initialRow - 1)).obstacleID, (endColumn - 1) + "", (endRow - 1) + "", tempBearing, "");

//                    sendObstacleInformation(endColumn, endRow, tempID, tempBearing);
                        sendObstacleInformation(endColumn, endRow, om.getObstacle(Integer.toString(endColumn - 1), Integer.toString(endRow - 1)).obstacleID, tempBearing);

                        for (int i = 0; i < obstacleCoord.size(); i++) {
                            if (Arrays.equals(obstacleCoord.get(i), new int[]{initialColumn - 1, initialRow - 1})) {
                                obstacleCoord.remove(i);
                            }
                        }
                        cells[initialColumn][20 - initialRow].setType("unexplored");
                        showLog(commandMessageGenerator(MOVE_OBSTACLE));
                        // commented out for Wk 8 and Wk 9
//                    MainActivity.printMessage(commandMessageGenerator(MOVE_OBSTACLE));
                    }
                } else {
                    showLog("Drag event failed");
                }
            }

            showLog("initialColumn = " + initialColumn + ", initialRow = " + initialRow + "\nendColumn = " + endColumn + ", endRow = " + endRow);
            this.invalidate();
            return true;
        }
        catch(Exception e){
            Log.e("dfdf","FKU");
            return false;
        }
    }

    public void callInvalidate() {
        showLog("Entering callInvalidate");
        this.invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event){
        showLog("Entering onTouchEvent");
        if (event.getAction() == MotionEvent.ACTION_DOWN && this.getAutoUpdate() == false) {
            int column = (int) (event.getX() / cellSize);
            int row = this.convertRow((int) (event.getY() / cellSize));
            initialColumn = column;
            initialRow = row;

            ToggleButton startPointToggle = ((Activity) this.getContext()).findViewById(R.id.startPointToggle);

            showLog("event.getX = " + event.getX() + ", event.getY = " + event.getY());
            showLog("row = " + row + ", column = " + column);

            if (MainActivity.dragObstacleFlag) {
                if (!((1 <= initialColumn && initialColumn <= 20) && (1 <= initialRow && initialRow <= 20))) {
                    return false;
                }
                else if (IMAGE_ID_LIST.get(row - 1)[column - 1].equals("") && OBSTACLE_BEARING_LIST.get(row - 1)[column - 1].equals("")) {
                    return false;
                }

                DragShadowBuilder dragShadowBuilder = new MyDragShadowBuilder(this);
                this.startDragAndDrop(null, dragShadowBuilder, null, 0);
            }

            if (MainActivity.editObstacleFlag) {
                if (!((1 <= initialColumn && initialColumn <= 20) && (1 <= initialRow && initialRow <= 20))) {
                    return false;
                }
                else if (IMAGE_ID_LIST.get(row - 1)[column - 1] == "" && OBSTACLE_BEARING_LIST.get(row - 1)[column - 1] == "") {
                    return false;
                }
                else {
                    showLog("Enter edit obstacle");
                    String currentImageID = IMAGE_ID_LIST.get(row -1)[column - 1];
                    String currentObstacleBearing = OBSTACLE_BEARING_LIST.get(row - 1)[column - 1];
                    final int tRow = row;
                    final int tCol = column;

                    AlertDialog.Builder mBuilder = new AlertDialog.Builder(this.getContext());
                    View mView = ((Activity) this.getContext()).getLayoutInflater().inflate(R.layout.pop_up_edit_obstacle, null);
                    mBuilder.setTitle("Edit existing obstacle");

                    final Spinner imageIDPopUpSpinner = mView.findViewById(R.id.imageIDPopUpSpinner);
                    final Spinner obstacleBearingPopUpSpinner = mView.findViewById(R.id.obstacleBearingPopUpSpinner);

                    ArrayAdapter<CharSequence> imageIDAdapter = ArrayAdapter.createFromResource(this.getContext(), R.array.imageID_Array, android.R.layout.simple_spinner_item);
                    imageIDAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    imageIDPopUpSpinner.setAdapter(imageIDAdapter);

                    ArrayAdapter<CharSequence> obstacleBearingAdapter = ArrayAdapter.createFromResource(this.getContext(), R.array.obstacleBearing_Array, android.R.layout.simple_spinner_item);
                    obstacleBearingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    obstacleBearingPopUpSpinner.setAdapter(obstacleBearingAdapter);

                    //Spinner begins at 0 for imageID, and last direction for obstacleBearing
                    if (currentImageID.equals("")) {
                        imageIDPopUpSpinner.setSelection(0);
                    }
                    else {
                        imageIDPopUpSpinner.setSelection(0);
                    }
                    switch (currentObstacleBearing) {
                        case "North":
                            obstacleBearingPopUpSpinner.setSelection(0);
                            break;

                        case "South":
                            obstacleBearingPopUpSpinner.setSelection(1);
                            break;

                        case "East":
                            obstacleBearingPopUpSpinner.setSelection(2);
                            break;

                        case "West":
                            obstacleBearingPopUpSpinner.setSelection(3);
                            break;
                    }

                    //When user click OK
                    mBuilder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            String newImageID = imageIDPopUpSpinner.getSelectedItem().toString();
                            String newObstacleBearing = obstacleBearingPopUpSpinner.getSelectedItem().toString();

                            IMAGE_ID_LIST.get(tRow - 1)[tCol - 1] = newImageID;
                            OBSTACLE_BEARING_LIST.get(tRow - 1)[tCol - 1] = newObstacleBearing;
                            showLog("tRow - 1 = " + (tRow - 1));
                            showLog("tCol - 1 = " + (tCol - 1));
                            showLog("newImageID = " + newImageID);
                            showLog("newObstacleBearing = " + newObstacleBearing);

                            // assume new obstacle confirmed
                            if(!om.checkExists(newImageID))
                                om.addObstacle(new Obstacle(newImageID,(tCol-1)+"",(tRow-1)+"",newObstacleBearing,""));
                            else
                                om.updateObstacle(newImageID,(tCol-1)+"",(tRow-1)+"",newObstacleBearing,"");
                            sendObstacleInformation(tCol, tRow, newImageID, newObstacleBearing);


                            callInvalidate();
                        }
                    });

                    //When user clicks CANCEL
                    mBuilder.setNegativeButton("CANCEL", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    });

                    mBuilder.setView(mView);
                    AlertDialog dialog = mBuilder.create();
                    dialog.show();
                    Window window =  dialog.getWindow();
                    WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
                    layoutParams.width = 150;
                    window.setLayout(layoutParams.WRAP_CONTENT, layoutParams.WRAP_CONTENT);
                }
                showLog("Exit edit obstacle");
            }

            if (startCoordFlag) {
                if (canDrawRobot) {
                    for (int i = 0; i < 21; i++) {
                        for (int j = 0; j < 21; j++) {
                            if (cells[i][j].type == "robot") {
                                cells[i][j].setType("explored");
                            }
                        }
                    }

                    //Do not place robot if there are obstacles there
                    int[] startCoord = this.getStartCoord();
                    if (startCoord[0] >= 2 && startCoord[1] >= 2) {
                        showLog("startCoord = " + startCoord[0] + " " + startCoord[1]);
                        for (int x = startCoord[0] - 1; x <= startCoord[0]; x++) {
                            for (int y = startCoord[1] - 1; y <= startCoord[1]; y++) {
                                cells[x][y].setType("unexplored");
                            }
                        }
                    }
                }
                else {
                    canDrawRobot = true;
                }
                showLog("curCoord[0] = " + curCoord[0] + ", curCoord[1] = " + curCoord[1]);
                showLog("");
                this.setStartCoord(column, row);
                startCoordFlag = false;
                String direction = getRobotBearing();
                if(direction.equals("None")) {
                    direction = "North";
                }
                try {
                    int directionInt = 0;
                    if(direction.equals("North")){
                        directionInt = 0;
                    }
                    else if(direction.equals("West")) {
                        directionInt = 3;
                    }
                    else if(direction.equals("East")) {
                        directionInt = 1;
                    }
                    else if(direction.equals("South")) {
                        directionInt = 2;
                    }
                    showLog("starting " + "(" + String.valueOf(row-1) + "," + String.valueOf(column-1) + "," + String.valueOf(directionInt) + ")");
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
                updateRobotAxisAndBearing(column, row, direction);
                if (startPointToggle.isChecked()) {
                    startPointToggle.toggle();
                }
                this.invalidate();
                return true;
            }

            //
            if (obstacleFlag) {
                if ((1 <= row && row <= 20) && (1 <= column && column <= 20)) {
                    // get user input from spinners in MapTabFragment static values
                    String imageID = (MainActivity.imageID).equals("0") ? "" : MainActivity.imageID;
                    String obstacleBearing = MainActivity.obstacleBearing;

                    if(!om.checkExists(imageID))
                    {
                        IMAGE_ID_LIST.get(row - 1)[column - 1] = "";
//                      IMAGE_ID_LIST.get(row - 1)[column - 1] = imageID;
                        OBSTACLE_BEARING_LIST.get(row - 1)[column - 1] = obstacleBearing;
                        this.setObstacleCoord(column, row);

                        om.addObstacle(new Obstacle(imageID,(column-1)+"",(row-1)+"",obstacleBearing,""));
                        sendObstacleInformation(column, row, imageID, obstacleBearing);
                        showLog(commandMessageGenerator(ADD_OBSTACLE));
                        this.invalidate();
                    }

                    else{
//                      om.updateObstacle(imageID,column+"",row+"",obstacleBearing,"");
                        TextView receiveMsgTextView = ((Activity)this.getContext()).findViewById(R.id.receiveMsgTextView);
                        receiveMsgTextView.append("\n" + "ObstacleID already on map, Please choose other IDs!" + "\n");
                        Toast.makeText(BTManager.instance.appCompatActivity, "ObstacleID already on map", Toast.LENGTH_SHORT).show();
                    }

//                    Log.e("GridMap","x y coordinate are: " + this.getObstacleCoord());


                    // commented out for Wk 8 and 9
                    //MainActivity.printMessage(commandMessageGenerator(ADD_OBSTACLE));
                }

                return true;
            }

            if (exploreFlag) {
                cells[column][20-row].setType("explored");
                this.invalidate();
                return true;
            }

            //Removing obstacles information
            if (cellFlag) {
                ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
                cells[column][20-row].setType("unexplored");
                for (int i=0; i<obstacleCoord.size(); i++) {
                    if (obstacleCoord.get(i)[0] == column && obstacleCoord.get(i)[1] == row) {
                        obstacleCoord.remove(i);
                    }
                }

                IMAGE_ID_LIST.get(row)[column-1] = "";  // remove imageID
                OBSTACLE_BEARING_LIST.get(row)[column-1] = "";  // remove bearing
                this.invalidate();
                return true;
            }
        }
        showLog("Exiting onTouchEvent");
        return false;
    }

    public void toggleCheckedButton(String buttonName) {
        ToggleButton startPointToggle = ((Activity)this.getContext()).findViewById(R.id.startPointToggle);
        Button addObstacleButton = ((Activity)this.getContext()).findViewById(R.id.addObstacleButton);

        if (!buttonName.equals("startPointToggle"))
            if (startPointToggle.isChecked()) {
                this.setStartCoordFlag(false);
                startPointToggle.toggle();
            }
        if (!buttonName.equals("addObstacleButton"))
            if (addObstacleButton.isEnabled())
                this.setObstacleFlag(false);
    }

    public void resetMap() {
        showLog("Entering resetMap");
        TextView robotStatusTextView =  ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
        updateRobotAxisAndBearing(1, 1, "None");
        robotStatusTextView.setText("Not Available");
        SharedPreferences.Editor editor = sharedPreferences.edit();

        this.toggleCheckedButton("None");

        receivedJsonObject = null;
        backupMapInformation = null;
        startCoord = new int[]{-1, -1};
        curCoord = new int[]{-1, -1};
        oldCoord = new int[]{-1, -1};
        robotBearing = "None";
        autoUpdate = false;
        arrowCoord = new ArrayList<>();
        obstacleCoord = new ArrayList<>();
        mapDrawn = false;
        canDrawRobot = false;
        validPosition = false;
        Bitmap arrowBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.draw_arrow_error);

        for (int i = 0; i < 20; i++) {
            for (int j = 0; j < 20; j++) {
                IMAGE_ID_LIST.get(i)[j] = "";
                OBSTACLE_BEARING_LIST.get(i)[j] = "";
            }
        }

        // update om
        om.clearObstacles();

        showLog("Exiting resetMap");
        this.invalidate();
    }

    public boolean isRobotOnMap(){
        return (curCoord[0] != -1 && curCoord[1] != -1);
    }
    public void updateMapInformation() throws JSONException{
        showLog("Entering updateMapInformation");
        JSONObject mapInformation = this.getReceivedJsonObject();
        showLog("updateMapInformation --- mapInformation: " + mapInformation);
        JSONArray infoJsonArray;
        JSONObject infoJsonObject;
        String hexStringExplored, hexStringObstacle, exploredString, obstacleString;
        BigInteger hexBigIntegerExplored, hexBigIntegerObstacle;
        String message;

        if (mapInformation == null) {
            return;
        }

        for(int i=0; i<mapInformation.names().length(); i++) {
            message = "updateMapInformation Default message";
            switch (mapInformation.names().getString(i)) {
                case "map":
                    infoJsonArray = mapInformation.getJSONArray("map");
                    infoJsonObject = infoJsonArray.getJSONObject(0);

                    hexStringExplored = infoJsonObject.getString("explored");
                    hexBigIntegerExplored = new BigInteger(hexStringExplored, 16);
                    exploredString = hexBigIntegerExplored.toString(2);
                    showLog("updateMapInformation.exploredString: " + exploredString);

                    int x, y;
                    for (int j = 0; j < exploredString.length() - 4; j++) {
                        y = 19 - (j / 15);
                        x = 1 + j - ((19 - y) * 15);
                        if ((String.valueOf(exploredString.charAt(j + 2))).equals("1") && !cells[x][y].type.equals("robot")) {
                            cells[x][y].setType("explored");
                        }
                        else if ((String.valueOf(exploredString.charAt(j + 2))).equals("0") && !cells[x][y].type.equals("robot")) {
                            cells[x][y].setType("unexplored");
                        }
                    }

                    int length = infoJsonObject.getInt("length");

                    hexStringObstacle = infoJsonObject.getString("obstacle");
                    showLog("updateMapInformation hexStringObstacle: " + hexStringObstacle);
                    hexBigIntegerObstacle = new BigInteger(hexStringObstacle, 16);
                    showLog("updateMapInformation hexBigIntegerObstacle: " + hexBigIntegerObstacle);
                    obstacleString = hexBigIntegerObstacle.toString(2);
                    while (obstacleString.length() < length) {
                        obstacleString = "0" + obstacleString;
                    }
                    showLog("updateMapInformation obstacleString: " + obstacleString);
                    setPublicMDFExploration(hexStringExplored);
                    setPublicMDFObstacle(hexStringObstacle);

                    int k = 0;
                    for (int row = ROW - 1; row >= 0; row--) {
                        for (int col = 1; col <= COL; col++) {
                            if ((cells[col][row].type.equals("explored") || (cells[col][row].type.equals("robot"))) && k < obstacleString.length()) {
                                if ((String.valueOf(obstacleString.charAt(k))).equals("1")) {
                                    this.setObstacleCoord(col, 20 - row);
                                }
                                k++;
                            }
                        }
                    }
                    break;

                case "robotPosition":
                    if (canDrawRobot) {
                        this.setOldRobotCoord(curCoord[0], curCoord[1]);
                    }

                    infoJsonArray = mapInformation.getJSONArray("robotPosition");

                    for (int row = ROW - 1; row >= 0; row--) {
                        for (int col = 1; col <= COL; col++) {
                            cells[col][row].setType("unexplored");
                        }
                    }
                    String direction;
                    if (infoJsonArray.getInt(2) == 90) {
                        direction = "East";
                    }
                    else if (infoJsonArray.getInt(2) == 180) {
                        direction = "South";
                    }
                    else if (infoJsonArray.getInt(2) == 270) {
                        direction = "West";
                    }
                    else {
                        direction = "North";
                    }

                    this.setStartCoord(infoJsonArray.getInt(0), infoJsonArray.getInt(1));
                    this.setCurCoord(infoJsonArray.getInt(0)+2, convertRow(infoJsonArray.getInt(1))-1, direction);
                    canDrawRobot = true;
                    break;

                case "obstacle":
                    infoJsonArray = mapInformation.getJSONArray("obstacle");
                    for (int j = 0; j < infoJsonArray.length(); j++) {
                        infoJsonObject = infoJsonArray.getJSONObject(j);
                        this.setObstacleCoord(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"));
                    }
                    message = "No. of Obstacle: " + String.valueOf(infoJsonArray.length());
                    break;

                case "arrow":
                    infoJsonArray = mapInformation.getJSONArray("arrow");
                    for (int j = 0; j < infoJsonArray.length(); j++) {
                        infoJsonObject = infoJsonArray.getJSONObject(j);
                        if (!infoJsonObject.getString("face").equals("pseudo")) {
                            this.setArrowCoordinate(infoJsonObject.getInt("x"), infoJsonObject.getInt("y"), infoJsonObject.getString("face"));
                            message = "Arrow:  (" + String.valueOf(infoJsonObject.getInt("x")) + "," + String.valueOf(infoJsonObject.getInt("y")) + "), face: " + infoJsonObject.getString("face");
                        }
                    }
                    break;

                case "move":
                    infoJsonArray = mapInformation.getJSONArray("move");
                    infoJsonObject = infoJsonArray.getJSONObject(0);
                    if (canDrawRobot) {
                        moveRobot(infoJsonObject.getString("direction"));
                    }
                    message = "moveDirection: " + infoJsonObject.getString("direction");
                    break;

                case "status":
                    String text = mapInformation.getString("status");
                    printRobotStatus(text);
                    message = "status: " + text;
                    break;

                default:
                    message = "Unintended default for JSONObject";
                    break;
            }
            if (!message.equals("updateMapInformation Default message")) {
                MainActivity.receiveMessage(message);
            }
        }
        showLog("Exiting updateMapInformation");
        this.invalidate();
    }

    public void moveRobot(String direction) {
        showLog("Entering moveRobot");
        setValidPosition(false);
        int[] curCoord = this.getCurCoord();

        if(curCoord[0] == -1 || curCoord[1] == -1)
            return;

        ArrayList<int[]> obstacleCoord = this.getObstacleCoord();
        this.setOldRobotCoord(curCoord[0], curCoord[1]);
        int[] oldCoord = this.getOldRobotCoord();
        String robotDirection = getRobotBearing();
        String backupDirection = robotDirection;

        switch (robotDirection) {
            case "North":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;

                    case "right":
                        robotDirection = "East";
                        break;

                    case "back":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;

                    case "left":
                        robotDirection = "West";
                        break;

                    default:
                        robotDirection = "ERROR NORTH CASE";
                        break;
                }
                break;

            case "East":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 14) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;

                    case "right":
                        robotDirection = "South";
                        break;

                    case "back":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;
                    case "left":
                        robotDirection = "North";
                        break;
                    default:
                        robotDirection = "ERROR EAST CASE";
                }
                break;

            case "South":
                switch (direction) {
                    case "forward":
                        if (curCoord[1] != 2) {
                            curCoord[1] -= 1;
                            validPosition = true;
                        }
                        break;

                    case "right":
                        robotDirection = "West";
                        break;

                    case "back":
                        if (curCoord[1] != 19) {
                            curCoord[1] += 1;
                            validPosition = true;
                        }
                        break;

                    case "left":
                        robotDirection = "East";
                        break;

                    default:
                        robotDirection = "ERROR SOUTH CASE";
                }
                break;

            case "West":
                switch (direction) {
                    case "forward":
                        if (curCoord[0] != 2) {
                            curCoord[0] -= 1;
                            validPosition = true;
                        }
                        break;

                    case "right":
                        robotDirection = "North";
                        break;

                    case "back":
                        if (curCoord[0] != 14) {
                            curCoord[0] += 1;
                            validPosition = true;
                        }
                        break;

                    case "left":
                        robotDirection = "South";
                        break;

                    default:
                        robotDirection = "ERROR WEST CASE";
                }
                break;

            default:
                robotDirection = "ERROR MOVE ROBOT";
                break;
        }

        if (getValidPosition())
            for (int x = curCoord[0] - 1; x <= curCoord[0] + 1; x++) {
                for (int y = curCoord[1] - 1; y <= curCoord[1] + 1; y++) {
                    for (int i = 0; i < obstacleCoord.size(); i++) {
                        if (obstacleCoord.get(i)[0] != x || obstacleCoord.get(i)[1] != y)
                            setValidPosition(true);
                        else {
                            setValidPosition(false);
                            break;
                        }
                    }
                    if (!getValidPosition())
                        break;
                }
                if (!getValidPosition())
                    break;
            }
        if (getValidPosition())
            this.setCurCoord(curCoord[0], curCoord[1], robotDirection);
        else {
            if (direction.equals("forward") || direction.equals("back"))
                robotDirection = backupDirection;
            this.setCurCoord(oldCoord[0], oldCoord[1], robotDirection);
        }
        this.invalidate();
        showLog("Exiting moveRobot");
    }

    public void printRobotStatus(String message) {
        TextView robotStatusTextView = ((Activity)this.getContext()).findViewById(R.id.robotStatusTextView);
        robotStatusTextView.setText(message);
    }

    public static void setPublicMDFExploration(String message) {
        publicMDFExploration = message;
    }

    public static void setPublicMDFObstacle(String message) {
        publicMDFObstacle = message;
    }

    private static class MyDragShadowBuilder extends DragShadowBuilder {
        private Point mScaleFactor;

        // Defines the constructor for myDragShadowBuilder
        public MyDragShadowBuilder(View v) {
            // Stores the View parameter passed to myDragShadowBuilder.
            super(v);
        }

        // Defines a callback that sends the drag shadow dimensions and touch point back to the
        // system.
        @Override
        public void onProvideShadowMetrics (Point size, Point touch) {
            // Defines local variables
            int width;
            int height;

            // Sets the width of the shadow to half the width of the original View
            width = (int) (cells[1][1].endX - cells[1][1].startX);

            // Sets the height of the shadow to half the height of the original View
            height = (int) (cells[1][1].endY - cells[1][1].startY);

            // Sets the size parameter's width and height values. These get back to the system
            // through the size parameter.
            size.set(width, height);
            // Sets size parameter to member that will be used for scaling shadow image.
            mScaleFactor = size;

            // Sets the touch point's position to be in the middle of the drag shadow
            touch.set(width / 2, height / 2);
        }

        @Override
        public void onDrawShadow(Canvas canvas) {
            // Draws the ColorDrawable in the Canvas passed in from the system.
            canvas.scale(mScaleFactor.x/(float)getView().getWidth(), mScaleFactor.y/(float)getView().getHeight());
            getView().draw(canvas);
        }

    }

    // week 8 req to update robot pos when alg sends updates
    public void updateRobotPositionFromAlgo(int x, int y, String direction) {
        showLog("Enter updateRobotPositionFromAlgo");
        showLog("x = " + x + "\n" + "y = " + y);
        if ((x > 1 && x < 21) && (y > -1 && y < 20)) {
            showLog("within grid");
            robotBearing = (robotBearing.equals("None")) ? "North" : robotBearing;
            switch (direction) {
                case "90":
                    robotBearing = "North";
                    break;

                case "270":
                    robotBearing = "South";
                    break;

                case "0":
                    robotBearing = "East";
                    break;

                case "180":
                    robotBearing = "West";
                    break;
            }
        }

        //If robot has no initial position, do not set as explored before moving to new coord
        if (!(curCoord[0] == -1 && curCoord[1] == -1)) {
            showLog("If robot was not at invalid position previously");
            if ((curCoord[0] > 1 && curCoord[0] < 21) && (curCoord[1] > -1 && curCoord[1] < 20)) {
                showLog("Robot previous position was within grid");
                for (int i = curCoord[0] - 1; i <= curCoord[0] + 1; i++) {
                    for (int j = curCoord[1] - 2; j <= curCoord[1]; j++) {
                        cells[i][20 - j - 1].setType("explored");
                    }
                }
            }
        }

        //If robot is still within grid
        if ((x > 1 && x < 21) && (y > -1 && y < 20) ) {
            showLog("Robot is within grid");
            setCurCoord(x+1, y+1, robotBearing);
            canDrawRobot = true;
        }

        //If robot goes out of grid
        else {
            showLog("set canDrawRobot to false");
            canDrawRobot = false;
            curCoord[0] = -1;
            curCoord[1] = -1;
        }

        this.invalidate();
        showLog("Exit updateRobotPositionFromAlgo");
    }

    public void moveRobotFromAlgo(int x, int y, String facing, String command) {
        showLog("Enter moveRobotFromAlgo");
        final int i = y;
        final int j = x;
        final String finalFacing = facing;
        int delay = 500;   //Can add 1000 after each run

        if ((x > 1 && x < 21) && (y > -1 && y < 20)) {
            showLog("Robot is within grid");
            robotBearing = (robotBearing.equals("None")) ? "North" : robotBearing;

            //Do the following only if robot is within grid
            if (curCoord[0] != -1 && curCoord[1] != -1) {
                showLog("Robot is within grid");
                switch (robotBearing) {
                    case "North":
                        switch (command) {
                            case "fl":
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 4, i - 1, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                // turn
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 3, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                // rest of the forward motion
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 2, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 1, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;

                            case "fr":
                                // move forward 1 grid
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 4, i - 1, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                // turn
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 3, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                // rest of the forward motion
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 2, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 1, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;

                            case "rl":
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 2, i + 3, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 2, i + 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 2, i + 1, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 1, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;

                            case "rr":
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 2, i + 3, robotBearing);
                                    }
                                }, delay);
                                delay += 500;
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 2, i + 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                // rest of the forward motion
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 2, i + 1, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 1, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;
                        }
                        break;

                    case "South":
                        switch (command) {
                            case "fl":
                                // move forward 1 grid
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 4, i + 1, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                // turn
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 3, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                // move 1 grid forward
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 2, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 1, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;

                            case "fr":
                                // move forward 1 grid
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 4, i + 1, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                // turn
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 3, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                // move 1 grid forward
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 2, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 1, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;

                            case "rl":
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 2, i - 3, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 2, i - 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 2, i - 1, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 1, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;

                            case "rr":
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 2, i - 3, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 2, i - 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 2, i - 1, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 1, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;
                        }
                        break;

                    case "East":
                        switch (command) {
                            case "fl":
                                // move forward 1 grid
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 1, i - 4, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                // turn
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i - 3, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                // move 1 grid forward
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i - 2, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i - 1, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;

                            case "fr":
                                // move forward 1 grid
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 1, i + 4, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                // turn
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i + 3, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                // move 1 grid forward
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i + 2, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i + 1, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;

                            case "rl":
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 3, i - 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 2, i - 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 1, i - 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i - 1, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;

                            case "rr":
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 3, i + 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 2, i + 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 1, i + 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i + 1, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;
                        }
                        break;

                    case "West":
                        switch (command) {
                            case "fl":
                                // move forward 1 grid
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 1, i + 4, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                // turn
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i + 3, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                // move 1 grid forward
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i + 2, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i + 1, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;
                                break;

                            case "fr":
                                // move forward 1 grid
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j + 1, i - 4, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                // turn
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i - 3, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                // move 1 grid forward
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i - 2, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i - 1, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                delay += 500;
                                break;

                            case "rl":
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 3, i + 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 2, i + 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 1, i + 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i + 1, finalFacing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;

                            case "rr":
                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 3, i - 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 2, i - 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j - 1, i - 2, robotBearing);
                                    }
                                }, delay);
                                delay += 500;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i - 1, finalFacing);
                                    }
                                }, delay);
                                delay += 1000;

                                new Handler().postDelayed(new Runnable() {
                                    public void run() {
                                        updateRobotPositionFromAlgo(j, i, finalFacing);
                                    }
                                }, delay);
                                break;
                        }
                        break;
                }
            }
            //If robot was out of grid in previous state
            else {
                showLog("Robot was out of grid in previous state");
                updateRobotPositionFromAlgo(x, y, facing);
            }
        }

        //If robot goes out of grid
        else {
            //Change cells to explored before erasing robot
            if (!(curCoord[0] == -1 && curCoord[1] == -1)) {
                showLog("If robot is already within grid");
                if ((curCoord[0] > 1 && curCoord[0] < 21) && (curCoord[1] > -1 && curCoord[1] < 20)) {
                    showLog("Robot position is good before going out of grid");
                    for (int n = curCoord[0] - 1; n <= curCoord[0]; n++) {
                        for (int m = curCoord[1] - 1; m <= curCoord[1]; m++) {
                            cells[n][20 - m - 1].setType("explored");
                        }
                    }
                }
            }
            showLog("Setting canDrawRobot to false");
            canDrawRobot = false;
            curCoord[0] = -1;
            curCoord[1] = -1;
        }
        this.invalidate();
        showLog("Exit moveRobotFromAlgo");
    }

    //Week 8 task to send ALGO obstacle info
    public String getObstacles() {
        String message = "ALG|";

        for (int i = 0; i < obstacleCoord.size(); i++) {
            showLog("i = " + Integer.toString(i));
            message +=  (Float.toString((float) (obstacleCoord.get(i)[0] + 0.5)) + "," + Float.toString((float) (obstacleCoord.get(i)[1] + 0.5)) + ","
                    + OBSTACLE_BEARING_LIST.get(obstacleCoord.get(i)[1])[obstacleCoord.get(i)[0]].charAt(0) + ";");
        }
        message += "\n";
        return message;
    }

    //Checklist
    public String commandMessageGenerator(int command) {
        String message = "";
        switch (command) {
            case ADD_OBSTACLE:
                // format: <target component>|<command>,<image id>,<obstacle coord>,<face direction>
                message = "ADD_OBSTACLE,"
                        + IMAGE_ID_LIST.get(initialRow - 1)[initialColumn - 1] + ","
                        + "(" + (initialColumn - 1) + "," + (initialRow - 1) + "),"
                        + OBSTACLE_BEARING_LIST.get(initialRow - 1)[initialColumn - 1].charAt(0) + "\n";
                break;

            case REMOVE_OBSTACLE:
                // format: <target component>|<command>,<image id>,<obstacle coord>
                message = "REMOVE_OBSTACLE,"
                        + oldItem + ","
                        + "(" + (initialColumn - 1) + "," + (initialRow - 1) + ")" + "\n";
                break;

            case MOVE_OBSTACLE:
                // format: <target component>|<command>,<old coord>,<new coord>
                message = "MOVE_OBSTACLE,"
                        + "(" + (initialColumn - 1) + "," + (initialRow - 1) + "),"
                        + "(" + (endColumn - 1) + "," + (endRow - 1) + ")" + "\n";
                break;

            case ROBOT_MOVING:
                // format: <target component>|<command>,<string>
                message = "Message," + "Move robot" + "\n";
                break;

            case START_IMAGE_RECOGNITION:
                // format: <target component>|<command>,<string>
                message = "Message," + "Start image recognition mode." + "\n";
                break;

            case START_FASTEST_CAR:
                // format: <target component>|<command>,<string>
                message = "Message," + "Start fastest car mode." + "\n";
        }
        return message;
    }

    //Week 8 task
    public boolean updateObstaclesFromRPI(String obstacleID, String imageID) {
        showLog("starting updateObstaclesFromRPI");
        // old way
//        int x = obstacleCoord.get(Integer.parseInt(obstacleID) - 1)[0];
//        int y = obstacleCoord.get(Integer.parseInt(obstacleID) - 1)[1];
//        IMAGE_ID_LIST.get(y)[x] = (imageID.equals("-1")) ? "" : imageID;
//        this.invalidate();
        //

        // legit way
        om.updateObstacle(obstacleID,"","","",imageID);
        Obstacle temp = om.getObstacle(obstacleID);
        if(temp != null){
            int y = Integer.parseInt(temp.y)+1;
            int x = Integer.parseInt(temp.x)+1;
            IMAGE_ID_LIST.get(y - 1)[x - 1] = imageID;
            temp.imageID = imageID;

            this.invalidate();
            return true;
        }else{
            Log.e("sadflksf", "obstacle " + obstacleID + " does not exist! Cannot update.");
            return false;
        }


        //

    }
}