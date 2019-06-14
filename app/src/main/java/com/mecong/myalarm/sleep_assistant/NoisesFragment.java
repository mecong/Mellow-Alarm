package com.mecong.myalarm.sleep_assistant;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mecong.myalarm.R;

import java.util.List;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link NoisesFragment.OnFragmentInteractionListener} interface
 * to handle interaction events.
 * Use the {@link NoisesFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoisesFragment extends Fragment implements NoisesItemViewAdapter.ItemClickListener {
    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "selectedPosition";
    int selectedPosition;
    NoisesItemViewAdapter adapter;
    // TODO: Rename and change types of parameters
    String mParam1;
    String mParam2;
    OnFragmentInteractionListener mListener;

    public NoisesFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment NoisesFragment.
     */
    // TODO: Rename and change types and number of parameters
    private static NoisesFragment newInstance(String param1, String param2) {
        NoisesFragment fragment = new NoisesFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            selectedPosition = getArguments().getInt(ARG_PARAM1);
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_noises, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {

        final RecyclerView recyclerView = view.findViewById(R.id.noisesList);

        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));
        adapter = new NoisesItemViewAdapter(view.getContext(),
                Noises.retrieveNoises(view.getContext()), selectedPosition);
        adapter.setClickListener(this);

        recyclerView.setAdapter(adapter);

    }

    // TODO: Rename method, update argument and hook method into UI event
    private void onNoiseSoundSelected(String uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(View view, int position) {
        onNoiseSoundSelected(adapter.getItem(position).getUrl());
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(String uri);
    }
}


@FieldDefaults(level = AccessLevel.PRIVATE)
class NoisesItemViewAdapter extends RecyclerView.Adapter<NoisesItemViewAdapter.ViewHolder> {

    int selectedPosition;
    List<Noises> mData;
    LayoutInflater mInflater;
    ItemClickListener mClickListener;

    // data is passed into the constructor
    NoisesItemViewAdapter(Context context, List<Noises> data, int selectedPosition) {
        this.selectedPosition = selectedPosition;
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
    }

    // inflates the row layout from xml when needed
    @Override
    @NonNull
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.fragment_noises_row, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the TextView in each row
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Noises item = mData.get(position);
        holder.headerText.setText(item.getName());
        holder.urlText.setText(item.getUrl());


        holder.itemView.setSelected(selectedPosition == position);

    }

    // total number of rows
    @Override
    public int getItemCount() {
        return mData.size();
    }

    // convenience method for getting data at click position
    Noises getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView headerText;
        TextView urlText;

        ViewHolder(View itemView) {
            super(itemView);
            headerText = itemView.findViewById(R.id.headerText);
            urlText = itemView.findViewById(R.id.urlText);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (getAdapterPosition() == RecyclerView.NO_POSITION) return;
            notifyItemChanged(selectedPosition);

            selectedPosition = getAdapterPosition();
            if (mClickListener != null) mClickListener.onItemClick(view, getAdapterPosition());
            notifyItemChanged(selectedPosition);
        }
    }
}