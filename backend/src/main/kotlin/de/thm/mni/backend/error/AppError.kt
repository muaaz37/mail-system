package de.thm.mni.backend.error

class AppError {
    val status: Int
    val message: String?

    constructor(status: Int, message: String?) {
        this.status = status
        this.message = message
    }
}