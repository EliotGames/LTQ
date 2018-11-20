package ua.lviv.iot.model.firebase

class UserQuest {
    var currentLocation: Int = 0

    constructor() {}

    constructor(location: Int) {
        this.currentLocation = location
    }
}