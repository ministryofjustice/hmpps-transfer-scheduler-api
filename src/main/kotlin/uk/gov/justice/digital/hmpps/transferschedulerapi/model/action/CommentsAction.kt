package uk.gov.justice.digital.hmpps.transferschedulerapi.model.action

interface CommentsAction {
  val comments: String?
  infix fun changes(comments: String?): Boolean {
    val truncationIndex = this.comments?.indexOf(TRUNCATION_IDENTIFIER) ?: -1
    val replacementComments = if (truncationIndex > 0 && comments?.startsWith(this.comments!!.substring(0, truncationIndex)) == true) {
      comments
    } else {
      this.comments
    }
    return replacementComments != comments
  }

  companion object {
    const val TRUNCATION_IDENTIFIER = "... see DPS for full text"
  }
}
