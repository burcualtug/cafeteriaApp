package repositories

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class UserRepository() {

    suspend fun returncompanyID(userID: String)=
        suspendCoroutine<String> {
            val user = FirebaseAuth.getInstance().currentUser!!
            val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().reference

            databaseRef.child("matches").child(user.uid)
                .addListenerForSingleValueEvent(object: ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val globalOrgID = snapshot.child("organizationID").value.toString()
                        it.resume(globalOrgID)
                        Log.d("GLBID",globalOrgID)
                    }

                    override fun onCancelled(error: DatabaseError) {
                        TODO("Not yet implemented")
                    }
                })
        }
}