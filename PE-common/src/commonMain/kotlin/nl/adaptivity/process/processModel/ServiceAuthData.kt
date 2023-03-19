package nl.adaptivity.process.processModel

/**
 * Interface to represent the information needed to authenticate the message to the receiver.
 */
sealed interface ServiceAuthData {

}

interface PasswordServiceAuthData : ServiceAuthData {
    val userName: String
    val password: String
}

interface TokenServiceAuthData : ServiceAuthData {
    val token: String
}
