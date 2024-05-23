package com.tejas.videoapp

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.tejas.videoapp.databinding.ActivityCallBinding

class CallActivity : AppCompatActivity() {
    private var _binding: ActivityCallBinding? = null
    private val binding get() = _binding!!
    lateinit var username: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityCallBinding.inflate(layoutInflater)
        setContentView(binding.root)
        username = intent.getStringExtra("username")?:""
    }
}