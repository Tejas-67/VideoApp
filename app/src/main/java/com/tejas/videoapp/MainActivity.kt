package com.tejas.videoapp

import android.Manifest
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.permissionx.guolindev.PermissionX
import com.tejas.videoapp.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.enterBtn.setOnClickListener {
            PermissionX.init(this)
                .permissions(
                    Manifest.permission.RECORD_AUDIO,
                    Manifest.permission.CAMERA
                ).request{ allGranted, _, _ ->
                    if(allGranted){
                        startActivity(
                            Intent(
                                this@MainActivity,
                                CallActivity::class.java
                            )
                                .putExtra("username", binding.usernameEditText.text.toString())
                        )
                    }else{
                        Toast.makeText(this@MainActivity, "Please accept all permissions", Toast.LENGTH_SHORT).show()
                    }
                }
        }
    }
}