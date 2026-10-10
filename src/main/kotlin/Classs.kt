
data class Classs(val course:Course, val  teacher:Teacher, val time:String, val room:Int) {
    val classId = course.courseId+":"+time+":"+room;
}