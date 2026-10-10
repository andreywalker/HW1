abstract class User(val userName: String, var userId: String, var passwordH:String, val fio: String, val userType: UserType) {

    fun changePassword(newPassword:String){
        val oldPassword =CLI.requestInput("Enter the old password")
        if(DbManager.comparePassword(oldPassword, this)){
            val newPassword =CLI.requestInput("Enter new password")
            DbManager.updatePassword(newPassword, this)
        }else{
            CLI.printk("Wrong password, try again")
        }
    }

}