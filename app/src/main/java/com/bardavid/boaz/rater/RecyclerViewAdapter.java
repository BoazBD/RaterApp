package com.bardavid.boaz.rater;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

//import com.example.boaz.rater.R;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.squareup.picasso.Callback;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> {
    List<ImageRates> images;
    Context context;
    int ratingsCount;
    public RecyclerViewAdapter(Context ct, List<ImageRates> images, int ratingsCount){
        context=ct;
        this.images=images;
        this.ratingsCount=ratingsCount;
    }
    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater=LayoutInflater.from(context);
        View view=inflater.inflate(R.layout.activity_chart_template,parent,false);
        return new MyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Picasso.get().load(images.get(position).getUrl()).into(holder.image, new Callback() {
            @Override
            public void onSuccess() {
                if (ratingsCount < images.size() * Prefs.NUMBER_OF_RATINGS_NEEDED_FOR_EACH_PHOTO) {
                    //User did'nt rate enough
                    holder.progressBar.setVisibility(View.INVISIBLE);
                    holder.averageTxt.setVisibility(View.INVISIBLE);
                    holder.ratingsProcessText.setText("To view ratings, rate " + (images.size() * Prefs.NUMBER_OF_RATINGS_NEEDED_FOR_EACH_PHOTO - ratingsCount) + " more photos");
                    holder.chart.setVisibility(View.INVISIBLE);
                    holder.questionmark.setVisibility(View.VISIBLE);
                    holder.numVotes.setVisibility(View.INVISIBLE);
                    holder.ratingsProcessText.setVisibility(View.VISIBLE);
                    holder.goRateBtn.setVisibility(View.VISIBLE);
                } else if (images.get(position).getNumRates() == 0) {
                    //User rated enough but the photo does'nt have ratings yet
                    holder.progressBar.setVisibility(View.INVISIBLE);
                    holder.averageTxt.setVisibility(View.VISIBLE);
                    holder.averageTxt.setText("Average rate: ?");
                    holder.ratingsProcessText.setText("Photo is currently being rated by other people, come back later!");
                    holder.chart.setVisibility(View.INVISIBLE);
                    holder.questionmark.setVisibility(View.VISIBLE);
                    holder.numVotes.setVisibility(View.INVISIBLE);
                    holder.ratingsProcessText.setVisibility(View.VISIBLE);
                } else {
                    //User rated enough, reveal rating chart
                    holder.progressBar.setVisibility(View.INVISIBLE);
                    holder.averageTxt.setVisibility(View.VISIBLE);
                    holder.chart.setVisibility(View.VISIBLE);
                    holder.numVotes.setVisibility(View.VISIBLE);
                    holder.ratesTxt.setVisibility(View.VISIBLE);
                    holder.ratingsProcessText.setText("Photo is currently being rated by other people, come back later for more votes!");
                    holder.ratingsProcessText.setVisibility(View.VISIBLE);
                    setPhotoAverageRate(holder, images.get(position).getRates());
                    plotChart(holder, images.get(position).getRates());
                }
            }

            @Override
            public void onError(Exception e) {
                Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void setPhotoAverageRate(MyViewHolder holder, List<Integer> rates){
        int numVotes=0,sumVotes=0;
        for(int i=1;i<rates.size();i++){
            numVotes+= rates.get(i);
            sumVotes+= rates.get(i)*(i);
        }
        float average=(float)sumVotes/numVotes;
        holder.averageTxt.setText("Average rate: "+String.format(java.util.Locale.US,"%.2f", average));
    }
    public void plotChart(MyViewHolder holder,List<Integer> rates) {
        ArrayList<BarEntry> barEntries=new ArrayList<>();
        for(int i=1;i<11;i++){
            barEntries.add(new BarEntry(i,rates.get(i)));
        }
        BarChart chart= holder.chart;
        BarDataSet barDataSet=new BarDataSet(barEntries,"rates");
        barDataSet.setColor(Color.rgb(0,0,100));
        barDataSet.setDrawValues(false);

        BarData theData= new BarData(barDataSet);

        chart.setData(theData);
        //hiding the grey background of the chart, default false if not set
        chart.setDrawGridBackground(false);
        //remove the bar shadow, default false if not set
        chart.setDrawBarShadow(false);
        //remove border of the chart, default false if not set
        chart.setDrawBorders(false);

        //remove the description label text located at the lower right corner
        chart.getDescription().setEnabled(false);


        //change the position of x-axis to the bottom
        chart.getXAxis().setPosition(chart.getXAxis().getPosition().BOTTOM);
        //set the horizontal distance of the grid line
        chart.getAxisLeft().setGranularity(1);
        chart.getAxisLeft().setGranularityEnabled(true);

        //hiding the x-axis line, default true if not set
        chart.getXAxis().setDrawAxisLine(false);
        chart.getAxisLeft().setDrawGridLines(false);
        chart.getAxisRight().setDrawGridLines(false);
        //hiding the grid lines, default true if not set
        chart.getXAxis().setDrawGridLines(false);

        //hiding the left y-axis line, default true if not set
        chart.getAxisLeft().setDrawAxisLine(false);
        chart.getLegend().setEnabled(false);
        chart.getXAxis().setLabelCount(10);
        //hiding the right y-axis line, default true if not set
        chart.getAxisRight().setDrawAxisLine(false);
        chart.getAxisRight().setDrawLabels(false);
        chart.animateY(1500);
    }
    @Override
    public int getItemCount() {
        return images.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        private ImageView image;
        private ImageView numVotes, questionmark;
        private TextView emptyView,averageTxt;
        private BarChart chart;
        private Button goRateBtn;
        private TextView ratingsProcessText,ratesTxt;
        private ProgressBar progressBar;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.image);
            numVotes = itemView.findViewById(R.id.numVotes);
            emptyView = itemView.findViewById(R.id.emptyView);
            chart = itemView.findViewById(R.id.chart);
            questionmark=itemView.findViewById(R.id.questionmark);
            averageTxt=itemView.findViewById(R.id.averageTxt);
            ratingsProcessText=itemView.findViewById(R.id.ratingsProcessText);
            ratesTxt=itemView.findViewById(R.id.ratesTxt);
            progressBar=itemView.findViewById(R.id.progress_bar);

            goRateBtn=itemView.findViewById(R.id.goRateBtn);
            goRateBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(context, RateActivity.class);
                    context.startActivity(intent);
                }
            });

        }
    }
}
