package com.hamyareman.ir.ui.profile

import android.content.Context
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import org.json.JSONArray

/**
 * استان → شهرستان → شهر، از دیتاست رسمی پست ایران
 * (github.com/masterking32/iran-states-cities-districts).
 * دراپ‌داون‌ها پیش‌فرض خالی‌اند و وابسته به انتخاب قبلی.
 */
object IranPlaces {
    data class County(val name: String, val cities: List<String>)
    data class Province(val name: String, val counties: List<County>)

    @Volatile private var cache: List<Province>? = null

    fun load(ctx: Context): List<Province> {
        cache?.let { return it }
        synchronized(this) {
            cache?.let { return it }
            val txt = ctx.assets.open("iran_places.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
            val arr = JSONArray(txt)
            val list = ArrayList<Province>(arr.length())
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val cs = o.getJSONArray("c")
                val counties = ArrayList<County>(cs.length())
                for (j in 0 until cs.length()) {
                    val c = cs.getJSONObject(j)
                    val ts = c.getJSONArray("t")
                    val towns = ArrayList<String>(ts.length())
                    for (k in 0 until ts.length()) towns.add(ts.getString(k))
                    counties.add(County(c.getString("n"), towns))
                }
                list.add(Province(o.getString("n"), counties))
            }
            cache = list
            return list
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IranLocationFields(
    province: String,
    county: String,
    city: String,
    onProvince: (String) -> Unit,
    onCounty: (String) -> Unit,
    onCity: (String) -> Unit,
) {
    val ctx = LocalContext.current
    val places = remember { IranPlaces.load(ctx) }
    val counties = remember(province) { places.firstOrNull { it.name == province }?.counties.orEmpty() }
    val cities = remember(province, county) { counties.firstOrNull { it.name == county }?.cities.orEmpty() }

    PlaceDropdown(
        label = "استان",
        value = province,
        options = places.map { it.name },
        placeholder = "انتخاب استان",
        onPick = { onProvince(it); onCounty(""); onCity("") },
    )
    PlaceDropdown(
        label = "شهرستان",
        value = county,
        options = counties.map { it.name },
        placeholder = "انتخاب شهرستان",
        enabled = province.isNotBlank(),
        onPick = { onCounty(it); onCity("") },
    )
    PlaceDropdown(
        label = "شهر",
        value = city,
        options = cities,
        placeholder = "انتخاب شهر",
        enabled = county.isNotBlank(),
        onPick = onCity,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaceDropdown(
    label: String,
    value: String,
    options: List<String>,
    placeholder: String,
    enabled: Boolean = true,
    onPick: (String) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = open && enabled, onExpandedChange = { if (enabled) open = it }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            placeholder = { Text(placeholder) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(open && enabled) },
            modifier = Modifier.fillMaxWidth().menuAnchor(),
        )
        ExposedDropdownMenu(expanded = open && enabled, onDismissRequest = { open = false }) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(opt) },
                    onClick = { onPick(opt); open = false },
                )
            }
        }
    }
}
