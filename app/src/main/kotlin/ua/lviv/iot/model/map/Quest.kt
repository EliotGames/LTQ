package ua.lviv.iot.model.map

/**
 * Class that represents quest as a object
 * @property questID the unique identifier of the quest
 * @property questName title of the quest
 * @property description description of quest
 * @property imageUrl url in database of the quest's image
 * @property parentCategoryID identifier of the quest's category
 * @property distance the length of the quest (in kilometers)
 * @property imageViewId ???
 */
data class Quest(
        val questID: Int,
        val questName: String,
        val description: String,
        val imageUrl: String = "",
        val parentCategoryID: Int = 0,
        val distance: Double = 0.0,
        val imageViewId: Int = 0
)
