package cn.chitanda.wanandroid.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cn.chitanda.wanandroid.data.DataRepository
import cn.chitanda.wanandroid.data.bean.User
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * @Author:       Chen
 * @Date:         2021/3/10 13:48
 * @Description:
 */
private const val TAG = "UserViewModel"

class UserViewModel(application: Application) : AndroidViewModel(application) {
    private val _user = MutableStateFlow(User())
    val user: StateFlow<User> get() = _user

    init {
        getTodayImage()
    }

    private val _imageUrl = MutableStateFlow("")
    val imageUrl: StateFlow<String> get() = _imageUrl

    fun login(username: String, password: String, callback: (Int, String) -> Unit) {
        launch {
            val response =
                DataRepository.login(username, password)
            if (response.errorCode == 0 && response.data != null) {
                _user.emit(response.data)
                MMKV.defaultMMKV()?.apply {
                    encode("username", username)
                    encode("password", password)
                }
            }
            withContext(Dispatchers.Main) {
                callback.invoke(
                    response.errorCode,
                    response.errorMsg
                )
            }
            Log.d(TAG, "login: ${response.errorCode}")
        }
    }

    private fun getTodayImage() {
        launch {
            val images = DataRepository.getTodayImage().images
            if (images.isNotEmpty()) {
                _imageUrl.emit("https://s.cn.bing.net" + images.first().url)
            }
        }
    }

    fun checkUserData(callback: (Boolean) -> Unit) {
        val mmkv = MMKV.defaultMMKV() ?: return
        callback(mmkv.getStringSet("cookie", emptySet())?.isEmpty() == false)
    }
}

fun ViewModel.launch(
    onError: (Exception) -> Unit = {
        Log.e("ViewModel", "launch: ", it)
    }, block: suspend () -> Unit
) {
    viewModelScope.launch(Dispatchers.IO) {
        try {
            block.invoke()
        } catch (e: Exception) {
            onError.invoke(e)
        }
    }
}