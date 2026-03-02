package app.serfeli.model

data class UpdateProfileRequest(
    val userId: String,
    val username: String?,
    val email: String?,
    val phone: String?
)

data class UserProfileResponse(
    val id: String,
    val username: String,
    val email: String?,
    val phone: String?
)
