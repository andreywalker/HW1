abstract class User(val userName: String, val userId: String, var password:String) {

    fun changePassword(newPassword:String){
        val oldPassword =CLI.requestInput("Enter the old password")
        if(DbManager.comparePassword(oldPassword, this)){
            val newPassword =CLI.requestInput("Enter new password")
            DbManager.updatePassword(newPassword, this)
        }else{
            CLI.print("Wrong password, try again")
        }
    }

}