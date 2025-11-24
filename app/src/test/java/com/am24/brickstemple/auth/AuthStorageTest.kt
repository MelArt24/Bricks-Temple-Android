package com.am24.brickstemple.auth

import android.content.ContextWrapper
import android.content.SharedPreferences
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class FakeSharedPreferences : SharedPreferences {

    private val data = mutableMapOf<String, Any?>()

    override fun getString(key: String?, defValue: String?): String? =
        data[key] as? String ?: defValue

    override fun getAll(): MutableMap<String, *> = data

    override fun edit(): SharedPreferences.Editor = Editor()

    inner class Editor : SharedPreferences.Editor {

        private val pending = mutableMapOf<String, Any?>()
        private var clear = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            pending[key!!] = value
            return this
        }

        override fun clear(): SharedPreferences.Editor {
            clear = true
            return this
        }

        override fun apply() {
            commit()
        }

        override fun commit(): Boolean {
            if (clear) data.clear()
            data.putAll(pending)
            return true
        }

        override fun putInt(key: String?, value: Int) = this
        override fun putLong(key: String?, value: Long) = this
        override fun putFloat(key: String?, value: Float) = this
        override fun putBoolean(key: String?, value: Boolean) = this
        override fun putStringSet(key: String?, values: MutableSet<String>?) = this
        override fun remove(key: String?) = this
    }

    override fun contains(key: String?) = data.containsKey(key)
    override fun getInt(key: String?, def: Int) = def
    override fun getLong(key: String?, def: Long) = def
    override fun getFloat(key: String?, def: Float) = def
    override fun getBoolean(key: String?, def: Boolean) = def
    override fun getStringSet(key: String?, def: MutableSet<String>?) = def
    override fun registerOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(l: SharedPreferences.OnSharedPreferenceChangeListener?) {}
}

class FakeContext : ContextWrapper(null) {

    private val prefs = FakeSharedPreferences()

    override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
        return prefs
    }
}


class AuthStorageTest {

    private val context = FakeContext()

    @Before
    fun setup() {
        AuthSession.clear()
    }

    @Test
    fun `save should write token email username`() {
        AuthStorage.save(context, "abc123", "test@mail.com", "Artem")

        val prefs = context.getSharedPreferences("auth_prefs", 0)

        assertEquals("abc123", prefs.getString("token", null))
        assertEquals("test@mail.com", prefs.getString("email", null))
        assertEquals("Artem", prefs.getString("username", null))
    }

    @Test
    fun `load should update AuthSession`() {
        val prefs = context.getSharedPreferences("auth_prefs", 0)
        prefs.edit()
            .putString("token", "zzz")
            .putString("email", "user@mail.com")
            .putString("username", "TestUser")
            .apply()

        AuthStorage.load(context)

        assertEquals("zzz", AuthSession.token)
        assertEquals("user@mail.com", AuthSession.email)
        assertEquals("TestUser", AuthSession.username)
        assertTrue(AuthSession.isLoggedIn())
    }

    @Test
    fun `clear should remove all stored values`() {
        val prefs = context.getSharedPreferences("auth_prefs", 0)
        prefs.edit().putString("token", "hello").apply()

        AuthStorage.clear(context)

        assertNull(prefs.getString("token", null))
    }
}
