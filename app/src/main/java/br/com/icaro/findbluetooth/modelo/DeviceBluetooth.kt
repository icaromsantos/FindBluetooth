package br.com.icaro.findbluetooth.modelo

import android.bluetooth.BluetoothDevice

class DeviceBluetooth (
    val name: String,
    val macaddress:String,
    val isConnected:Boolean?,
    val ligarFunc:(()->Unit)?,
    val gravarVoz:(()->Unit)?
)