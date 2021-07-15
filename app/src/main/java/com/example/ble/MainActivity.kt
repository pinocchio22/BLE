package com.example.ble

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ToggleButton

class MainActivity : AppCompatActivity() {
    private val REQUEST_ENABLE_BT = 1
    private var bluetoothAdapter: BluetoothAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val bleOnOffBtn: ToggleButton = findViewById(R.id.ble_on_off_btn)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if(bluetoothAdapter!=null){
            // Device doesn't support Bluetooth
            bleOnOffBtn.isChecked = bluetoothAdapter?.isEnabled==false
        }

        bleOnOffBtn.setOnCheckedChangeListener { _, isChecked ->
            bluetoothOnOff()
        }

    }
    private fun bluetoothOnOff(){
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.d("bluetoothAdapter","Device doesn't support Bluetooth")
        }else{
            if (bluetoothAdapter?.isEnabled == false) { // 블루투스 꺼져 있으면 블루투스 활성화
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else{ // 블루투스 켜져있으면 블루투스 비활성화
                bluetoothAdapter?.disable()
            }
        }
    }
}