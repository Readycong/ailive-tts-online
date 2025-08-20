package com.danshuang.AiLive.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danshuang.AiLive.data.LoginRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val token: String, val phone: String) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val loginRepository = LoginRepository(application)
    
    private val _loginState = MutableStateFlow<LoginState>(LoginState.Idle)
    val loginState: StateFlow<LoginState> = _loginState.asStateFlow()
    
    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()
    
    private val _password = MutableStateFlow("")
    val password: StateFlow<String> = _password.asStateFlow()
    
    fun updatePhoneNumber(phone: String) {
        _phoneNumber.value = phone
    }
    
    fun updatePassword(pass: String) {
        _password.value = pass
    }
    
    fun login() {
        val phone = _phoneNumber.value
        val pass = _password.value
        
        if (!validateInput(phone, pass)) {
            _loginState.value = LoginState.Error("请输入正确的手机号和密码")
            return
        }
        
        viewModelScope.launch {
            _loginState.value = LoginState.Loading
            
            try {
                // 模拟网络请求
                kotlinx.coroutines.delay(1500)
                
                // 模拟登录验证
                val (success, token, message) = performLogin(phone, pass)
                
                if (success && token != null) {
                    loginRepository.saveLoginInfo(token, phone)
                    _loginState.value = LoginState.Success(token, phone)
                } else {
                    _loginState.value = LoginState.Error(message ?: "登录失败")
                }
            } catch (e: Exception) {
                _loginState.value = LoginState.Error("网络错误: ${e.message}")
            }
        }
    }
    
    private fun validateInput(phone: String, password: String): Boolean {
        return phone.isNotEmpty() && 
               password.isNotEmpty() && 
               phone.matches(Regex("^1[3-9]\\d{9}$")) &&
               password.length >= 6
    }
    
    private suspend fun performLogin(phone: String, password: String): Triple<Boolean, String?, String?> {
        // 模拟API调用
        return when {
            phone == "13800138000" && password == "123456" -> Triple(true, "mock_token_12345", null)
            phone.length != 11 -> Triple(false, null, "手机号格式不正确")
            password.length < 6 -> Triple(false, null, "密码长度不能少于6位")
            else -> Triple(false, null, "手机号或密码错误")
        }
    }
    
    fun resetState() {
        _loginState.value = LoginState.Idle
    }
    
    fun clearError() {
        if (_loginState.value is LoginState.Error) {
            _loginState.value = LoginState.Idle
        }
    }
}