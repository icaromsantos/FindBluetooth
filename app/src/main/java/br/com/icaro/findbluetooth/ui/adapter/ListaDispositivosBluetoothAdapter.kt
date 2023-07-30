package br.com.icaro.findbluetooth.ui.adapter

import android.content.Context
import android.opengl.Visibility
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import br.com.icaro.findbluetooth.R
import br.com.icaro.findbluetooth.modelo.DeviceBluetooth


class ListaDispositivosBluetoothAdapter (private val context: Context,
private val devices: List<DeviceBluetooth>): RecyclerView.Adapter<ListaDispositivosBluetoothAdapter.ViewHolder>() {

    class ViewHolder(view: View): RecyclerView.ViewHolder(view) {

        fun vincula(device: DeviceBluetooth) {
            val deviceName = itemView.findViewById<TextView>(R.id.devicename)
            deviceName.text = device.name

            val macAddress = itemView.findViewById<TextView>(R.id.macaddress)
            macAddress.text = device.macaddress

            val ligar = itemView.findViewById<Button>(R.id.ligar)
            ligar.setVisibility(View.GONE);

            if(device.isConnected == true) {
                ligar.setVisibility(View.VISIBLE);
                ligar.setOnClickListener({
                    device.ligarFunc?.invoke()
                })
            }

            val gravar = itemView.findViewById<Button>(R.id.gravar)
            gravar.setVisibility(View.GONE);

            if(device.isConnected == true) {
                gravar.setVisibility(View.VISIBLE);
                gravar.setOnClickListener({
                    device.gravarVoz?.invoke()
                })
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val inflater = LayoutInflater.from(context)
        val view = inflater.inflate(R.layout.dispositivos_bluetooth,parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int  = devices.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val produto = devices[position];
        holder.vincula(produto)

    }


}