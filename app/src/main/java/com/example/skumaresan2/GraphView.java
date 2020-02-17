package com.example.skumaresan2;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * GraphView creates a scaled line or bar graph1 with x and y axis labels.
 * @author Arno den Hond
 *
 */
public class GraphView extends View {

    private static boolean BAR = false; //Draw a line graph1, NOT bar graph1.
    private Paint paint;
    private float[] values;
    private String[] horlabels;
    private String[] verlabels;
    private String title;
    private boolean type;


    public GraphView(Context context) {
        this(context, null, null,null, null, false, null);
    }


    public GraphView(Context context, @Nullable AttributeSet attrs) {
        this(context, null, null,null, null, false, attrs);
    }


    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, null, null,null, null, false, attrs);
    }


    public GraphView(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        this(context, null, null,null, null, false, attrs);
    }


    public GraphView(Context context, float[] values, String title, String[] horlabels, String[] verlabels, boolean type, @Nullable AttributeSet attrs) {
        super(context, attrs);

        //Define initial values1 for the graph1
        this.title="";
        this.paint=new Paint();
        this.values=new float[]{0,0,0,0,0,0,0,0,0,0};
        this.horlabels = new String[]{"",  "", "", "", "", "", "", "", "", "0"};
        this.verlabels = new String[]{"250","200", "150", "100", "50", "0"};
        this.type=true;

        paint = new Paint();
        setBackgroundColor(Color.BLACK);
    }

    public void setValues(float[] newValues){
        this.values = newValues;
    }

    public float[] getValues(){
        return (this.values);
    }

    public void setHorlabels(String[] horlabels){
        this.horlabels = horlabels;
    }

    public void setTitle (String title) { this.title = title;}

    @Override
    protected void onDraw(Canvas canvas) {

        float border = 20;
        float horstart = border * 2;
        float height = getHeight();
        float width = getWidth() - 1;
        float max = getMax();
        float min = getMin();
        float diff = max - min;

        float graphheight = height - (2 * border);
        float graphwidth = width - (2 * border);


        setBackgroundColor(Color.BLACK); //Set background color to Black
        paint.setTextAlign(Align.LEFT);
        paint.setStrokeWidth(5.0f); //Increase store width

        int vers = verlabels.length - 1;
        for (int i = 0; i < verlabels.length; i++) {
            paint.setColor(Color.DKGRAY);
            float y = ((graphheight / vers) * i) + border;
            canvas.drawLine(horstart, y, width, y, paint);
            paint.setColor(Color.WHITE);
            paint.setTextSize(30f);
            canvas.drawText(verlabels[i], 0, y, paint);
        }
        int hors = horlabels.length - 1;
        for (int i = 0; i < horlabels.length; i++) {
            paint.setColor(Color.DKGRAY);
            float x = ((graphwidth / hors) * i) + horstart;
            canvas.drawLine(x, height - border, x, border, paint);
            paint.setTextAlign(Align.CENTER);
            if (i==horlabels.length-1)
                paint.setTextAlign(Align.RIGHT);
            if (i==0)
                paint.setTextAlign(Align.LEFT);
            paint.setColor(Color.WHITE);
            canvas.drawText(horlabels[i], x, height - 4, paint);
        }

        paint.setTextAlign(Align.CENTER);
        canvas.drawText(title, (graphwidth / 2) + horstart, border - 4, paint);

        if (max != min) {
            paint.setColor(Color.LTGRAY);
            if (type == BAR) {
                float datalength = values.length;
                float colwidth = (width - (2 * border)) / datalength;
                for (int i = 0; i < values.length; i++) {
                    float val = values[i] - min;
                    float rat = val / diff;
                    float h = graphheight * rat;
                    canvas.drawRect((i * colwidth) + horstart, (border - h) + graphheight, ((i * colwidth) + horstart) + (colwidth - 1), height - (border - 1), paint);
                }
            } else {
                float datalength = values.length;
                float colwidth = (width - (2 * border)) / datalength;
                float halfcol = colwidth / 2;
                float lasth = 0;
                for (int i = 0; i < values.length; i++) {
                    float val = values[i] - min;
                    float rat = val / diff;
                    float h = graphheight * rat;
                    //if (i > 0)
                        paint.setColor(Color.GREEN);

                    canvas.drawLine(((i - 1) * colwidth) + (horstart + 1) + halfcol, (border - lasth) + graphheight, (i * colwidth) + (horstart + 1) + halfcol, (border - h) + graphheight, paint);
                    lasth = h;
                }
            }
        }
    }

    private float getMax() {
        float largest = Integer.MIN_VALUE;
        for (int i = 0; i < values.length; i++)
            if (values[i] > largest)
                largest = values[i];
        return largest;
    }

    private float getMin() {
        float smallest = Integer.MAX_VALUE;
        for (int i = 0; i < values.length; i++)
            if (values[i] < smallest)
                smallest = values[i];
        return smallest;
    }

}