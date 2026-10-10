class Manager(
    userName:String,
    userId:String,
    passwordH:String,
    fio:String,
    val speciality:String) :User(userName, userId, passwordH, fio, UserType.Manager) {
}
