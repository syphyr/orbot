package org.torproject.android.circumvention

import android.util.Log
import com.android.volley.toolbox.HurlStack
import java.io.IOException
import java.net.Authenticator
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.PasswordAuthentication
import java.net.Proxy
import java.net.URL

class ProxiedHurlStack(
    private val mHost: String,
    private val mPort: Int,
    private val mUsername: String,
    private val mPassword: String
) : HurlStack() {

    @Throws(IOException::class)
    override fun createConnection(url: URL): HttpURLConnection {
        val proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(mHost, mPort))

        val authenticator = object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                Log.d(this::class.java.simpleName, "getPasswordAuthentication!")
                return PasswordAuthentication(mUsername, mPassword.toCharArray())
            }
        }

        Authenticator.setDefault(authenticator)

        return url.openConnection(proxy) as HttpURLConnection
    }
}