class Student(
    userName:String,
    userId:String,
    passwordH:String,
    fio:String,
    var studyYear:Int,
    var gpa:Double,
    val speciality:String) :User(userName, userId, passwordH, fio, UserType.Student) {
}
