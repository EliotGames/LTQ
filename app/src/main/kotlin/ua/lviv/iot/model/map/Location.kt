package ua.lviv.iot.model.map

/**
 * Class that represents location as a object
 * @property id the unique identifier of the location
 * @property lat location latitude
 * @property lon location longitude
 * @property locationName location name
 * @property locationDescription location description
 * @property isSecret should location be secret or not
 */
data class Location(
   val id: Int = 0,
   val lat: Long = 0,
   val lon: Long = 0,
   val locationName: String? = null,
   val locationDescription: String? = null,
   val isSecret: Boolean = false
)