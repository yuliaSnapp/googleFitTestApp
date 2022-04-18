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
        insertAndReadData(-1, 1, 0)
        insertAndReadData(-4, 14, 1)
        insertAndReadData(-4, 28, 2)

    }

    private fun getGoogleAccount() = GoogleSignIn.getLastSignedInAccount(this)

    private fun insertAndReadData(amount: Int, day: Int, field: Int) =
        insertData().continueWith { readHistoryData(amount, day, field) }

    private fun insertData(): Task<Void> {
        val dataSet = insertFitnessData()

        return Fitness.getHistoryClient(this, getGoogleAccount()!!)
            .insertData(dataSet)
            .addOnSuccessListener { Log.i(TAG, "successful!") }
            .addOnFailureListener { exception ->
                Log.e(TAG, "There was a problem inserting the dataset.", exception)
            }
    }

    private fun readHistoryData(amount: Int, day: Int, field: Int): Task<DataReadResponse> {
        val readRequest = queryFitnessData(amount, day)

        return Fitness.getHistoryClient(this, getGoogleAccount()!!)
            .readData(readRequest)
            .addOnSuccessListener { dataReadResponse ->
                printData(dataReadResponse, field)
                Log.i(TAG, "$dataReadResponse '''' $readRequest ")
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

    private fun queryFitnessData(amount: Int, day: Int): DataReadRequest {

        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        val now = Date()
        calendar.time = now
        val endTime = calendar.timeInMillis
        calendar.add(Calendar.WEEK_OF_YEAR, amount)
        val startTime = calendar.timeInMillis

        return DataReadRequest.Builder()
            .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
            .bucketByTime(day, TimeUnit.DAYS)
            .aggregate(DataType.TYPE_STEP_COUNT_DELTA)
            .build()
    }

    private fun printData(dataReadResult: DataReadResponse, field: Int) {
        if (dataReadResult.buckets.isNotEmpty()) {
            for (bucket in dataReadResult.buckets) {
                bucket.dataSets.forEach { dumpDataSet(it, field) }
            }
        } else if (dataReadResult.dataSets.isNotEmpty()) {
            dataReadResult.dataSets.forEach { dumpDataSet(it, field) }
        }
    }

    private fun dumpDataSet(dataSet: DataSet, field: Int) {

        for (dp in dataSet.dataPoints) {
            dp.dataType.fields.forEach {
                when (field) {
                    0 -> binding.dailyStep.text = "${dp.getValue(it)}"

                    1 -> binding.weekStep.text = "${dp.getValue(it)}"

                    2 -> binding.monthStep.text = "${dp.getValue(it)}"

                }
            }
        }
    }
}
