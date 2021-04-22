package com.ybdev.facerecognition.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ybdev.facerecognition.R;

import java.util.ArrayList;

public class MatchAdapter extends RecyclerView.Adapter<MatchAdapter.ViewHolder>{


    private final Context context;
    private  ArrayList<String> arrayList;

    public MatchAdapter(Context context) {
        this.context = context;
        arrayList = new ArrayList<>();
    }

    @NonNull
    @Override
    public MatchAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.match_row, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MatchAdapter.ViewHolder holder, int position) {
        holder.text.setText(arrayList.get(position));
    }


    public void clearArrayList(){arrayList.clear();}

    public void setArrayList(ArrayList<String> array){
        arrayList.addAll(array);
    }

    private String getItem(int position) {
        return arrayList.get(position);
    }
    @Override
    public int getItemCount() {
        return arrayList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {


        TextView text;

        ViewHolder(View view) {
            super(view);

            //find views
            text = view.findViewById(R.id.text);

        }
    }
}