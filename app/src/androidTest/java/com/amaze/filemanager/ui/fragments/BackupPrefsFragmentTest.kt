/*
 * Copyright (C) 2014-2022 Arpit Khurana <arpitkh96@gmail.com>, Vishal Nehra <vishalmeham2@gmail.com>,
 * Emmanuel Messulam<emmanuelbendavid@gmail.com>, Raymond Lai <airwave209gt at gmail.com> and Contributors.
 *
 * This file is part of Amaze File Manager.
 *
 * Amaze File Manager is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.amaze.filemanager.ui.fragments

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.Lifecycle
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.amaze.filemanager.R
import com.amaze.filemanager.ui.activities.PreferencesActivity
import com.amaze.filemanager.ui.fragments.preferencefragments.BackupPrefsFragment
import com.google.gson.*
import com.google.gson.reflect.TypeToken
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class BackupPrefsFragmentTest {

    var storagePath = "`/storage/emulated/0`"
    var fileName = "amaze_backup.json"

    @Test
    fun testExport() {
        val backupPrefsFragment = BackupPrefsFragment()

        val activityScenario = ActivityScenario.launch(PreferencesActivity::class.java)

        activityScenario.moveToState(Lifecycle.State.STARTED)

        val exportFile =
            File(
                storagePath +
                    File.separator +
                    fileName
            )

        exportFile.delete() // delete if already exists

        activityScenario.onActivity {
            it.supportFragmentManager.beginTransaction()
                .add(backupPrefsFragment, null)
                .commitNow()

            backupPrefsFragment.exportPrefs()
        }

        val tempFile =
            File(
                ApplicationProvider.getApplicationContext<Context>().cacheDir.absolutePath +
                    File.separator +
                    fileName
            )

        Assert.assertTrue(tempFile.exists())

        // terrible hack :cringe:
        onView(withId(R.id.home)).perform(ViewActions.click())
        Thread.sleep(500)

        onView(withText(R.string.save)).perform(ViewActions.click())
        Thread.sleep(500)

        Assert.assertTrue(exportFile.exists())
    }

    @Test
    fun verifyExportFile() {
        val backupPrefsFragment = BackupPrefsFragment()

        val activityScenario = ActivityScenario.launch(PreferencesActivity::class.java)

        activityScenario.moveToState(Lifecycle.State.STARTED)

        val file =
            File(
                storagePath +
                    File.separator +
                    fileName
            )

        activityScenario.onActivity { preferencesActivity ->
            preferencesActivity.supportFragmentManager.beginTransaction()
                .add(backupPrefsFragment, null)
                .commitNow()

            val preferences = PreferenceManager
                .getDefaultSharedPreferences(preferencesActivity)

            val preferenceMap: Map<String?, *> = preferences.all

            val inputString = file
                .inputStream()
                .bufferedReader()
                .use {
                    it.readText()
                }

            val type = object : TypeToken<Map<String?, *>>() {}.type

            val importMap: Map<String?, *> = GsonBuilder()
                .create()
                .fromJson(
                    inputString,
                    type
                )

            for ((key, value) in preferenceMap) {
                var mapValue = importMap[key]

                if (mapValue!!::class.simpleName.equals("Double")) {
                    mapValue = (mapValue as Double).toInt() // since Gson parses Integer as Double
                }

                Assert.assertEquals(value, mapValue)
            }
        }
    }

    @Test
    fun testImport() {
        val backupPrefsFragment = BackupPrefsFragment()

        val activityScenario = ActivityScenario.launch(PreferencesActivity::class.java)

        activityScenario.moveToState(Lifecycle.State.STARTED)

        val exportFile =
            File(
                storagePath +
                    File.separator +
                    fileName
            )

        exportFile.delete() // delete if already exists

        activityScenario.onActivity { preferencesActivity ->
            preferencesActivity.supportFragmentManager.beginTransaction()
                .add(backupPrefsFragment, null)
                .commitNow()

            javaClass.getResourceAsStream("/$fileName")?.copyTo(exportFile.outputStream())

            backupPrefsFragment.onActivityResult(
                BackupPrefsFragment.IMPORT_BACKUP_FILE,
                Activity.RESULT_OK,
                Intent().setData(
                    Uri.fromFile(exportFile)
                )
            )

            val inputString = exportFile
                .inputStream()
                .bufferedReader()
                .use {
                    it.readText()
                }

            val type = object : TypeToken<Map<String?, *>>() {}.type

            val importMap: Map<String?, *> = GsonBuilder()
                .create()
                .fromJson(
                    inputString,
                    type
                )

            val preferences = PreferenceManager
                .getDefaultSharedPreferences(preferencesActivity)

            val preferenceMap: Map<String?, *> = preferences.all

            for ((key, value) in preferenceMap) {
                when (value!!::class.simpleName) {
                    "Boolean" -> Assert.assertEquals(
                        importMap[key] as Boolean,
                        preferences.getBoolean(key, false)
                    )
                    "Float" -> Assert.assertEquals(
                        importMap[key] as Float,
                        preferences.getFloat(key, 0f)
                    )
                    "Int" -> {
                        // since Gson parses Integer as Double
                        val toInt = (importMap[key] as Double).toInt()

                        Assert.assertEquals(toInt, preferences.getInt(key, 0))
                    }
                    "Long" -> Assert.assertEquals(
                        importMap[key] as Long,
                        preferences.getLong(key, 0L)
                    )
                    "String" -> Assert.assertEquals(
                        importMap[key] as String,
                        preferences.getString(key, null)
                    )
                    "Set<*>" -> Assert.assertEquals(
                        importMap[key] as Set<*>,
                        preferences.getStringSet(key, null)
                    )
                }
            }
        }
    }
}
