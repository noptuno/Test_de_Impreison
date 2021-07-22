package com.example.test_de_impreison.Adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.example.test_de_impreison.Clase.ProductoDemo;
import com.example.test_de_impreison.R;

import java.util.ArrayList;
import java.util.List;

public class AdapterProductoDemo extends RecyclerView.Adapter<AdapterProductoDemo.NoteViewHolder> {

    private List<ProductoDemo> notes;
    private OnNoteSelectedListener onNoteSelectedListener;
    private OnNoteDetailListener onDetailListener;

    public AdapterProductoDemo() {
        this.notes = new ArrayList<>();
    }

    public AdapterProductoDemo(List<ProductoDemo> notes) {
        this.notes = notes;
    }


    @Override
    public NoteViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View elementoTitular = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note_productodemo, parent, false);

        return new NoteViewHolder(elementoTitular);
    }

    @Override
    public void onBindViewHolder(NoteViewHolder view, int pos) {
        view.bind(notes.get(pos));
    }

    @Override
    public int getItemCount() {
        return notes.size();
    }

    public List<ProductoDemo> getNotes() {
        return notes;
    }

    public void setNotes(List<ProductoDemo> notes) {
        this.notes = notes;
    }

    public void setOnNoteSelectedListener(OnNoteSelectedListener onNoteSelectedListener) {
        this.onNoteSelectedListener = onNoteSelectedListener;
    }

    public void setOnDetailListener(OnNoteDetailListener onDetailListener) {
        this.onDetailListener = onDetailListener;
    }

    public interface OnNoteSelectedListener {
        void onClick(ProductoDemo note);
    }

    public interface OnNoteDetailListener {
        void onDetail(ProductoDemo note);
    }

    public class NoteViewHolder extends RecyclerView.ViewHolder {
        private TextView codigoean;
        private TextView codigoprincipal;
        private TextView descripcion;
        private TextView precio;


        public NoteViewHolder(View item) {
            super(item);

            codigoprincipal = (TextView) item.findViewById(R.id.txt_codigoproducto);
            codigoean = (TextView) item.findViewById(R.id.txt_codigoean);
            descripcion = (TextView) item.findViewById(R.id.txt_descripcion);
            precio= (TextView) item.findViewById(R.id.txt_precio);

        }

        public void bind(final ProductoDemo producto) {


            codigoprincipal.setText(producto.getCodigoProducto().toString());
            codigoean.setText(producto.getCodigoEan().toString());
            descripcion.setText(producto.getDescripcion().toString());
            precio.setText(producto.getPrecio());
            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (onNoteSelectedListener != null) {
                        onNoteSelectedListener.onClick(producto);
                    }
                }
            });
        }
    }
}
