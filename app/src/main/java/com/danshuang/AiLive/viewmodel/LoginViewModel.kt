package com.danshuang.AiLive.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.danshuang.AiLive.data.LoginRepository
import com.danshuang.AiLive.data.model.LoginResult
import com.danshuang.AiLive.data.model.LoggedInUser
import com.danshuang.AiLive.api.ServerApiUtilsV2
import com.danshuang.AiLive.data.manager.TokenManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class LoginState {
    object Idle : LoginState()
    object Loading : LoginState()
    data class Success(val user: LoggedInUser) : LoginState()
    data class Error(val message: String) : LoginState()
}

class LoginViewModel(application: Application) : AndroidViewModel(application) {
    private val loginRepository = LoginRepository(application)
    private val tokenManager = TokenManager(loginRepository)
    
    // 从strings.xml中读取API基础URL并创建 API 工具类
    private val apiUtils = ServerApiUtilsV2.createFromContext(application, tokenManager, loginRepository)
    
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
                // 使用真实的API调用
                when (val result = apiUtils.login(phone, pass)) {
                    is LoginResult.Success -> {
                        _loginState.value = LoginState.Success(result.user)
                    }
                    is LoginResult.Error -> {
                        _loginState.value = LoginState.Error(
                            result.exception.message ?: "登录失败"
                        )
                    }
                    is LoginResult.Loading -> {
                        // 保持Loading状态
                    }
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
    
    /**
     * 检查用户是否已登录
     */
    suspend fun isLoggedIn(): Boolean {
        return apiUtils.isLoggedIn()
    }
    
    /**
     * 获取当前登录用户信息
     */
    suspend fun getCurrentUser(): LoggedInUser? {
        return apiUtils.getCurrentUser()
    }
    
    /**
     * 注销登录
     */
    fun logout() {
        viewModelScope.launch {
            try {
                apiUtils.logout()
                _loginState.value = LoginState.Idle
                _phoneNumber.value = ""
                _password.value = ""
            } catch (e: Exception) {
                // 即使服务器注销失败，也清除本地状态
                _loginState.value = LoginState.Idle
            }
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
    
    override fun onCleared() {
        super.onCleared()
        // 清理资源
    }
}