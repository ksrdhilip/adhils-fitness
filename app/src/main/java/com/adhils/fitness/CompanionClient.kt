package com.adhils.fitness

import android.content.Context
import android.util.Base64
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import com.adhils.fitness.core.*
import java.net.InetAddress
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.net.ssl.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@Serializable data class PairInvitation(val version:Int=1,val url:String,val fingerprint:String,val secret:String)
@Serializable data class Connection(val url:String,val fingerprint:String,val token:String)
class CompanionClient(private val context:Context) {
    private val prefs=context.getSharedPreferences("companion",Context.MODE_PRIVATE)
    private val json=Json { ignoreUnknownKeys=true; encodeDefaults=true }
    var provider:String
        get()=prefs.getString("provider","codex")!!
        set(value) { require(value in setOf("codex","openai","claude")); prefs.edit().putString("provider",value).apply() }
    fun connection():Connection? = runCatching {
        val value=prefs.getString("connection",null) ?: return null
        val data=Base64.decode(value,Base64.NO_WRAP)
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,data.copyOfRange(0,12)))
        json.decodeFromString<Connection>(cipher.doFinal(data.copyOfRange(12,data.size)).toString(Charsets.UTF_8))
    }.getOrNull()
    fun disconnect() { prefs.edit().remove("connection").apply() }
    private fun save(c:Connection) {
        val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE,key())
        val value=cipher.iv+cipher.doFinal(json.encodeToString(c).toByteArray())
        prefs.edit().putString("connection",Base64.encodeToString(value,Base64.NO_WRAP)).apply()
    }
    private fun key():SecretKey {
        val store=KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey("adhils-companion",null) as? SecretKey)?.let { return it }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder("adhils-companion",KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    private fun client(c:Connection):OkHttpClient {
        require(c.fingerprint.matches(Regex("[a-fA-F0-9]{64}"))) { "Invalid certificate fingerprint" }
        val address=java.net.URI(c.url)
        require(address.scheme=="https" && address.userInfo==null && address.rawQuery==null && address.fragment==null)
        val host=address.host ?: error("Missing companion host")
        val resolved=InetAddress.getByName(host)
        require(resolved.isSiteLocalAddress || resolved.isLoopbackAddress) { "Use a private-network companion address" }
        val manager=object:X509TrustManager {
            override fun getAcceptedIssuers()=emptyArray<X509Certificate>()
            override fun checkClientTrusted(chain:Array<X509Certificate>,authType:String) { throw java.security.cert.CertificateException("Client certificates not supported") }
            override fun checkServerTrusted(chain:Array<X509Certificate>,authType:String) {
                if(chain.isEmpty()) throw java.security.cert.CertificateException("Missing certificate")
                chain[0].checkValidity()
                val digest=MessageDigest.getInstance("SHA-256").digest(chain[0].encoded).joinToString("") { "%02x".format(it) }
                if(!MessageDigest.isEqual(digest.lowercase().toByteArray(),c.fingerprint.lowercase().toByteArray()))
                    throw java.security.cert.CertificateException("Companion certificate changed. Pair again.")
            }
        }
        val ssl=SSLContext.getInstance("TLS").apply { init(null,arrayOf(manager),SecureRandom()) }
        return OkHttpClient.Builder().sslSocketFactory(ssl.socketFactory,manager)
            // Identity is established by the certificate fingerprint scanned in person.
            .hostnameVerifier { requested,_ -> requested==host }
            .followRedirects(false).followSslRedirects(false)
            .connectTimeout(10,TimeUnit.SECONDS).readTimeout(120,TimeUnit.SECONDS).callTimeout(125,TimeUnit.SECONDS).build()
    }
    private suspend fun request(c:Connection,path:String,payload:String?=null):String=withContext(Dispatchers.IO) {
        val builder=Request.Builder().url(c.url.trimEnd('/')+path)
        if(c.token.isNotEmpty()) builder.header("Authorization","Bearer "+c.token)
        if(payload!=null) builder.post(payload.toRequestBody("application/json".toMediaType()))
        client(c).newCall(builder.build()).execute().use { response ->
            val source=response.body?.source() ?: error("Empty response from companion")
            require(!source.request(262145)) { "Companion response too large" }
            val body=source.readUtf8()
            if(!response.isSuccessful) {
                val message=runCatching { json.parseToJsonElement(body).jsonObject["error"]?.jsonPrimitive?.content }.getOrNull()
                error(message ?: "Companion returned HTTP ${response.code}")
            }
            body
        }
    }
    suspend fun pair(invitationText:String):String {
        val invitation=json.decodeFromString<PairInvitation>(invitationText.trim())
        require(invitation.version==1)
        val provisional=Connection(invitation.url,invitation.fingerprint,"")
        val payload=buildJsonObject { put("secret",invitation.secret); put("deviceName",android.os.Build.MODEL) }
        val result=json.parseToJsonElement(request(provisional,"/v1/pair",payload.toString())).jsonObject
        save(provisional.copy(token=result.getValue("token").jsonPrimitive.content))
        return "Paired with your PC"
    }
    suspend fun status():JsonObject=request(connection() ?: error("Pair your PC in Settings"),"/v1/status").let { json.parseToJsonElement(it).jsonObject }
    suspend fun coach(input:CoachRequest):CoachReply {
        val c=connection() ?: error("Pair your PC in Settings to use AI coaching.")
        return json.decodeFromString(request(c,"/v1/coach",json.encodeToString(input)))
    }
}
