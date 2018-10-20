package ua.lviv.iot.model.firebase

class User {
    var name: String? = null
    var email: String? = null
    var sex: UserSex? = null
    var points: Int? = null
    var googleEmail: String? = null
    var facebookLink: String? = null

    // default
    constructor()

    // for google and facebook sign up
    constructor(accountType: LoginType, name: String, account: String) {
        this.name = name
        this.points = 0
        when (accountType) {
            LoginType.GOOGLE -> {
                this.googleEmail = account
            }
            LoginType.FACEBOOK -> {
                this.facebookLink = account
            }
        }
        this.sex = UserSex.CHOOSE_SEX
    }

    // for edit profile ???
    constructor(name: String, id: String, sex: UserSex) {
        this.name = name
        this.points = 0
        facebookLink = id
        this.sex = sex
    }
}
