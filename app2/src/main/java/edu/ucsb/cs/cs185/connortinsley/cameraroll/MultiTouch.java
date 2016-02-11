package edu.ucsb.cs.cs185.connortinsley.cameraroll;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.AttributeSet;
import android.util.FloatMath;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import java.util.ArrayList;

public class MultiTouch extends ActionBarActivity {

    public PointF startPoint= new PointF();
    public PointF midPoint = new PointF();
    public float startDist = 1f;
    public int touchState = 0;
    public boolean previousTouch = false;
    public float rotation = 0f;
    public TouchView myTouchView;
    public int flag = 0;
    public Bitmap goodBitmap;
    public ArrayList<PointF> pointArray;
    public LinearLayout LL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //setup layout
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setDisplayShowTitleEnabled(false);
        pointArray = new ArrayList<PointF>();
        LL = new LinearLayout(this);
        LL.layout(0,0,0,0);
        LL.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        LL.setOrientation(LinearLayout.HORIZONTAL);
        myTouchView = new TouchView(this);
        LL.addView(myTouchView);
        setContentView(LL);
    }


    public class TouchView extends ImageView {
        Matrix goodMatrix = new Matrix();
        Matrix matrix2 = new Matrix();

        public TouchView(Context context, AttributeSet attrs, int defStyle){
            super(context, attrs, defStyle);
            this.setScaleType(ScaleType.MATRIX);
            Bundle extras = getIntent().getExtras();
            String imagePath = extras.getString("intentFile");
            setWillNotDraw(true);
            Bitmap myBitmap = BitmapFactory.decodeFile(imagePath);
            goodBitmap = myBitmap;
            this.setImageBitmap(myBitmap);
            this.setWillNotDraw(false);
        }

        public TouchView(Context context, AttributeSet attrs) {
            this(context, attrs, 0);
        }

        public TouchView(Context context){
            this(context, null, 0);
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec),
                    MeasureSpec.getSize(heightMeasureSpec));
        }

        @Override
        public void onLayout(boolean changed, int left, int top, int right, int bottom) {
            super.onLayout(changed,left,top,right,bottom);
        }

        @Override
        // This method gets called after every detection of motion on the screen
        public void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setColor(Color.argb(255, 255,0,0));

            //extracts the x and y coordinates of each point
            float[] floatArray = new float[pointArray.size()*2];
            int j = 0;
            for(int i = 0; i < pointArray.size()*2-1; i+=2) {
                floatArray[i] = pointArray.get(j).x;
                floatArray[i+1] = pointArray.get(j).y;
                ++j;
            }
                matrix2.mapPoints(floatArray);

                //scale the red dots proportional to the scale of the updated image
                for (int i = 0; i < pointArray.size() * 2 - 1; i += 2) {
                    float[] scaleVals = new float[9];
                    matrix2.getValues(scaleVals);

                    float scalex = scaleVals[Matrix.MSCALE_X];  //scale value of matrix
                    float skewy = scaleVals[Matrix.MSKEW_Y];    //rotation value of matrix
                    float realScale = (float) Math.sqrt(scalex * scalex + skewy * skewy);

                    canvas.drawCircle(floatArray[i], floatArray[i+1], 25f*realScale, paint);
                }
            myTouchView.setImageMatrix(matrix2);
        }

        @Override
        public boolean onTouchEvent(MotionEvent event){
            myTouchView.setScaleType(ScaleType.MATRIX);
            switch(event.getAction() & MotionEvent.ACTION_MASK) {

                //user taps the screen with one finger and records the location of the tap
                case MotionEvent.ACTION_DOWN:
                    flag = 0;
                    goodMatrix.set(matrix2);
                    startPoint.set(event.getX(), event.getY());
                    touchState = 1;
                    previousTouch = false;
                    break;

                //indicates user is no longer touching the screen and records the location of the initial tap if no movement has occured
                case MotionEvent.ACTION_UP:
                    if(flag == 0) {
                        float newX = startPoint.x;
                        float newY = startPoint.y;
                        float[] ptss = {newX,newY};

                        //update the matrix with the transformed points
                        Matrix inverse = new Matrix();
                        matrix2.invert(inverse);
                        inverse.mapPoints(ptss);

                        //add the location of the new red dot to the pointArray
                        PointF newPoint = new PointF(ptss[0],ptss[1]);
                        pointArray.add(newPoint);
                    }
                    break;

                //user taps screen a second time, find the distance between touches and the midpoint
                case MotionEvent.ACTION_POINTER_DOWN:
                    startDist = distanceBetweenTouches(event);
                    goodMatrix.set(matrix2);
                    midPoint(midPoint,event);
                    touchState = 2;
                    previousTouch = true;
                    rotation = rotation(event);
                    break;

                //user removes second finger from the screen and updates the state of the image
                case MotionEvent.ACTION_POINTER_UP:
                    touchState = 0;
                    previousTouch = false;
                    goodMatrix.set(matrix2);
                    break;

                //occurs when user moves either or both fingers across the screen
                case MotionEvent.ACTION_MOVE:
                    matrix2.set(goodMatrix);
                    flag = 1;

                    //The image will translate upon finger movement
                    if(touchState == 1) {
                        float xInv = event.getX() - startPoint.x;
                        float yInv = event.getY() - startPoint.y;
                        float[] pt = {xInv, yInv};
                        matrix2.mapPoints(pt);
                        matrix2.postTranslate(xInv, yInv);

                    //The image will scale upon movement of one or both fingers
                    } else if(touchState == 2) {
                        float endDist = distanceBetweenTouches(event);
                        float scale = endDist/startDist;
                        matrix2.postScale(scale,scale,midPoint.x,midPoint.y);
                    }

                    //The image will rotate upon movement of one or both fingers
                    if(touchState == 2 & previousTouch == true) {
                        float angle = rotation(event);
                        float diff = angle - rotation;
                        matrix2.postRotate(diff, myTouchView.getMeasuredWidth()/2, myTouchView.getMeasuredHeight()/2);
                    }
                    break;


            }

            invalidate();
            return true;
        }
    }
    private float distanceBetweenTouches(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return FloatMath.sqrt(x * x + y * y);
    }

    private void midPoint(PointF point, MotionEvent event) {
        float x = event.getX(0) + event.getX(1);
        float y = event.getY(0) + event.getY(1);
        point.set(x / 2, y / 2);
    }

    private float rotation(MotionEvent event) {
        double delta_x = (event.getX(0) - event.getX(1));
        double delta_y = (event.getY(0) - event.getY(1));
        double radians = Math.atan2(delta_y, delta_x);
        return (float) Math.toDegrees(radians);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_full_screen_image, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //tapping the home/back button brings the user back to the photo album
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        //tapping the load button brings up images stored on phone to be transformed
        if (id == R.id.load) {
            Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            startActivityForResult(photoPickerIntent, 1);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        //when load button is pressed, grab the image file of the image selected from the phone's sd card and restart the Intent
          if(resultCode == RESULT_OK) {
              Uri selectedImage = data.getData();
              String[] filePathColumn = {MediaStore.Images.Media.DATA};

              Cursor cursor = getContentResolver().query(selectedImage, filePathColumn, null, null, null);
              cursor.moveToFirst();

              int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
              String filePath = cursor.getString(columnIndex);
              cursor.close();

              Intent restartIntent = getIntent();
              restartIntent.putExtra("intentFile", filePath);
              finish();
              startActivity(restartIntent);
          }

    }

}

