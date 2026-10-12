class Student(
    userName:String,
    userId:String,
    passwordH:String,
    fio:String,
    var studyYear:Int =0,
    var gpa:Double=0.0,
    val speciality:String) :User(userName, userId, passwordH, fio, UserType.Student) {
}
