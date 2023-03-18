package nl.adaptivity.process.processModel

/**
 * Interface to represent the information needed to authenticate the message to the receiver.
 */
sealed interface ServiceAuthData {

}

data class PasswordServiceAuthData(val userName: String, val password: String) : ServiceAuthData

data class TokenServiceAuthData(val token: String) : ServiceAuthData
