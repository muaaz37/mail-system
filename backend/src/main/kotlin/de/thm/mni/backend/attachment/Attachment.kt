package de.thm.mni.backend.attachment

import de.thm.mni.backend.mail.Mail
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID


@Entity
@Table(name = "attachments")
class Attachment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    var id : UUID? = null

    @Column(name = "file_name")
    var fileName: String?= ""

    @Column(name = "size")
    var size: Long = 0

    @Column(name = "mime_type")
    var mimeType: String? = ""

    @Column(name = "path")
    var path: String = ""

    @ManyToOne
    @JoinColumn(name = "mail_id", nullable = false)
    var mail: Mail? = null

    constructor()

    constructor(fileName: String, size: Long, mimeType: String, path: String, mail: Mail) {
        this.fileName = fileName
        this.size = size
        this.mimeType = mimeType
        this.path = path
        this.mail = mail
    }
}