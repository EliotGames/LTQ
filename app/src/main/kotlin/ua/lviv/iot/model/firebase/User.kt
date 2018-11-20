package ua.lviv.iot.model.firebase

class User {
    var name: String? = null
    var email: String? = null
    var sex: UserSex? = null
    var points: Int? = null
    lateinit var quests: HashMap<String, Int>

    // default
    constructor(){}

    // for google and facebook sign up
    constructor(name: String, account: String) {
        this.name = name
        this.points = 0
        this.email = account
        this.sex = UserSex.CHOOSE_SEX
        this.quests = HashMap()
    }

    // for edit profile ???
    constructor(name: String, id: String, sex: UserSex) {
        this.name = name
        this.points = 0
        this.sex = UserSex.CHOOSE_SEX
    }
}
