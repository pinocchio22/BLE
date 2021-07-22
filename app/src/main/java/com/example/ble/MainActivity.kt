package com.example.ble

import android.Manifest
import android.annotation.TargetApi
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ble.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private val REQUEST_ENABLE_BT = 1
    private val REQUEST_ALL_PERMISSION = 2
    private val PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION
    )
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var scanning: Boolean = false
    private var devicesArr = ArrayList<BluetoothDevice>()
    private val SCAN_PERIOD = 1000
    private val handler = Handler()
    private lateinit var viewManager : RecyclerView.LayoutManager
    private lateinit var recyclerViewAdapter: RecyclerViewAdapter

    private var bleGatt: BluetoothGatt? = null
    private var mContext:Context? = null    //Toast 아림을 위한 Context 전달


    private val mLeScanCallback = @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    object : ScanCallback() {
        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.d("scanCallback", "BLE Scan Failed: " + errorCode)
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            results?.let {
                //results is not null
                for (result in it) {
                    if (!devicesArr.contains(result.device) && result.device.name != null) devicesArr.add(result.device)
                }
            }
        }

        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            result?.let {
                // result is not null
                if (!devicesArr.contains(it.device) && it.device.name != null) devicesArr.add(it.device)
                recyclerViewAdapter.notifyDataSetChanged()
            }
        }
    }
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun scanDevice(state:Boolean) = if (state) {
        handler.postDelayed({
            scanning = false
            bluetoothAdapter?.bluetoothLeScanner?.stopScan(mLeScanCallback)
        }, SCAN_PERIOD.toLong())
        scanning = true
        devicesArr.clear()
        bluetoothAdapter?.bluetoothLeScanner?.startScan(mLeScanCallback)
    } else{
        scanning = false
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(mLeScanCallback)
    }

    private fun hasPermission(context: Context?, permissions: Array<String>): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    return false
                }
            }
        }
        return true
    }
    // Permission 확인
    @RequiresApi(Build.VERSION_CODES.M)
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        //If request is cancelled, the result arrays are empty.
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            Toast.makeText(this, "Permission granted!", Toast.LENGTH_SHORT).show()
        } else{
            requestPermissions(permissions, REQUEST_ALL_PERMISSION)
            Toast.makeText(this, "Permission must be grandted", Toast.LENGTH_SHORT).show()
        }
    }

    @TargetApi(Build.VERSION_CODES.M)
    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        viewManager = LinearLayoutManager(this)
        recyclerViewAdapter = RecyclerViewAdapter(devicesArr)
        val recyclerView = findViewById<RecyclerView>(R.id.recyclerView).apply {
            layoutManager = viewManager
            adapter = recyclerViewAdapter
        }

        mContext = this
        recyclerViewAdapter.mListener = object : RecyclerViewAdapter.OnItemClickListener{
            override fun onClick(view: View, position: Int) {
            scanDevice(false)   //scan중지
                val device = devicesArr.get(position)
                bleGatt = DeviceControlActivity(mContext, bleGatt).connectGatt(device)
            }
        }

        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter != null) {
            if (bluetoothAdapter?.isEnabled == false) {
                binding.bleOnOffBtn.isChecked = true
                binding.scanBtn.isVisible = false
            } else {
                binding.bleOnOffBtn.isChecked = false
                binding.scanBtn.isVisible = true
            }
        }
        binding.bleOnOffBtn.setOnCheckedChangeListener { _, isChecked ->
            bluetoothOnOff()
            binding.scanBtn.visibility = if (binding.scanBtn.visibility == View.VISIBLE) {
                View.INVISIBLE
            } else {
                View.VISIBLE
            }
        }

        binding.scanBtn.setOnClickListener{
            v:View? -> //Scan Button Onclick
            if (!hasPermission(this, PERMISSIONS)) {
                requestPermissions(PERMISSIONS,REQUEST_ALL_PERMISSION)
            }
            scanDevice(true)
        }
    }

    private fun bluetoothOnOff() {
        if (bluetoothAdapter == null) {
            // Device doesn't support Bluetooth
            Log.d("bluetoothAdapter", "Device doesn't support Bluetooth")
        } else {
            if (bluetoothAdapter?.isEnabled == false) { // 블루투스 꺼져 있으면 블루투스 활성화
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            } else { // 블루투스 켜져있으면 블루투스 비활성화
                bluetoothAdapter?.disable()
            }
        }
    }


    class RecyclerViewAdapter(private val myDataset: ArrayList<BluetoothDevice>):
    RecyclerView.Adapter<RecyclerViewAdapter.MyViewHolder> () {

        var mListener : OnItemClickListener? = null
        interface OnItemClickListener{
            fun onClick(view: View, position: Int)
        }

        class MyViewHolder(val linearView:LinearLayout): RecyclerView.ViewHolder(linearView)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
            // create a new view
            val linearView = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recyclerview_item,parent,false) as LinearLayout
            return MyViewHolder(linearView)
        }

        override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
            val itemName : TextView = holder.linearView.findViewById(R.id.item_name)
            val itemAddress : TextView = holder.linearView.findViewById(R.id.item_address)
            itemName.text = myDataset[position].name
            itemAddress.text = myDataset[position].address

            if (mListener != null){
                holder?.itemView?.setOnClickListener {v ->
                    mListener?.onClick(v, position)
                }
            }
        }
        override fun getItemCount() = myDataset.size
    }

    private fun Handler.postDelayed(funtion: () -> Unit?, scanPeriod: Int){

    }
}


