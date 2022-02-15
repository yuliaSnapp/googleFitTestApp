package com.example.googlefittestapp

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.example.googlefittestapp.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.fitness.Fitness
import com.google.android.gms.fitness.data.*
import com.google.android.gms.fitness.request.DataReadRequest
import com.google.android.gms.fitness.result.DataReadResponse
import com.google.android.gms.tasks.Task
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity() {

    private val TAG = "BasicHistoryApi"

    lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_main)
        insertAndReadData()


    }

    private fun getGoogleAccount() = GoogleSignIn.getLastSignedInAccount(this)

    private fun insertAndReadData() = insertData().continueWith { readHistoryData() }

    private fun insertData(): Task<Void> {
        val dataSet = insertFitnessData()

        return Fitness.getHistoryClient(this, getGoogleAccount()!!)
            .insertData(dataSet)
            .addOnSuccessListener { Log.i(TAG, "successful!") }
            .addOnFailureListener { exception ->
                Log.e(TAG, "There was a problem inserting the dataset.", exception)
            }
    }

    private fun readHistoryData(): Task<DataReadResponse> {
        val readRequest = queryFitnessData()

        return Fitness.getHistoryClient(this, getGoogleAccount()!!)
            .readData(readRequest)
            .addOnSuccessListener { dataReadResponse ->
                printData(dataReadResponse)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "There was a problem reading the data.", e)
            }
    }


    private fun insertFitnessData(): DataSet {

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val now = Date()
        calendar.time = now
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.HOUR_OF_DAY, -1)
        val startTime = calendar.timeInMillis

        val dataSource = DataSource.Builder()
            .setAppPackageName(this)
            .setDataType(DataType.TYPE_STEP_COUNT_DELTA)
            .setStreamName("$TAG - step count")
            .setType(DataSource.TYPE_RAW)
            .build()

        val stepCountDelta = 0
        return DataSet.builder(dataSource)
            .add(
                DataPoint.builder(dataSource)
                    .setField(Field.FIELD_STEPS, stepCountDelta)
                    .setTimeInterval(startTime, endTime, TimeUnit.MILLISECONDS)
                    .build()
            ).build()
    }

    private fun queryFitnessData(): DataReadRequest {

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val now = Date()
        calendar.time = now
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, -1)
        val startTime = calendar.timeInMillis

        return DataReadRequest.Builder()
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA, DataType.AGGREGATE_STEP_COUNT_DELTA)
            .bucketByTime(1, TimeUnit.DAYS)
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .build()
    }

    private fun printData(dataReadResult: DataReadResponse) {
        if (dataReadResult.buckets.isNotEmpty()) {
            for (bucket in dataReadResult.buckets) {
                bucket.dataSets.forEach { dumpDataSet(it) }
            }
        } else if (dataReadResult.dataSets.isNotEmpty()) {
            dataReadResult.dataSets.forEach { dumpDataSet(it) }
        }
    }

    private fun dumpDataSet(dataSet: DataSet) {

        for (dp in dataSet.dataPoints) {
            dp.dataType.fields.forEach {
                binding.titleInfo.text = it.name
                binding.info.text = "${dp.getValue(it)}"
            }
        }
    }
}
