package com.deviceinfo.auto

/** When each location tab row is included in the phone schema. */
object LocationRowVisibility {

    fun locationRows(@Suppress("UNUSED_PARAMETER") info: DeviceInfo): List<String> = buildList {
        add(DeviceInfoUiShared.Subsection.LOCATION_COMPASS)
        add(DeviceInfoUiShared.Row.LOCATION_DEVICE_HEADING)
        add(DeviceInfoUiShared.Row.LOCATION_DEVICE_HEADING_ACCURACY)
        add(DeviceInfoUiShared.Row.LOCATION_DEVICE_HEADING_SOURCE)
        add(DeviceInfoUiShared.Subsection.LOCATION_GOOGLE_PLAY_SERVICES)
        add(DeviceInfoUiShared.Row.LOCATION_GOOGLE_STATUS)
        add(DeviceInfoUiShared.Row.LOCATION_GOOGLE_COORDINATES)
        add(DeviceInfoUiShared.Row.LOCATION_GOOGLE_ACCURACY)
        add(DeviceInfoUiShared.Row.LOCATION_GOOGLE_ALTITUDE)
        add(DeviceInfoUiShared.Row.LOCATION_GOOGLE_SPEED)
        add(DeviceInfoUiShared.Row.LOCATION_GOOGLE_BEARING)
        add(DeviceInfoUiShared.Row.LOCATION_GOOGLE_FIX_AGE)
        add(DeviceInfoUiShared.Subsection.LOCATION_NETWORK)
        add(DeviceInfoUiShared.Row.LOCATION_NETWORK)
        add(DeviceInfoUiShared.Row.LOCATION_NETWORK_COORDINATES)
        add(DeviceInfoUiShared.Row.LOCATION_NETWORK_ACCURACY)
        add(DeviceInfoUiShared.Row.LOCATION_NETWORK_ALTITUDE)
        add(DeviceInfoUiShared.Row.LOCATION_NETWORK_FIX_AGE)
        add(DeviceInfoUiShared.Subsection.LOCATION_GPS)
        add(DeviceInfoUiShared.Row.LOCATION_GPS)
        add(DeviceInfoUiShared.Row.LOCATION_GPS_COORDINATES)
        add(DeviceInfoUiShared.Row.LOCATION_GPS_SPEED)
        add(DeviceInfoUiShared.Row.LOCATION_GPS_BEARING)
        add(DeviceInfoUiShared.Row.LOCATION_GPS_ACCURACY)
        add(DeviceInfoUiShared.Row.LOCATION_GPS_ALTITUDE)
        add(DeviceInfoUiShared.Row.LOCATION_GPS_FIX_AGE)
        add(DeviceInfoUiShared.Row.LOCATION_GPS_VERTICAL_ACCURACY)
        add(DeviceInfoUiShared.Row.LOCATION_GPS_SPEED_ACCURACY)
        add(DeviceInfoUiShared.Row.LOCATION_GPS_BEARING_ACCURACY)
        add(DeviceInfoUiShared.Row.LOCATION_GPS_MOCK)
        add(DeviceInfoUiShared.Row.LOCATION_GNSS_SATELLITES)
        add(DeviceInfoUiShared.Row.LOCATION_GNSS_FREQUENCY_BANDS)
    }
}
