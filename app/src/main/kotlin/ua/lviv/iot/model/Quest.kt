package ua.lviv.iot.model

/**
 * Class that represents quest as a object
 * @property title title of the quest
 * @property description description of quest
 * @property imageUrl url in database of the quest's image
 * @property id the unique identifier of the quest
 */
data class Quest(
        val title: String,
        val description: String,
        val imageUrl : String,
        val id: Int = 0
)
