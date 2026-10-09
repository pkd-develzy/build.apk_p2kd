package id.p2kd.kalisalak.coklit

import android.app.Application
import id.p2kd.kalisalak.coklit.data.api.ApiClient

class P2kdApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ApiClient.initialize(this)
    }
}
